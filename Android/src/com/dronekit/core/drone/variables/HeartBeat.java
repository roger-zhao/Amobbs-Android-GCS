package com.dronekit.core.drone.variables;

import android.os.Handler;

import com.MAVLink.common.msg_heartbeat;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.gcs.GCSHeartbeat;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class HeartBeat extends DroneVariable {
    // Mavlink版本号
    public static final int INVALID_MAVLINK_VERSION = -1;
    // 连接超时
    private static final long CONNECTION_TIMEOUT = 5000; //ms
    // 时间：心跳超时（5秒）
    private static final long HEARTBEAT_NORMAL_TIMEOUT = 5000; //ms
    // 时间：心跳丢失超时（15秒）
    private static final long HEARTBEAT_LOST_TIMEOUT = 15000; //ms
    // 时间：IMU校准超时（35秒）
    private static final long HEARTBEAT_IMU_CALIBRATION_TIMEOUT = 35000; //ms

    // 看门狗（用于定时）
    public final Handler watchdog;
    // 心跳包对象（用于发送心跳消息到飞行器）
    private final GCSHeartbeat gcsHeartbeat;
    // 设置当前心跳包状态
    public HeartbeatState heartbeatState = HeartbeatState.FIRST_HEARTBEAT;
    // 使用Runabale方式进行计时操作，用于检测心跳包是否超时
    public final Runnable watchdogCallback = new Runnable() {
        @Override
        public void run() {
            onHeartbeatTimeout();
        }
    };

    // 全局SystemID和ComponetID
    private byte sysid = 1;
    private byte compid = 1;
    /**
     * Stores the version of the mavlink protocol.存储Mavlink版本号
     */
    private short mMavlinkVersion = INVALID_MAVLINK_VERSION;

    // 构造函数
    public HeartBeat(Drone myDrone, Handler handler) {
        super(myDrone);
        this.watchdog = handler;
        this.gcsHeartbeat = new GCSHeartbeat(myDrone, 1);
        EventBus.getDefault().register(this);
    }

    // 获取SystemID
    public byte getSysid() {
        return sysid;
    }

    // 获取ComponentID
    public byte getCompid() {
        return compid;
    }

    /**
     * @return the version of the mavlink protocol.获取Mavlink版本号
     */
    public short getMavlinkVersion() {
        return mMavlinkVersion;
    }

    // 心跳正常
    public void onHeartbeat(msg_heartbeat msg) {
        sysid = (byte) msg.sysid;
        compid = (byte) msg.compid;
        mMavlinkVersion = msg.mavlink_version;

        // 根据心跳包不同状态执行不同操作
        switch (heartbeatState) {
            case FIRST_HEARTBEAT:
                notifyConnected();
                EventBus.getDefault().post(AttributeEvent.HEARTBEAT_FIRST);
                EventBus.getDefault().post(AttributeEvent.STATE_CONNECTED);
                break;

            case LOST_HEARTBEAT:
                EventBus.getDefault().post(AttributeEvent.HEARTBEAT_RESTORED);
                break;
        }
        // 心跳正常
        heartbeatState = HeartbeatState.NORMAL_HEARTBEAT;
        restartWatchdog(HEARTBEAT_NORMAL_TIMEOUT);
    }

    // 是否有心跳
    public boolean hasHeartbeat() {
        return heartbeatState != HeartbeatState.FIRST_HEARTBEAT;
    }

    // 是否连接正常(是否丢失心跳)
    public boolean isConnectionAlive() {
        return heartbeatState != HeartbeatState.LOST_HEARTBEAT;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case CALIBRATION_IMU:
                // Set the heartbeat in imu calibration mode.
                heartbeatState = HeartbeatState.IMU_CALIBRATION;
                // 重启看门狗，并重新计时
                restartWatchdog(HEARTBEAT_IMU_CALIBRATION_TIMEOUT);
                break;

            case CHECKING_VEHICLE_LINK:
                // 使能发送心跳包消息
                gcsHeartbeat.setActive(true);
                notifyConnecting();
                break;

            case STATE_CONNECTION_FAILED:
            case STATE_DISCONNECTED:
                // 失能发送心跳包消息
                gcsHeartbeat.setActive(false);
                notifyDisconnected();
                break;
        }
    }

    // 连接中
    private void notifyConnecting() {
        restartWatchdog(CONNECTION_TIMEOUT);
    }

    private void notifyConnected() {
        restartWatchdog(HEARTBEAT_NORMAL_TIMEOUT);
    }

    // 连接失败时移除看门狗，并设置状态为“FIRST_HEARTBEAT”，Mavlink版本号为“-1”
    private void notifyDisconnected() {
        watchdog.removeCallbacks(watchdogCallback);
        heartbeatState = HeartbeatState.FIRST_HEARTBEAT;
        mMavlinkVersion = INVALID_MAVLINK_VERSION;
    }

    // 处理心跳超时
    private void onHeartbeatTimeout() {
        switch (heartbeatState) {
            case IMU_CALIBRATION:
                // 重启看门狗，并重新定时
                restartWatchdog(HEARTBEAT_IMU_CALIBRATION_TIMEOUT);
                // 通知全局，校准超时
                EventBus.getDefault().post(AttributeEvent.CALIBRATION_IMU_TIMEOUT);
                break;

            case FIRST_HEARTBEAT:
                // 通知全局，连接失败
                EventBus.getDefault().post(AttributeEvent.STATE_CONNECTION_FAILED);
                break;

            default:
                heartbeatState = HeartbeatState.LOST_HEARTBEAT;
                restartWatchdog(HEARTBEAT_LOST_TIMEOUT);
                EventBus.getDefault().post(AttributeEvent.HEARTBEAT_TIMEOUT);
                break;
        }
    }

    // 重启看门狗，定时timeout毫秒
    private void restartWatchdog(long timeout) {
        // re-start watchdog
        watchdog.removeCallbacks(watchdogCallback);
        watchdog.postDelayed(watchdogCallback, timeout);
    }

    // 心跳状态（起跳，丢失心跳，正常心跳，IMU校准）
    public enum HeartbeatState {
        FIRST_HEARTBEAT, LOST_HEARTBEAT, NORMAL_HEARTBEAT, IMU_CALIBRATION
    }
}
