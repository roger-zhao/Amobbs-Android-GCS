package org.farring.gcs;

import android.content.Context;
import android.os.Handler;
import android.support.multidex.MultiDexApplication;

import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.Drone;
import com.iflytek.cloud.SpeechUtility;
import com.orhanobut.logger.Logger;
import com.squareup.leakcanary.LeakCanary;

import org.farring.gcs.proxy.mission.MissionProxy;

import cn.bmob.v3.Bmob;

public class FishDroneGCSApp extends MultiDexApplication {

    private static Context context;
    private DroneManager droneManager;
    private MissionProxy missionProxy;

    // 建立全局静态变量，便于调用！
    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init("调试");
        // 获取全局上下环境
        context = getApplicationContext();

        LeakCanary.install(this);

        /***** 统一初始化Bugly产品 *****/
        // Bugly.init(this, "900016184", true);

        // 科大讯飞云语音
        SpeechUtility.createUtility(this, "appid=55b373d4");
        // 初始化LeanCloud
        Bmob.initialize(this, "335c2dab71e6bb52f48077bc00e92355");

        droneManager = new DroneManager(this, new Handler());
        missionProxy = new MissionProxy(this, getDrone());
    }

    public DroneManager getDroneManager() {
        return droneManager;
    }

    public Drone getDrone() {
        return this.droneManager.getDrone();
    }

    public MissionProxy getMissionProxy() {
        return this.missionProxy;
    }

}
