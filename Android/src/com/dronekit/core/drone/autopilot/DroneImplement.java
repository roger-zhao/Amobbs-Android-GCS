package com.dronekit.core.drone.autopilot;

import android.content.Context;
import android.os.Handler;

import com.dronekit.communication.model.DataLink.DataLinkProvider;
import com.dronekit.core.MAVLink.MavLinkCommands;
import com.dronekit.core.MAVLink.WaypointManager;
import com.dronekit.core.drone.commandListener.ICommandListener;
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
import com.dronekit.core.drone.variables.ApmModes;
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
import com.dronekit.core.error.CommandExecutionError;
import com.dronekit.core.mission.Mission;
import com.dronekit.core.model.AutopilotWarningParser;
import com.dronekit.core.survey.CameraInfo;
import com.dronekit.utils.file.IO.CameraInfoLoader;
import com.evenbus.AttributeEvent;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * Base drone implementation.
 * Supports mavlink messages belonging to the common set: https://pixhawk.ethz.ch/mavlink/
 */
public class DroneImplement implements Drone {

    // 【飞行器属性实体类】[单一实例模式，保证全局只有一个该属性对象]
    private final Type type;
    private final Altitude altitude;
    private final Speed speed;
    private final Battery battery;
    private final Signal signal;
    private final Attitude attitude;
    private final Vibration vibration;
    private final Home vehicleHome;
    private final Gps vehicleGps;
    private final State state;
    private final StreamRates streamRates;
    private final RC rc;
    private final Mission mission;
    private final Camera camera;
    private final Magnetometer magnetometer;
    private final GimbalOrientation gimbalOrientation;
    // 【校准类】
    private final AccelCalibration accelCalibration;
    private final MagnetometerCalibrationImpl magnetometerCalibration;
    // 【功能类】
    private final GuidedPoint guidedPoint;
    private final HeartBeat heartbeat;
    private final WaypointManager waypointManager;
    private final ParameterManager parameterManager;
    private final MissionStats missionStats;
    // 【其它】
    private final DataLinkProvider mavClient;
    private CameraInfoLoader cameraInfoLoader;
    private List<CameraInfo> cachedCameraDetails;

    /**
     * 构造方法
     *
     * @param context       上下文
     * @param handler       Handler
     * @param mavClient     Mavlink客户端
     * @param warningParser
     */
    public DroneImplement(Context context, Handler handler, DataLinkProvider mavClient, AutopilotWarningParser warningParser) {
        this.mavClient = mavClient;
        EventBus.getDefault().register(this);

        // 读取相机消息
        cameraInfoLoader = new CameraInfoLoader(context);
        // 心跳
        heartbeat = new HeartBeat(this, handler);
        // 飞行器类型
        this.type = new Type(this);
        // 任务统计
        this.missionStats = new MissionStats(this);
        // 流速
        this.streamRates = new StreamRates(this);
        // 状态
        this.state = new State(this, handler, warningParser);
        // 参数管理类
        this.parameterManager = new ParameterManager(this, context, handler);
        // 高度
        this.altitude = new Altitude();
        // 速度
        this.speed = new Speed();
        // 电池
        this.battery = new Battery();
        // 信号
        this.signal = new Signal();
        // 姿态
        this.attitude = new Attitude();
        // 震动系数
        this.vibration = new Vibration();
        // Home
        this.vehicleHome = new Home();
        // GPS
        this.vehicleGps = new Gps();
        // 遥控
        this.rc = new RC(this);
        // 任务
        this.mission = new Mission(this);
        // 指点
        this.guidedPoint = new GuidedPoint(this, handler);
        // 加速度校准
        this.accelCalibration = new AccelCalibration(this, handler);
        // 磁罗盘校准
        this.magnetometerCalibration = new MagnetometerCalibrationImpl(this);
        // 磁罗盘
        this.magnetometer = new Magnetometer(this);
        // 相机
        this.camera = new Camera(this);
        // 航点管理
        this.waypointManager = new WaypointManager(this, handler);
        // 云台控制
        this.gimbalOrientation = new GimbalOrientation(this);
    }

    @Override
    public HeartBeat getHeartBeat() {
        return heartbeat;
    }

    @Override
    public MissionStats getMissionStats() {
        return missionStats;
    }

    @Override
    public Mission getMission() {
        return mission;
    }

    @Override
    public Camera getCamera() {
        return camera;
    }

    @Override
    public Altitude getAltitude() {
        return altitude;
    }

    @Override
    public Attitude getAttitude() {
        return attitude;
    }

    @Override
    public Battery getBattery() {
        return battery;
    }

    @Override
    public Gps getVehicleGps() {
        return vehicleGps;
    }

    @Override
    public Home getVehicleHome() {
        return vehicleHome;
    }

    @Override
    public RC getVehicleRC() {
        return rc;
    }

    @Override
    public Signal getSignal() {
        return signal;
    }

    @Override
    public Speed getSpeed() {
        return speed;
    }

    @Override
    public Vibration getVibration() {
        return vibration;
    }

    @Override
    public GimbalOrientation getGimbalOrientation() {
        return gimbalOrientation;
    }

    @Override
    public Magnetometer getMagnetometer() {
        return magnetometer;
    }

    @Override
    public GuidedPoint getGuidedPoint() {
        return guidedPoint;
    }

    @Override
    public AccelCalibration getCalibrationSetup() {
        return accelCalibration;
    }

