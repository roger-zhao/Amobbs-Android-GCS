package com.evenbus;

/**
 * Stores all possible drone events.
 * 该枚举类型储存飞行器所有的事件，用于事件总线的调用
 */
public enum AttributeEvent {

    /**
     * Attitude attribute events.
     */
    ATTITUDE_UPDATED,

    /**
     * Signals an autopilot error.
     */
    AUTOPILOT_ERROR,

    /**
     * Event describing a message received from the autopilot.
     */
    AUTOPILOT_MESSAGE,

    /**
     * Event to signal cancellation of the magnetometer calibration process.
     */
    CALIBRATION_MAG_CANCELLED,

    /**
     * Signals completion of the magnetometer calibration.
     */
    CALIBRATION_MAG_COMPLETED,

    /**
     * Provides progress updates for the magnetometer calibration.
     */
    CALIBRATION_MAG_PROGRESS,

    CALIBRATION_IMU,
    CALIBRATION_IMU_POS,
    CALIBRATION_IMU_TIMEOUT,

    FOLLOW_START,
    FOLLOW_STOP,
    FOLLOW_UPDATE,

    /**
     * Camera attribute events.
     */
    CAMERA_UPDATED,
    CAMERA_FOOTPRINTS_UPDATED,

    /**
     * GuidedState attribute events.
     */
    GUIDED_POINT_UPDATED,

    /**
     * Mission attribute events.
     */
    MISSION_UPDATED,
    MISSION_DRONIE_CREATED,
    MISSION_SENT,
    MISSION_RECEIVED,
    MISSION_ITEM_UPDATED,
    MISSION_ITEM_REACHED,

    /*
     * Parameter attribute events.
     */

    /**
     * Event to signal the start of parameters refresh from the vehicle.
     */
    PARAMETERS_REFRESH_STARTED,

    /**
     * Event to signal the completion of the parameters refresh from the vehicle.
     */
    PARAMETERS_REFRESH_COMPLETED,

    /**
     * Event to signal receipt of a single parameter from the vehicle. During a parameters refresh,
     * this event will fire as many times as the count of the set of parameters being refreshed.
     * Allows listeners to keep track of the parameters refresh progress.
     */
    PARAMETER_RECEIVED,

    /**
     * Event to signal update of the vehicle type.
     */
    TYPE_UPDATED,

    /**
     * Signal attribute events.
     */
    SIGNAL_UPDATED,
    SIGNAL_WEAK,

    /**
     * Speed attribute events.
     */
    SPEED_UPDATED,

    /**
     * Battery attribute events.
     */
    BATTERY_UPDATED,

    /*
     * State attribute events.
     */
    /**
     * Signals changes in the vehicle readiness (i.e: standby or active/airborne).
     */
    STATE_UPDATED,

    /**
     * Signals changes in the vehicle arming state.
     */
    STATE_ARMING,
    STATE_CONNECTING,
    /**
     * Successful connection event.
     */
    STATE_CONNECTED,
    STATE_DISCONNECTED,
    /**
     * Connection failed event.
     */
    STATE_CONNECTION_FAILED,

    /**
     * Vehicle link is being validated.
     */
    CHECKING_VEHICLE_LINK,

    /**
     * Signals updates of the ekf status.
     */
    STATE_EKF_REPORT,

    /**
     * Signals updates to the ekf position state.
     */
    STATE_EKF_POSITION,

    /**
     * Signals update of the vehicle mode.
     */
    STATE_VEHICLE_MODE,

    /**
     * Signals vehicle vibration updates.
     */
    STATE_VEHICLE_VIBRATION,

    /**
     * Signals vehicle UID updates.
     */
    STATE_VEHICLE_UID,

    /**
     * Home attribute events.
     */
    HOME_UPDATED,

    /**
     * Gps' attribute events.
     */
    GPS_POSITION,
    GPS_FIX,
    GPS_COUNT,
    WARNING_NO_GPS,

    HEARTBEAT_FIRST,
    HEARTBEAT_RESTORED,
    HEARTBEAT_TIMEOUT,

    /**
     * Altitude's attribute events.
     */
    ALTITUDE_UPDATED,

    /**
     * Signals the gimbal orientation was updated.
     */
    GIMBAL_ORIENTATION_UPDATED,

    /**
     * Signals an update to the return to me state.
     */
    RETURN_TO_ME_STATE_UPDATE,

    RC_IN,
    RC_OUT,

    UPDATE_MAGNETOMETER_NO1,
    UPDATE_MAGNETOMETER_NO2,
    ESC_CALIBRATION,
    RC_CALIBRATION_DONE,
    MAG_CALIBRATION_DONE,
    LOW_BATTERY,
    RC_LOST,
    LIDAR_ENABLED,
    LIDAR_DISABLED,
}
