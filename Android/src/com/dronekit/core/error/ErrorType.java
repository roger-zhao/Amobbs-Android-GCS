package com.dronekit.core.error;

import android.content.Context;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import org.farring.gcs.R;

/**
 * List all the possible error types.
 */
public enum ErrorType {

    NO_ERROR(R.string.error_no_error),

    ARM_THROTTLE_BELOW_FAILSAFE(R.string.error_throttle_below_failsafe),
    ARM_GYRO_CALIBRATION_FAILED(R.string.error_gyro_calibration_failed),
    ARM_MODE_NOT_ARMABLE(R.string.error_mode_not_armable),
    ARM_ROTOR_NOT_SPINNING(R.string.error_rotor_not_spinning),

    ARM_LEANING(R.string.error_vehicle_leaning),
    ARM_THROTTLE_TOO_HIGH(R.string.error_throttle_too_high),
    ARM_SAFETY_SWITCH(R.string.error_safety_switch),
    ARM_COMPASS_CALIBRATION_RUNNING(R.string.error_compass_calibration_running),

    PRE_ARM_RC_NOT_CALIBRATED(R.string.error_rc_not_calibrated),
    PRE_ARM_BAROMETER_NOT_HEALTHY(R.string.error_barometer_not_healthy),
    PRE_ARM_COMPASS_NOT_HEALTHY(R.string.error_compass_not_healthy),
    PRE_ARM_COMPASS_NOT_CALIBRATED(R.string.error_compass_not_calibrated),
    PRE_ARM_COMPASS_OFFSETS_TOO_HIGH(R.string.error_compass_offsets_too_high),
    PRE_ARM_CHECK_MAGNETIC_FIELD(R.string.error_check_magnetic_field),
    PRE_ARM_INCONSISTENT_COMPASSES(R.string.error_inconsistent_compass),
    PRE_ARM_CHECK_FENCE(R.string.error_check_geo_fence),
    PRE_ARM_INS_NOT_CALIBRATED(R.string.error_ins_not_calibrated),
    PRE_ARM_ACCELEROMETERS_NOT_HEALTHY(R.string.error_accelerometers_not_healthy),
    PRE_ARM_INCONSISTENT_ACCELEROMETERS(R.string.error_inconsistent_accelerometers),
    PRE_ARM_GYROS_NOT_HEALTHY(R.string.error_gyros_not_healthy),
    PRE_ARM_INCONSISTENT_GYROS(R.string.error_inconsistent_gyros),
    PRE_ARM_CHECK_BOARD_VOLTAGE(R.string.error_check_board_voltage),
    PRE_ARM_DUPLICATE_AUX_SWITCH_OPTIONS(R.string.error_duplicate_aux_switch_options),
    PRE_ARM_CHECK_FAILSAFE_THRESHOLD_VALUE(R.string.error_check_failsafe_threshold),
    PRE_ARM_CHECK_ANGLE_MAX(R.string.error_check_angle_max),
    PRE_ARM_ACRO_BAL_ROLL_PITCH(R.string.error_acro_bal_roll_pitch),
    PRE_ARM_NEED_GPS_LOCK(R.string.error_need_gps_lock),
    PRE_ARM_EKF_HOME_VARIANCE(R.string.error_ekf_home_variance),
    PRE_ARM_HIGH_GPS_HDOP(R.string.error_high_gps_hdop),
    PRE_ARM_GPS_GLITCH(R.string.error_gps_glitch),
    WAITING_FOR_NAVIGATION_ALIGNMENT(R.string.error_waiting_for_navigation_alignment),

    ALTITUDE_DISPARITY(R.string.error_altitude_disparity),
    LOW_BATTERY(R.string.error_low_battery),
    AUTO_TUNE_FAILED(R.string.error_auto_tune_failed),
    CRASH_DISARMING(R.string.error_crash),
    PARACHUTE_TOO_LOW(R.string.error_parachute_too_low),
    EKF_VARIANCE(R.string.error_ekf_variance),
    NO_DATAFLASH_INSERTED(R.string.error_no_dataflash),
    MODE_CHANGE_FAIL(R.string.mode_change_fail),
    AUTO_NO_TAKEOFF(R.string.auto_no_takeoff),
    FC_INIT_NOW(R.string.wait_for_nav_chk),
    MISS_INTERRUPT(R.string.miss_interrupt),
    RC7_BAD_POS(R.string.loiter_start_in_A_point),
    MISS_RESUME(R.string.resume_mission),
    A_POINT_SET(R.string.A_point_set),
    B_POINT_SET(R.string.B_point_set),
    RC_LOST(R.string.rc_lost),
    ACC_CALI_CANCEL(R.string.ACC_cali_canceled),
    GYROS_NOT_CALIED(R.string.NEW_GYROS_NOT_CALIED),
    ACCL_3D_CALI_NEED(R.string.NEW_ACCL_3D_CALI_NEED),
    ACCL_CALI_DONE_NOW_REBOOT(R.string.NEW_ACCL_CALI_DONE_NOW_REBOOT),
    ACCL_INCONSISTENT(R.string.NEW_ACCL_INCONSISTENT),
    GYRO_INCONSISTENT(R.string.NEW_GYRO_INCONSISTENT),
    COMPASS_NOT_HEALTHY(R.string.NEW_COMPASS_NOT_HEALTHY),
    COMPASS_CALI_DONE_NOW_REBOOT(R.string.NEW_COMPASS_CALI_DONE_NOW_REBOOT),
    COMPASS_INCONSISTENT(R.string.NEW_COMPASS_INCONSISTENT),
    GPS_AHRS_DIFF(R.string.NEW_GPS_AHRS_DIFF),
    BATT_FAILSAFE_ON(R.string.NEW_BATT_FAILSAFE_ON),
    RC_FAILSAFE_ON(R.string.NEW_RC_FAILSAFE_ON),
    RC_NOT_CONFIG(R.string.NEW_RC_NOT_CONFIG),
    RC_BAD_VALUE(R.string.NEW_RC_BAD_VALUE),
    RC_FAILSAFE(R.string.error_rc_failsafe);

    @StringRes
    private final int labelResId;

    ErrorType(@StringRes int labelResId) {
        this.labelResId = labelResId;
    }

    public static ErrorType getErrorById(String errorId) {
        if (TextUtils.isEmpty(errorId))
            return null;

        return ErrorType.valueOf(errorId);
    }

    public CharSequence getLabel(Context context) {
        if (context == null)
            return null;

        return context.getText(this.labelResId);
    }
}