    @Override
    public WaypointManager getWaypointManager() {
        return waypointManager;
    }

    @Override
    public MagnetometerCalibrationImpl getMagnetometerCalibration() {
        return magnetometerCalibration;
    }

    @Override
    public void destroy() {
        ParameterManager parameterManager = getParameterManager();
        if (parameterManager != null)
            parameterManager.setParameterListener(null);

        MagnetometerCalibrationImpl magnetometer = getMagnetometerCalibration();
        if (magnetometer != null)
            magnetometer.setListener(null);
    }

    @Override
    public String getFirmwareVersion() {
        return type.getFirmwareVersion();
    }

    @Override
    public ParameterManager getParameterManager() {
        return parameterManager;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean isConnected() {
        return mavClient.isConnected() && heartbeat.hasHeartbeat();
    }

    @Override
    public boolean isConnectionAlive() {
        return heartbeat.isConnectionAlive();
    }

    @Override
    public byte getSysid() {
        return heartbeat.getSysid();
    }

    @Override
    public byte getCompid() {
        return heartbeat.getCompid();
    }

    @Override
    public int getMavlinkVersion() {
        return heartbeat.getMavlinkVersion();
    }

    @Override
    public StreamRates getStreamRates() {
        return streamRates;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_DISCONNECTED:
                signal.setValid(false);
                break;
        }
    }

    @Override
    public DataLinkProvider getMavClient() {
        return mavClient;
    }

    /**
     * [Dis|En]able manual control on the vehicle.
     * The result of the action will be conveyed through the passed listener.
     *
     * @param enable   True to enable manual control, false to disable.
     * @param listener Register a callback to receive the result of the operation.
     * @since 2.6.9
     */
    protected boolean enableManualControl(final boolean enable, ICommandListener listener) {
        if (enable) {
            listener.onSuccess();
        } else {
            listener.onError(CommandExecutionError.COMMAND_UNSUPPORTED);
        }
        return true;
    }

    /**
     * Arm or disarm the connected drone.
     *
     * @param doArm           true to arm, false to disarm.
     * @param emergencyDisarm true to skip landing check and disarm immediately,
     *                        false to disarm only if it is safe to do so.
     * @param listener        Register a callback to receive update of the command execution state.
     */
    protected boolean performArming(boolean doArm, boolean emergencyDisarm, ICommandListener listener) {
        if (!doArm && emergencyDisarm) {
            MavLinkCommands.sendFlightTermination(this, listener);
        } else {
            MavLinkCommands.sendArmMessage(this, doArm, false, listener);
        }
        return true;
    }

    /**
     * Change the vehicle mode for the connected drone.
     *
     * @param newMode  new vehicle mode.
     * @param listener Register a callback to receive update of the command execution state.
     */
    protected boolean setVehicleMode(ApmModes newMode, ICommandListener listener) {
        if (newMode != null) {
            switch (newMode) {
                case ROTOR_LAND:
                    MavLinkCommands.sendNavLand(this, listener);
                    break;

                case ROTOR_RTL:
                    MavLinkCommands.sendNavRTL(this, listener);
                    break;

                case ROTOR_GUIDED:
                    MavLinkCommands.sendPause(this, listener);
                    break;

                case ROTOR_AUTO:
                    MavLinkCommands.startMission(this, listener);
                    break;
            }
        }
        return true;
    }

    /**
     * Move the vehicle along the specified normalized velocity vector.
     *
     * @param xAxis    x velocity normalized to the range [-1.0f, 1.0f]. Generally correspond to the pitch of the vehicle.
     * @param yAxis    y velocity normalized to the range [-1.0f, 1.0f]. Generally correspond to the roll of the vehicle.
     * @param zAxis    z velocity normalized to the range [-1.0f, 1.0f]. Generally correspond to the thrust of the vehicle.
     * @param listener Register a callback to receive update of the command execution state.
     */
    protected boolean setVelocity(float xAxis, float yAxis, float zAxis, ICommandListener listener) {
        short x = (short) (xAxis * 1000);
        short y = (short) (yAxis * 1000);
        short z = (short) (zAxis * 1000);

        MavLinkCommands.sendManualControl(this, x, y, z, (short) 0, 0, listener);
        return true;
    }

    @Override
    public Type getType() {
        return type;
    }


    /**
     * Retrieves the set of camera info provided by the app.
     *
     * @return a list of {@link CameraInfo} objects.
     */
    @Override
    public synchronized List<CameraInfo> getCameraDetails() {
        if (cachedCameraDetails == null) {
            List<String> cameraInfoNames = cameraInfoLoader.getCameraInfoList();

            List<CameraInfo> cameraInfos = new ArrayList<>(cameraInfoNames.size());
            for (String infoName : cameraInfoNames) {
                try {
                    cameraInfos.add(cameraInfoLoader.openFile(infoName));
                } catch (Exception e) {
                    Logger.e(e, e.getMessage());
                }
            }

            List<CameraInfo> cameraDetails = new ArrayList<>(cameraInfos.size());
            for (CameraInfo camInfo : cameraInfos) {
                cameraDetails.add(new CameraInfo(camInfo.name, camInfo.sensorWidth,
                        camInfo.sensorHeight, camInfo.sensorResolution, camInfo.focalLength,
                        camInfo.overlap, camInfo.sidelap, camInfo.isInLandscapeOrientation));
            }

            cachedCameraDetails = cameraDetails;
        }

        return cachedCameraDetails;
    }
}
