package org.farring.gcs.maps.providers.GoogleMap.offline.Downloader;

import android.content.Context;

import com.litesuits.orm.LiteOrm;
import com.orhanobut.logger.Logger;

import org.farring.gcs.maps.DPMap.VisibleMapArea;
import org.farring.gcs.maps.providers.GoogleMap.offline.Database.OfflineDatabase;
import org.farring.gcs.maps.providers.GoogleMap.offline.Database.TileBean;
import org.farring.gcs.maps.providers.GoogleMap.offline.Tiles.OfflineTileProvider;
import org.farring.gcs.maps.providers.GoogleMap.offline.Utils.OfflineMapUtils;
import org.farring.gcs.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 离线地图下载器
 * TODO:后台下载！
 */
public class MapDownloader {


    private final AtomicInteger totalFilesWritten = new AtomicInteger(0);            // 被写入多少文件
    private final AtomicInteger totalFilesExpectedToWrite = new AtomicInteger(0);    // 需要写入多少文件
    // 上下文
    private final Context context;
    // 监听器
    private final ArrayList<MapDownloaderListener> listeners = new ArrayList<>();
    private OfflineMapDownloaderState state;    // 下载状态
    // 线程池
    private ExecutorService downloadsScheduler;
    private OkHttpClient okHttpClient;

    public MapDownloader(Context context) {
        this.context = context;
        setupDownloadScheduler();

        okHttpClient = new OkHttpClient();

        this.state = OfflineMapDownloaderState.AVAILABLE;
    }

    public OfflineMapDownloaderState getState() {
        return state;
    }

    public boolean addMapDownloaderListener(MapDownloaderListener listener) {
        if (listener != null) {
            listener.stateChanged(this.state);
            return listeners.add(listener);
        }
        return false;
    }

    public boolean removeMapDownloaderListener(MapDownloaderListener listener) {
        return listeners.remove(listener);
    }

    public void cancelDownload() {
        if (state == OfflineMapDownloaderState.RUNNING) {
            this.state = OfflineMapDownloaderState.CANCELLING;
            notifyDelegateOfStateChange();
        }

        setupDownloadScheduler();

        if (state == OfflineMapDownloaderState.CANCELLING) {
            this.state = OfflineMapDownloaderState.AVAILABLE;
            notifyDelegateOfStateChange();
        }
    }

    private void setupDownloadScheduler() {
        if (downloadsScheduler != null) {
            downloadsScheduler.shutdownNow();
        }

        final int processorsCount = (int) (Runtime.getRuntime().availableProcessors() * 1.5f);
        Logger.v("Using " + processorsCount + " processors.");
        downloadsScheduler = Executors.newFixedThreadPool(processorsCount);
    }

    public void notifyDelegateOfStateChange() {
        for (MapDownloaderListener listener : listeners) {
            listener.stateChanged(this.state);
        }
    }

    /*
     *   Delegate Notifications
     */
    public void notifyDelegateOfInitialCount(int totalFilesExpectedToWrite) {
        for (MapDownloaderListener listener : listeners) {
            listener.initialCountOfFiles(totalFilesExpectedToWrite);
        }
    }

    public void notifyDelegateOfProgress(int totalFilesWritten, int totalFilesExpectedToWrite) {
        for (MapDownloaderListener listener : listeners) {
            listener.progressUpdate(totalFilesWritten, totalFilesExpectedToWrite);
        }
    }

    public void notifyDelegateOfNetworkConnectivityError(Throwable error) {
        for (MapDownloaderListener listener : listeners) {
            listener.networkConnectivityError(error);
        }
    }

    public void notifyDelegateOfSqliteError(Throwable error) {
        for (MapDownloaderListener listener : listeners) {
            listener.sqlLiteError(error);
        }
    }

    public void notifyDelegateOfHTTPStatusError(int status, String url) {
        for (MapDownloaderListener listener : listeners) {
            listener.httpStatusError(status, url);
        }
    }

    public void notifyDelegateOfCompletionWithOfflineMapDatabase() {
        for (MapDownloaderListener listener : listeners) {
            listener.completionOfOfflineDatabaseMap();
        }
    }

