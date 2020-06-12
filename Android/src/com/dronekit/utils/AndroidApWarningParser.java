package com.dronekit.utils;

import android.widget.Toast;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.error.ErrorType;
import com.dronekit.core.model.AutopilotWarningParser;

import java.util.Locale;

import static com.dronekit.core.error.ErrorType.ACCL_3D_CALI_NEED;
import static com.dronekit.core.error.ErrorType.ACCL_CALI_DONE_NOW_REBOOT;
import static com.dronekit.core.error.ErrorType.ACCL_INCONSISTENT;
import static com.dronekit.core.error.ErrorType.ACC_CALI_CANCEL;
import static com.dronekit.core.error.ErrorType.ALTITUDE_DISPARITY;
import static com.dronekit.core.error.ErrorType.ARM_COMPASS_CALIBRATION_RUNNING;
import static com.dronekit.core.error.ErrorType.ARM_GYRO_CALIBRATION_FAILED;
import static com.dronekit.core.error.ErrorType.ARM_LEANING;
import static com.dronekit.core.error.ErrorType.ARM_MODE_NOT_ARMABLE;
import static com.dronekit.core.error.ErrorType.ARM_ROTOR_NOT_SPINNING;
import static com.dronekit.core.error.ErrorType.ARM_SAFETY_SWITCH;
import static com.dronekit.core.error.ErrorType.ARM_THROTTLE_BELOW_FAILSAFE;
import static com.dronekit.core.error.ErrorType.ARM_THROTTLE_TOO_HIGH;
import static com.dronekit.core.error.ErrorType.AUTO_NO_TAKEOFF;
import static com.dronekit.core.error.ErrorType.AUTO_TUNE_FAILED;
import static com.dronekit.core.error.ErrorType.A_POINT_SET;
import static com.dronekit.core.error.ErrorType.BATT_FAILSAFE_ON;
import static com.dronekit.core.error.ErrorType.B_POINT_SET;
import static com.dronekit.core.error.ErrorType.COMPASS_CALI_DONE_NOW_REBOOT;
import static com.dronekit.core.error.ErrorType.COMPASS_INCONSISTENT;
import static com.dronekit.core.error.ErrorType.COMPASS_NOT_HEALTHY;
import static com.dronekit.core.error.ErrorType.CRASH_DISARMING;
import static com.dronekit.core.error.ErrorType.EKF_VARIANCE;
import static com.dronekit.core.error.ErrorType.FC_INIT_NOW;
import static com.dronekit.core.error.ErrorType.GPS_AHRS_DIFF;
import static com.dronekit.core.error.ErrorType.GYROS_NOT_CALIED;
import static com.dronekit.core.error.ErrorType.GYRO_INCONSISTENT;
import static com.dronekit.core.error.ErrorType.LOW_BATTERY;
import static com.dronekit.core.error.ErrorType.MISS_INTERRUPT;
import static com.dronekit.core.error.ErrorType.MISS_RESUME;
import static com.dronekit.core.error.ErrorType.MODE_CHANGE_FAIL;
import static com.dronekit.core.error.ErrorType.NO_DATAFLASH_INSERTED;
import static com.dronekit.core.error.ErrorType.NO_ERROR;
import static com.dronekit.core.error.ErrorType.PARACHUTE_TOO_LOW;
import static com.dronekit.core.error.ErrorType.PRE_ARM_ACCELEROMETERS_NOT_HEALTHY;
import static com.dronekit.core.error.ErrorType.PRE_ARM_ACRO_BAL_ROLL_PITCH;
import static com.dronekit.core.error.ErrorType.PRE_ARM_BAROMETER_NOT_HEALTHY;
import static com.dronekit.core.error.ErrorType.PRE_ARM_CHECK_ANGLE_MAX;
import static com.dronekit.core.error.ErrorType.PRE_ARM_CHECK_BOARD_VOLTAGE;
import static com.dronekit.core.error.ErrorType.PRE_ARM_CHECK_FAILSAFE_THRESHOLD_VALUE;
import static com.dronekit.core.error.ErrorType.PRE_ARM_CHECK_FENCE;
import static com.dronekit.core.error.ErrorType.PRE_ARM_CHECK_MAGNETIC_FIELD;
import static com.dronekit.core.error.ErrorType.PRE_ARM_COMPASS_NOT_CALIBRATED;
import static com.dronekit.core.error.ErrorType.PRE_ARM_COMPASS_NOT_HEALTHY;
import static com.dronekit.core.error.ErrorType.PRE_ARM_COMPASS_OFFSETS_TOO_HIGH;
import static com.dronekit.core.error.ErrorType.PRE_ARM_DUPLICATE_AUX_SWITCH_OPTIONS;
import static com.dronekit.core.error.ErrorType.PRE_ARM_EKF_HOME_VARIANCE;
import static com.dronekit.core.error.ErrorType.PRE_ARM_GPS_GLITCH;
import static com.dronekit.core.error.ErrorType.PRE_ARM_GYROS_NOT_HEALTHY;
import static com.dronekit.core.error.ErrorType.PRE_ARM_HIGH_GPS_HDOP;
import static com.dronekit.core.error.ErrorType.PRE_ARM_INCONSISTENT_ACCELEROMETERS;
import static com.dronekit.core.error.ErrorType.PRE_ARM_INCONSISTENT_COMPASSES;
import static com.dronekit.core.error.ErrorType.PRE_ARM_INCONSISTENT_GYROS;
import static com.dronekit.core.error.ErrorType.PRE_ARM_INS_NOT_CALIBRATED;
import static com.dronekit.core.error.ErrorType.PRE_ARM_NEED_GPS_LOCK;
import static com.dronekit.core.error.ErrorType.PRE_ARM_RC_NOT_CALIBRATED;
import static com.dronekit.core.error.ErrorType.RC7_BAD_POS;
import static com.dronekit.core.error.ErrorType.RC_BAD_VALUE;
import static com.dronekit.core.error.ErrorType.RC_FAILSAFE;
import static com.dronekit.core.error.ErrorType.RC_FAILSAFE_ON;
import static com.dronekit.core.error.ErrorType.RC_LOST;
import static com.dronekit.core.error.ErrorType.RC_NOT_CONFIG;
import static com.dronekit.core.error.ErrorType.WAITING_FOR_NAVIGATION_ALIGNMENT;

