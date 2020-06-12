package com.dronekit.core.drone.autopilot;

import com.dronekit.communication.model.DataLink;
import com.dronekit.core.MAVLink.WaypointManager;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Altitude;
import com.dronekit.core.drone.property.Attitude;
import com.dronekit.core.drone.property.Battery;
import com.dronekit.core.drone.property.GimbalOrientation;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.property.Home;
import com.dronekit.core.drone.property.Signal;
import com.dronekit.core.drone.property.Speed;
import com.dronekit.core.drone.property.Vibration;
import com.dronekit.core.drone.variables.Camera;
import com.dronekit.core.drone.variables.GuidedPoint;
import com.dronekit.core.drone.variables.HeartBeat;
import com.dronekit.core.drone.variables.Magnetometer;
import com.dronekit.core.drone.variables.MissionStats;
import com.dronekit.core.drone.variables.RC;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.drone.variables.StreamRates;
import com.dronekit.core.drone.variables.Type;
import com.dronekit.core.drone.variables.calibration.AccelCalibration;
import com.dronekit.core.drone.variables.calibration.MagnetometerCalibrationImpl;
import com.dronekit.core.mission.Mission;
import com.dronekit.core.survey.CameraInfo;

import java.util.List;

/**
 * 定义一系列飞行器属性接口类[接口回调]
 */
public interface Drone {

    // 是否连接成功
    boolean isConnected();

    // 销毁飞行器对象
    void destroy();

    // 是否存活
    boolean isConnectionAlive();

    // 获取心跳
    HeartBeat getHeartBeat();

    // 获取MAVLink的版本号
    int getMavlinkVersion();

    // 获取SysId
    byte getSysid();

    // 获取CompId
    byte getCompid();

    // 获取状态
    State getState();

    // 获取参数管理器
    ParameterManager getParameterManager();

    // 获取飞行器类型
    Type getType();

    // 获取Mavlink客户端
    DataLink.DataLinkProvider getMavClient();

    // 获取航点管理器
    WaypointManager getWaypointManager();

    // 获取任务管理器
    Mission getMission();

    // 获取流速率
    StreamRates getStreamRates();

    // 获取任务统计
    MissionStats getMissionStats();

    // 获取“引导点”模块
    GuidedPoint getGuidedPoint();

    // 获取加速度计校准模块
    AccelCalibration getCalibrationSetup();

    // 获取磁罗盘校准模块
    MagnetometerCalibrationImpl getMagnetometerCalibration();

    // 获取固件版本
    String getFirmwareVersion();

    // 获取摄像机模块
    Camera getCamera();

    // 高度
    Altitude getAltitude();

    // 姿态
    Attitude getAttitude();

    // 电池
    Battery getBattery();

    // GPS
    Gps getVehicleGps();

    // Home
    Home getVehicleHome();

    // RC摇杆
    RC getVehicleRC();

    // 信号
    Signal getSignal();

    // 速度
    Speed getSpeed();

    // 震动系数
    Vibration getVibration();

    GimbalOrientation getGimbalOrientation();

    Magnetometer getMagnetometer();


    List<CameraInfo> getCameraDetails();
}
