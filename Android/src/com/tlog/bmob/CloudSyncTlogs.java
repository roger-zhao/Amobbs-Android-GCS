package com.tlog.bmob;

import android.content.Context;

import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.tlog.database.LogRecordBean;
import com.tlog.database.LogsRecordDatabase;

import org.farring.gcs.utils.file.DirectoryPath;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UploadFileListener;

/**
 * Created by Administrator on 2016/4/15.
 */
public class CloudSyncTlogs {

    private final Context context;
    private final String userName;
    private final CloudSyncListener cloudSyncListener;
    private final LiteOrm liteOrm;
    private AtomicInteger needUploadCounts = new AtomicInteger(0);
    private AtomicInteger finishedUploadCounts = new AtomicInteger(0);
    private AtomicInteger needDownloadCounts = new AtomicInteger(0);
    private AtomicInteger finishedDownloadCounts = new AtomicInteger(0);

    public CloudSyncTlogs(Context context, String userName, CloudSyncListener cloudSyncListener) {
        this.context = context;
        this.userName = userName;
        this.cloudSyncListener = cloudSyncListener;
        liteOrm = LogsRecordDatabase.getLiteOrm(context);
    }

    public void cloudSync() {
        // ------------ 本地 和 网络 的比对 ------------【上传】
        List<LogRecordBean> logsRecords = liteOrm.query(new QueryBuilder<>(LogRecordBean.class)
                .whereEquals(LogRecordBean.COL_USERNAME, userName));

        for (final LogRecordBean logRecord : logsRecords) {
            BmobQuery<TLogFileBmobObject> query = new BmobQuery<>();
            query.addWhereEqualTo("fileMD5", logRecord.getFileMD5());
            //执行查询方法
            query.findObjects(context, new FindListener<TLogFileBmobObject>() {
                @Override
                public void onSuccess(List<TLogFileBmobObject> tLogFileList) {
                    if (tLogFileList.size() == 0) {
                        needUploadCounts.getAndIncrement();
                        uploadTlog(logRecord.getFilePath());

                        if (cloudSyncListener != null)
                            cloudSyncListener.onRunning(needUploadCounts.get() - finishedUploadCounts.get(),
                                    needDownloadCounts.get() - finishedDownloadCounts.get());
                    }
                }

                @Override
                public void onError(int code, String msg) {
                }
            });
        }

        // ------------ 网络 和 本地 的比对 ------------【下载】
        BmobQuery<TLogFileBmobObject> query = new BmobQuery<>();
        query.addWhereEqualTo("userName", userName).order("createdAt").setLimit(1000);
        query.findObjects(context, new FindListener<TLogFileBmobObject>() {// 执行查询方法
            @Override
            public void onSuccess(List<TLogFileBmobObject> TLogFileBmobObjects) {
                for (TLogFileBmobObject tlog : TLogFileBmobObjects) {
                    BmobFile bmobfile = tlog.getBmobFile();

                    List<LogRecordBean> list = liteOrm.query(new QueryBuilder<>(LogRecordBean.class)
                            .whereEquals(LogRecordBean.COL_FILEMD5, tlog.getFileMD5()));

                    if (list.size() == 0) {
                        downloadFile(bmobfile);
                        needDownloadCounts.getAndIncrement();
                    } else {
                        if (!new File(list.get(0).getFilePath()).exists()) {
                            downloadFile(bmobfile); // 检查文件是否存在，如果不在，则下载
                            needDownloadCounts.incrementAndGet();
                        }
                    }

                    if (cloudSyncListener != null)
                        cloudSyncListener.onRunning(needUploadCounts.get() - finishedUploadCounts.get(),
                                needDownloadCounts.get() - finishedDownloadCounts.get());
                }
            }

            @Override
            public void onError(int code, String msg) {
                if (cloudSyncListener != null)
                    cloudSyncListener.onFailure(msg);
            }
        });
    }

    private void downloadFile(BmobFile file) {
        final File saveFile = new File(DirectoryPath.getTlogStringPath(), file.getFilename());
        file.download(context, saveFile, new DownloadFileListener() {
            @Override
            public void onSuccess(String savePath) {
                LogsRecordDatabase.saveTlogToDBAsync(saveFile);
                finishedDownloadCounts.getAndIncrement();

                if (cloudSyncListener != null)
                    cloudSyncListener.onRunning(needUploadCounts.get() - finishedUploadCounts.get(),
                            needDownloadCounts.get() - finishedDownloadCounts.get());
            }

            @Override
            public void onFailure(int code, String msg) {
                if (cloudSyncListener != null)
                    cloudSyncListener.onFailure(msg);
            }
        });
    }

    /**
     * 上传Tlog文件
     *
     * @param filePath
     */
    private void uploadTlog(String filePath) {
        final File file = new File(filePath);
        final BmobFile bmobFile = new BmobFile(file);
        bmobFile.uploadblock(context, new UploadFileListener() {
            @Override
            public void onSuccess() {
                new TLogFileBmobObject(bmobFile, userName, MD5FileUtil.md5(file)).save(context, null);
                finishedUploadCounts.getAndIncrement();

                if (cloudSyncListener != null)
                    cloudSyncListener.onRunning(needUploadCounts.get() - finishedUploadCounts.get(),
                            needDownloadCounts.get() - finishedDownloadCounts.get());
            }

            @Override
            public void onFailure(int code, String msg) {
                if (cloudSyncListener != null)
                    cloudSyncListener.onFailure(msg);
            }
        });
    }


    public interface CloudSyncListener {
        void onFailure(String msg);

        void onRunning(int remainUploadCounts, int remainDownloadCounts);
    }
}