/**
 * Autopilot error parser.
 * 飞行器出错提示转换器
 */
public class AndroidApWarningParser implements AutopilotWarningParser {

    @Override
    public String getDefaultWarning() {
        return NO_ERROR.name();
    }

    /**
     * Maps the ArduPilot warnings set to the 3DR Services warnings set.
     *
     * @param warning warning originating from the ArduPilot autopilot
     * @return equivalent 3DR Services warning type
     */
    @Override
    public String parseWarning(Drone drone, String warning) {
        if (android.text.TextUtils.isEmpty(warning))
            return null;

        ErrorType errorType = getErrorType(warning);
        if (errorType == null)
            return null;

        return errorType.name();
    }

    private ErrorType getErrorType(String warning) {
        switch (warning.toLowerCase(Locale.US)) {
            case "prearm: thr below fs":
            case "prearm: throttle below failsafe":
                return ARM_THROTTLE_BELOW_FAILSAFE;

            case "prearm: gyro calibration failed":
                return ARM_GYRO_CALIBRATION_FAILED;

            case "prearm: mode not armable":
                return ARM_MODE_NOT_ARMABLE;

            case "prearm: rotor not spinning":
                return ARM_ROTOR_NOT_SPINNING;

            case "arm: altitude disparity":
            case "prearm: altitude disparity":
                return ALTITUDE_DISPARITY;

            case "arm: leaning":
                return ARM_LEANING;

            case "arm: throttle too high":
                return ARM_THROTTLE_TOO_HIGH;

            case "arm: safety switch":
                return ARM_SAFETY_SWITCH;

            case "arm: compass calibration running":
                return ARM_COMPASS_CALIBRATION_RUNNING;

            case "prearm: rc not calibrated":
                return PRE_ARM_RC_NOT_CALIBRATED;

            case "prearm: barometer not healthy":
                return PRE_ARM_BAROMETER_NOT_HEALTHY;

            case "prearm: compass not healthy":
                return PRE_ARM_COMPASS_NOT_HEALTHY;

            case "prearm: compass not calibrated":
                return PRE_ARM_COMPASS_NOT_CALIBRATED;

            case "prearm: compass offsets too high":
                return PRE_ARM_COMPASS_OFFSETS_TOO_HIGH;

            case "prearm: check mag field":
                return PRE_ARM_CHECK_MAGNETIC_FIELD;

            case "prearm: inconsistent compasses":
                return PRE_ARM_INCONSISTENT_COMPASSES;

            case "prearm: check fence":
                return PRE_ARM_CHECK_FENCE;

            case "prearm: ins not calibrated":
                return PRE_ARM_INS_NOT_CALIBRATED;

            case "prearm: accelerometers not healthy":
                return PRE_ARM_ACCELEROMETERS_NOT_HEALTHY;

            case "prearm: inconsistent accelerometers":
                return PRE_ARM_INCONSISTENT_ACCELEROMETERS;

            case "prearm: gyros not healthy":
                return PRE_ARM_GYROS_NOT_HEALTHY;

            case "prearm: inconsistent gyros":
                return PRE_ARM_INCONSISTENT_GYROS;

            case "prearm: check board voltage":
                return PRE_ARM_CHECK_BOARD_VOLTAGE;

            case "prearm: duplicate aux switch options":
                return PRE_ARM_DUPLICATE_AUX_SWITCH_OPTIONS;

            case "prearm: check fs_thr_value":
                return PRE_ARM_CHECK_FAILSAFE_THRESHOLD_VALUE;

            case "prearm: check angle_max":
                return PRE_ARM_CHECK_ANGLE_MAX;

            case "prearm: acro_bal_roll/pitch":
                return PRE_ARM_ACRO_BAL_ROLL_PITCH;

            case "prearm: need 3d fix":
                return PRE_ARM_NEED_GPS_LOCK;

            case "prearm: ekf-home variance":
                return PRE_ARM_EKF_HOME_VARIANCE;

            case "prearm: high gps hdop":
                return PRE_ARM_HIGH_GPS_HDOP;

            case "prearm: gps glitch":
            case "prearm: bad velocity":
                return PRE_ARM_GPS_GLITCH;

            case "prearm: waiting for navigation alignment":
            case "arm: waiting for navigation alignment":
                return WAITING_FOR_NAVIGATION_ALIGNMENT;

            case "no dataflash inserted":
                return NO_DATAFLASH_INSERTED;

            case "low battery!":
                return LOW_BATTERY;

            case "autotune: failed":
                return AUTO_TUNE_FAILED;

            case "crash: disarming":
                return CRASH_DISARMING;

            case "parachute: too low":
                return PARACHUTE_TOO_LOW;

            case "ekf variance":
                return EKF_VARIANCE;

            case "rc failsafe":
                return RC_FAILSAFE;

            case " mode failed":
                return MODE_CHANGE_FAIL;

            case "no takeoff wp":
                return AUTO_NO_TAKEOFF;

            case "prearm: waiting for nav checks":
                return FC_INIT_NOW;

            case "mission interrupt":
                return MISS_INTERRUPT;

            case "resume index:":
                return MISS_RESUME;

            case "rc7 is in a point":
                return RC7_BAD_POS;
            case "a points set":
                return A_POINT_SET;
            case "b points set":
                return B_POINT_SET;

            case "caution: radio lost":
            case "no receiver":
                return RC_LOST;

            default:
                if(warning.toLowerCase(Locale.US).contains("caution: radio lost") || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return RC_LOST;
                }
                else if(warning.toLowerCase(Locale.US).contains("throttle below failsafe")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return ARM_THROTTLE_BELOW_FAILSAFE;
                }
                else if(warning.toLowerCase(Locale.US).contains("mode failed")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return MODE_CHANGE_FAIL;
                }
                else if(warning.toLowerCase(Locale.US).contains("no takeoff wp")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return AUTO_NO_TAKEOFF;
                }
                else if(warning.toLowerCase(Locale.US).contains("mission interrupt")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return MISS_INTERRUPT;
                }
                else if(warning.toLowerCase(Locale.US).contains("resume index:")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return MISS_RESUME;
                }
                else if(warning.toLowerCase(Locale.US).contains("accel cali canceled")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return ACC_CALI_CANCEL;
                }
                else if(warning.toLowerCase(Locale.US).contains("low battery")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return LOW_BATTERY;
                }
                else if(warning.contains("PreArm: Gyros not calibrated")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return GYROS_NOT_CALIED;
                }
                else if(warning.contains("PreArm: 3D Accel calibration needed")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return ACCL_3D_CALI_NEED;
                }
                else if(warning.contains("PreArm: Accels calibrated requires reboot")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return ACCL_CALI_DONE_NOW_REBOOT;
                }
                else if(warning.contains("PreArm: Accels inconsistent")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return ACCL_INCONSISTENT;
                }
                else if(warning.contains("PreArm: Gyros inconsistent")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return GYRO_INCONSISTENT;
                }
                else if(warning.contains("PreArm: Compass not healthy")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return COMPASS_NOT_HEALTHY;
                }
                else if(warning.contains("PreArm: Compass calibrated requires reboot")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return COMPASS_CALI_DONE_NOW_REBOOT;
                }
                else if(warning.contains("PreArm: Compasses inconsistent")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return COMPASS_INCONSISTENT;
                }
                else if(warning.contains("PreArm: GPS and AHRS differ by ")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return GPS_AHRS_DIFF;
                }
                else if(warning.contains("PreArm: Battery failsafe on")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return BATT_FAILSAFE_ON;
                }
                else if(warning.contains("PreArm: Radio failsafe on")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return RC_FAILSAFE_ON;
                }
                else if(warning.contains("PreArm: RC") && warning.contains("not configured")) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return RC_NOT_CONFIG;
                }
                else if(warning.contains("PreArm: ") && (warning.contains("radio min too high")
                        || warning.contains("radio max too low")
                        || warning.contains("radio trim below min")
                        || warning.contains("radio trim above max"))
                        ) //  || warning.toLowerCase(Locale.US).contains("no receiver") )
                {
                    return RC_BAD_VALUE;
                }
                else
                {
                    return null;
                }
        }
    }
}
