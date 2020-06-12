package com.dronekit.core.drone.variables;

import android.os.Handler;
import android.os.SystemClock;

import com.MAVLink.ardupilotmega.msg_ekf_status_report;
import com.MAVLink.enums.EKF_STATUS_FLAGS;
import com.dronekit.core.MAVLink.MavLinkCommands;
import com.dronekit.core.MAVLink.MavLinkWaypoint;
import com.dronekit.core.MAVLink.WaypointManager;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.APMConstants;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.drone.property.EkfStatus;
import com.dronekit.core.error.CommandExecutionError;
import com.dronekit.core.model.AutopilotWarningParser;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;


// 飞行器状态
public class State extends DroneVariable {
    private static final long ERROR_TIMEOUT = 5000L;

    private final AutopilotWarningParser warningParser;
    private final Handler handler;
    private msg_ekf_status_report ekfStatusReport;
    private boolean isEkfPositionOk;

    private String errorId;
    private final Runnable watchdogCallback = new Runnable() {
        @Override
        public void run() {
            resetWarning();
        }
    };
    private boolean armed = false;
    private boolean isFlying = false;
    private ApmModes mode = ApmModes.UNKNOWN;
    private int custom_mode = 0;
    private String pixhawkSerialNumber = "";

    // flightTimer
    // ----------------
    private long startTime = 0;
    private long elapsedFlightTime = 0;
    // ************** EKF *******************
    private EkfStatus ekfStatus = new EkfStatus();

    public State(Drone myDrone, Handler handler, AutopilotWarningParser warningParser) {
        super(myDrone);
        this.handler = handler;
        this.warningParser = warningParser;
        this.errorId = warningParser.getDefaultWarning();
        resetFlightTimer();
    }

    private static boolean areEkfStatusEquals(msg_ekf_status_report one, msg_ekf_status_report two) {
        return one == two || !(one == null || two == null) && one.toString().equals(two.toString());
    }

    // ****************************** 序列号 *****************************
    public String getPixhawkSerialNumber() {
        return pixhawkSerialNumber;
    }

    public void setPixhawkSerialNumber(String pixhawkSerialNumber) {
        this.pixhawkSerialNumber = pixhawkSerialNumber;
    }

    public boolean isArmed() {
        return armed;
    }

    // ************* 【设置解锁状态】 ***************************
    public void setArmed(boolean newState) {
        if (this.armed != newState) {
            this.armed = newState;
            EventBus.getDefault().post(AttributeEvent.STATE_ARMING);

            // 加载航点
            if (newState) {
                WaypointManager waypointManager = myDrone.getWaypointManager();
                if (waypointManager != null) {
                    waypointManager.getWaypoints();
                } else {
                    if (mode == ApmModes.ROTOR_RTL || mode == ApmModes.ROTOR_LAND) {
                        changeFlightMode(ApmModes.ROTOR_LOITER, null);  // When disarming set the mode back to loiter so we can do a takeoff in the future.
                    }
                }
            }
        }
        checkEkfPositionState(this.ekfStatusReport);
    }

    public boolean isFlying() {
        return isFlying;
    }

    public ApmModes getMode() {
        return mode;
    }
    public int getCustom_mode() {
        return custom_mode;
    }

    // ***************************** 【模式 Mode】 ***************************
    // 获取模式
    public void setMode(ApmModes mode) {
        if (this.mode != mode) {
            this.mode = mode;
            EventBus.getDefault().post(AttributeEvent.STATE_VEHICLE_MODE);
        }
    }
    public void setMode(ApmModes mode, int custom_mode) {
        if (this.mode != mode) {
            this.mode = mode;
            this.custom_mode = custom_mode;
            EventBus.getDefault().post(AttributeEvent.STATE_VEHICLE_MODE);
        }
    }
    public String getErrorId() {
        return errorId;
    }

    public void setIsFlying(boolean newState) {
        if (newState != isFlying) {
            isFlying = newState;
            EventBus.getDefault().post(AttributeEvent.STATE_UPDATED);

            if (isFlying) {
                resetFlightTimer();
            } else {
                stopTimer();
            }
        }
    }

