package org.farring.gcs.maps.providers.GoogleMap.offline.Downloader;

import org.farring.gcs.maps.providers.GoogleMap.offline.Downloader.MapDownloader.OfflineMapDownloaderState;

// 下载接口
public interface MapDownloaderListener {

    // 状态改变
    void stateChanged(OfflineMapDownloaderState newState);

    // 初始化文件总数
    void initialCountOfFiles(int numberOfFiles);

    // 进度更新
    void progressUpdate(int numberOfFilesWritten, int numberOfFilesExcepted);

    // 网络连接错误
    void networkConnectivityError(Throwable error);

    // SQL错误
    void sqlLiteError(Throwable error);

    // Http错误
    void httpStatusError(int status, String url);

    // 离线地图完成
    void completionOfOfflineDatabaseMap();
}
