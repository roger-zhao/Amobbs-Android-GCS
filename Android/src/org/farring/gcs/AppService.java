package org.farring.gcs;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.dronekit.core.drone.autopilot.Drone;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.notifications.NotificationHandler;

public class AppService extends Service implements AMapLocationListener {

    private NotificationHandler notificationHandler;
    private AMapLocationClient locationClient = null;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTED:
                if (notificationHandler != null)
                    notificationHandler.init();
                break;

            case STATE_DISCONNECTED:
                if (notificationHandler != null) {
                    notificationHandler.terminate();
                }
                break;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final Drone drone = ((FishDroneGCSApp) getApplication()).getDrone();

        final Context context = getApplicationContext();
        notificationHandler = new NotificationHandler(context, drone);

        if (drone.isConnected()) {
            notificationHandler.init();
        }
        EventBus.getDefault().register(this);

        startGaodeLocate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);

        if (notificationHandler != null)
            notificationHandler.terminate();

        // 定位清除
        if (null != locationClient) {
            // 停止定位
            locationClient.stopLocation();
            locationClient.onDestroy();
            locationClient = null;// 垃圾回收
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null)
            return;

        if (aMapLocation.getErrorCode() == 0) {
            EventBus.getDefault().post(aMapLocation);
        }
    }

    // 开启定位的方法
    private void startGaodeLocate() {
        // 如果是尚未启动定位则启动，因此如果已经启动定位情况下，则设置的参数会以第一个设置的为准
        locationClient = new AMapLocationClient(this);
        // 设置定位监听
        locationClient.setLocationListener(this);

        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        // 设置定位模式为高精度模式
        locationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
        // 持续定位
        locationOption.setOnceLocation(false);
        // 设置发送定位请求的时间间隔
        locationOption.setInterval(1000);
        // 设置定位参数
        locationClient.setLocationOption(locationOption);

        // 启动定位
        locationClient.startLocation();
    }
}