    // 【开始下载】传入需要下载URLs
    public void startDownloading(ArrayList<String> urls) {
        this.totalFilesExpectedToWrite.set(urls.size());
        this.totalFilesWritten.set(0);

        notifyDelegateOfInitialCount(totalFilesExpectedToWrite.get());

        Logger.i(String.format(Locale.US, "number of urls to download = %d", urls.size()));
        if (this.totalFilesExpectedToWrite.get() == 0) {
            finishUpDownloadProcess();
            return;
        }

        if (!Utils.isNetworkAvailable(context)) {
            Logger.e("Network is not available.");
            notifyDelegateOfNetworkConnectivityError(new IllegalStateException("Network is not available"));
            return;
        }

        // 下载开始
        final CountDownLatch downloadsTracker = new CountDownLatch(this.totalFilesExpectedToWrite.get());
        for (final String url : urls) {
            downloadsScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 创建一个Request
                        Request request = new Request.Builder().url(url).build();

                        Response response = okHttpClient.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            String msg = String.format(Locale.US, "HTTP Error connection.  Response Code = %d for url = %s", response.code(), url);
                            Logger.w(msg);
                            notifyDelegateOfHTTPStatusError(response.code(), url);
                            throw new IOException(msg);
                        }

                        ByteArrayOutputStream bais = new ByteArrayOutputStream();
                        InputStream is = null;
                        try {
                            is = response.body().byteStream();
                            // Read 4K at a time
                            byte[] byteChunk = new byte[4096];
                            int n;

                            while ((n = is.read(byteChunk)) > 0) {
                                bais.write(byteChunk, 0, n);
                            }
                        } catch (IOException e) {
                            Logger.e(e, String.format(Locale.US, "Failed while reading bytes from %s: %s", url, e.getMessage()));
                        } finally {
                            if (is != null) {
                                is.close();
                            }
                        }
                        sqliteSaveDownloadedData(bais.toByteArray(), url);
                    } catch (IOException e) {
                        Logger.e(e, "Error occurred while retrieving map data.");
                    } finally {
                        downloadsTracker.countDown();
                    }
                }
            });
        }

        downloadsScheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadsTracker.await();
                } catch (InterruptedException e) {
                    Logger.e(e, "Error while waiting for downloads to complete.");
                } finally {
                    finishUpDownloadProcess();
                }
            }
        });
    }

    /**
     * 保存下载数据到数据库中
     *
     * @param data
     * @param url
     */

    public void sqliteSaveDownloadedData(byte[] data, String url) {
        if (Utils.runningOnMainThread()) {
            Logger.w("trying to run sqliteSaveDownloadedData() on main thread. Return.");
            return;
        }

        // Bail out if the state has changed to canceling, suspended, or available
        if (this.state != OfflineMapDownloaderState.RUNNING) {
            Logger.w("sqliteSaveDownloadedData() is not in a Running state so bailing.  State = " + this.state);
            return;
        }

        // Open the database read-write and multi-threaded. The slightly obscure c-style variable names here and below are
        // used to stay consistent with the sqlite documentaion. Continue by inserting an image blob into the data table
        final LiteOrm liteOrm = OfflineDatabase.getLiteOrm(context);
        // String substring(int beginIndex) ：取从beginIndex位置开始到结束的子字符串。
        liteOrm.save(new TileBean(url.substring(OfflineTileProvider.TILEURI_SUB_INDEX), data));
        // 对URL进行遍历判断，数据库中有？数据一样？一样就跳过，否则就更新数据即可。

        // Update the progress
        notifyDelegateOfProgress(this.totalFilesWritten.incrementAndGet(), this.totalFilesExpectedToWrite.get());
        Logger.d("totalFilesWritten = " + this.totalFilesWritten + "; totalFilesExpectedToWrite = " + this.totalFilesExpectedToWrite.get());
    }

    /*
    *  Implementation: sqlite stuff
    */
    private void finishUpDownloadProcess() {
        if (this.state == OfflineMapDownloaderState.RUNNING) {
            Logger.i("Just finished downloading all materials.  Persist the OfflineMapDatabase, change the state, and call it a day.");
            // This is what to do when we've downloaded all the files
            notifyDelegateOfCompletionWithOfflineMapDatabase();
            this.state = OfflineMapDownloaderState.AVAILABLE;
            notifyDelegateOfStateChange();
        }
    }

    /*
        API: Begin an offline map download
    */
    public void beginDownloadingMap(VisibleMapArea mapRegion, int minimumZ, int maximumZ) {
        if (state != OfflineMapDownloaderState.AVAILABLE) {
            Logger.w("state doesn't equal MBXOfflineMapDownloaderStateAvailable so return.  state = " + state);
            return;
        }

        // Start a download job to retrieve all the resources needed for using the specified map offline
        this.state = OfflineMapDownloaderState.RUNNING;
        notifyDelegateOfStateChange();

        // 计算出需要下载的URL路径
        final ArrayList<String> urls = new ArrayList<>();

        // Loop through the zoom levels and lat/lon bounds to generate a list of urls which should be included in the offline map
        //
        double minLat = Math.min(
                Math.min(mapRegion.farLeft.getLatitude(), mapRegion.nearLeft.getLatitude()),
                Math.min(mapRegion.farRight.getLatitude(), mapRegion.nearRight.getLatitude()));
        double maxLat = Math.max(
                Math.max(mapRegion.farLeft.getLatitude(), mapRegion.nearLeft.getLatitude()),
                Math.max(mapRegion.farRight.getLatitude(), mapRegion.nearRight.getLatitude()));

        double minLon = Math.min(
                Math.min(mapRegion.farLeft.getLongitude(), mapRegion.nearLeft.getLongitude()),
                Math.min(mapRegion.farRight.getLongitude(), mapRegion.nearRight.getLongitude()));
        double maxLon = Math.max(
                Math.max(mapRegion.farLeft.getLongitude(), mapRegion.nearLeft.getLongitude()),
                Math.max(mapRegion.farRight.getLongitude(), mapRegion.nearRight.getLongitude()));

        int minX;
        int maxX;
        int minY;
        int maxY;
        int tilesPerSide;

        Logger.i("Generating urls for tiles from zoom " + minimumZ + " to zoom " + maximumZ);

        for (int zoom = minimumZ; zoom <= maximumZ; zoom++) {
            tilesPerSide = Double.valueOf(Math.pow(2.0, zoom)).intValue();
            minX = Double.valueOf(Math.floor(((minLon + 180.0) / 360.0) * tilesPerSide)).intValue();
            maxX = Double.valueOf(Math.floor(((maxLon + 180.0) / 360.0) * tilesPerSide)).intValue();
            minY = Double.valueOf(Math.floor((1.0 - (Math.log(Math.tan(maxLat * Math.PI / 180.0) + 1.0 / Math.cos(maxLat * Math.PI / 180.0)) / Math.PI)) / 2.0 * tilesPerSide)).intValue();
            maxY = Double.valueOf(Math.floor((1.0 - (Math.log(Math.tan(minLat * Math.PI / 180.0) + 1.0 / Math.cos(minLat * Math.PI / 180.0)) / Math.PI)) / 2.0 * tilesPerSide)).intValue();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    urls.add(OfflineMapUtils.getMapTileURL(zoom, x, y));
                }
            }
        }

        Logger.i(urls.size() + " urls generated.");

        // There aren't any marker icons to worry about, so just create database and start downloading
        startDownloadProcess(urls);
    }

    /**
     * Private method for Starting the Whole Download Process
     *
     * @param urls Map urls
     */
    private void startDownloadProcess(final ArrayList<String> urls) {
//        if (state != OfflineMapDownloaderState.AVAILABLE) {
//            Logger.w("state doesn't equal AVAILABLE so return.  state = " + state);
//            return;
//        }
//
//        // Start a download job to retrieve all the resources needed for using the specified map offline
//        this.state = OfflineMapDownloaderState.RUNNING;
//        notifyDelegateOfStateChange();

        downloadsScheduler.execute(new Runnable() {
            @Override
            public void run() {
                startDownloading(urls);
            }
        });
    }

    /**
     * The possible states of the offline map downloader.
     */
    public enum OfflineMapDownloaderState {
        /**
         * An offline map download job is in progress.
         */
        RUNNING,
        /**
         * An offline map download job is being canceled.
         */
        CANCELLING,
        /**
         * The offline map downloader is ready to begin a new offline map download job.
         */
        AVAILABLE
    }
}