    public boolean parseAutopilotError(String errorMsg) {
        String parsedError = warningParser.parseWarning(myDrone, errorMsg);
        if (parsedError == null || parsedError.trim().isEmpty())
            return false;

        if (!parsedError.equals(this.errorId)) {
            this.errorId = parsedError;
            EventBus.getDefault().post(AttributeEvent.AUTOPILOT_ERROR);
        }

        handler.removeCallbacks(watchdogCallback);
        this.handler.postDelayed(watchdogCallback, ERROR_TIMEOUT);
        return true;
    }

    public void repeatWarning() {
        if (errorId == null || errorId.length() == 0 || errorId.equals(warningParser.getDefaultWarning()))
            return;

        handler.removeCallbacks(watchdogCallback);
        this.handler.postDelayed(watchdogCallback, ERROR_TIMEOUT);
    }

    // 设置模式
    public void changeFlightMode(ApmModes mode, final ICommandListener listener) {
        if (this.mode == mode) {
            if (listener != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSuccess();
                    }
                });
            }
            return;
        }

        if (ApmModes.isValid(mode)) {
            MavLinkCommands.changeFlightMode(myDrone, mode, listener);
        } else {
            if (listener != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(CommandExecutionError.COMMAND_FAILED);
                    }
                });
            }
        }
    }

    // ************************ flightTimer ******************************************

    /**
     * 重置Warning
     */
    private void resetWarning() {
        String defaultWarning = warningParser.getDefaultWarning();
        if (defaultWarning == null)
            defaultWarning = "";

        if (!defaultWarning.equals(this.errorId)) {
            this.errorId = defaultWarning;
            EventBus.getDefault().post(AttributeEvent.AUTOPILOT_ERROR);
        }
    }

    /**
     * Reset the vehicle flight timer.
     * 重置计时
     */
    public void resetFlightTimer() {
        elapsedFlightTime = 0;
        startTime = SystemClock.elapsedRealtime();
    }

    // 停止计时
    public void stopTimer() {
        // lets calc the final elapsed timer
        elapsedFlightTime += SystemClock.elapsedRealtime() - startTime;
        startTime = SystemClock.elapsedRealtime();
    }

    /**
     * @return Vehicle flight time in seconds.获取飞行时间
     */
    public long getFlightTime() {
        if (isFlying()) {
            // calc delta time since last checked
            elapsedFlightTime += SystemClock.elapsedRealtime() - startTime;
            startTime = SystemClock.elapsedRealtime();
        }
        return elapsedFlightTime / 1000;
    }

    // 检查EKF位置状态
    private void checkEkfPositionState(msg_ekf_status_report ekfStatus) {
        if (ekfStatus == null)
            return;

        int flags = ekfStatus.flags;

        boolean isOk = this.armed
                ? (flags & EKF_STATUS_FLAGS.EKF_POS_HORIZ_ABS) != 0
                && (flags & EKF_STATUS_FLAGS.EKF_CONST_POS_MODE) == 0
                : (flags & EKF_STATUS_FLAGS.EKF_POS_HORIZ_ABS) != 0
                || (flags & EKF_STATUS_FLAGS.EKF_PRED_POS_HORIZ_ABS) != 0;

        if (isEkfPositionOk != isOk) {
            isEkfPositionOk = isOk;
            EventBus.getDefault().post(AttributeEvent.STATE_EKF_POSITION);

            if (isEkfPositionOk) {
                MavLinkWaypoint.requestWayPoint(myDrone, APMConstants.HOME_WAYPOINT_INDEX);
            }
        }
    }

    private void updateEkfStatus() {
        ekfStatus.setCompassVariance(ekfStatusReport.compass_variance);
        ekfStatus.setHorizontalPositionVariance(ekfStatusReport.pos_horiz_variance);
        ekfStatus.setTerrainAltitudeVariance(ekfStatusReport.terrain_alt_variance);
        ekfStatus.setVelocityVariance(ekfStatusReport.velocity_variance);
        ekfStatus.setVerticalPositionVariance(ekfStatusReport.pos_vert_variance);
        ekfStatus.setEkfStatusFlag(ekfStatusReport.flags);
    }

    public EkfStatus getEkfStatus() {
        return ekfStatus;
    }

    // **************************** [EKF] *******************************************
    // 设置EKF状态
    public void setEkfStatus(msg_ekf_status_report ekfState) {
        if (this.ekfStatusReport == null || !areEkfStatusEquals(this.ekfStatusReport, ekfState)) {
            this.ekfStatusReport = ekfState;
            updateEkfStatus();
            EventBus.getDefault().post(AttributeEvent.STATE_EKF_REPORT);
        }
    }
}