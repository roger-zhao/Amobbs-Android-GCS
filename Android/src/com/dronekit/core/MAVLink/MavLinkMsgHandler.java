package com.dronekit.core.MAVLink;

import android.text.TextUtils;
import android.util.Log;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.ardupilotmega.msg_camera_feedback;
import com.MAVLink.ardupilotmega.msg_ekf_status_report;
import com.MAVLink.ardupilotmega.msg_mag_cal_progress;
import com.MAVLink.ardupilotmega.msg_mag_cal_report;
import com.MAVLink.ardupilotmega.msg_mount_status;
import com.MAVLink.ardupilotmega.msg_radio;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_gps_raw_int;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_mission_current;
import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_mission_item_reached;
import com.MAVLink.common.msg_named_value_int;
import com.MAVLink.common.msg_nav_controller_output;
import com.MAVLink.common.msg_radio_status;
import com.MAVLink.common.msg_raw_imu;
import com.MAVLink.common.msg_rc_channels_raw;
import com.MAVLink.common.msg_scaled_imu2;
import com.MAVLink.common.msg_servo_output_raw;
import com.MAVLink.common.msg_statustext;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.MAVLink.common.msg_vibration;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_STATE;
import com.MAVLink.enums.MAV_SYS_STATUS_SENSOR;
import com.dronekit.core.drone.autopilot.APMConstants;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Altitude;
import com.dronekit.core.drone.property.Attitude;
import com.dronekit.core.drone.property.Battery;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.property.Signal;
import com.dronekit.core.drone.property.Speed;
import com.dronekit.core.drone.property.Vibration;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.dronekit.utils.MathUtils;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;
import com.evenbus.LogMessageEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse the received mavlink messages, and update the drone state appropriately.
 */
public class MavLinkMsgHandler {
    /**
     * 组件定义
     */
    public static final int AUTOPILOT_COMPONENT_ID = 1;
    public static final int ARTOO_COMPONENT_ID = 0;
    public static final int TELEMETRY_RADIO_COMPONENT_ID = 68;
    public static final int COLLISION_SECONDS_BEFORE_COLLISION = 2;
    public static final double COLLISION_DANGEROUS_SPEED_METERS_PER_SECOND = -3.0;
    public static final double COLLISION_SAFE_ALTITUDE_METERS = 1.0;
    private static final String PIXHAWK_SERIAL_NUMBER_REGEX = ".*PX4v2 (([0-9A-F]{8}) ([0-9A-F]{8}) ([0-9A-F]{8}))";
    private static final Pattern PIXHAWK_SERIAL_NUMBER_PATTERN = Pattern.compile(PIXHAWK_SERIAL_NUMBER_REGEX);
    private Drone drone;

    public MavLinkMsgHandler(Drone drone) {
        this.drone = drone;
    }

    public void receiveData(MAVLinkMessage message) {

        int compId = message.compid;

        // 判断飞行器类型是否为Ardupilot
        if (compId != AUTOPILOT_COMPONENT_ID && compId != ARTOO_COMPONENT_ID && compId != TELEMETRY_RADIO_COMPONENT_ID) {
            return;
        }

        // 参数
        if (drone.getParameterManager().processMessage(message)) {
            return;
        }

        // 航点
        drone.getWaypointManager().processMessage(message);
        // 校准
        drone.getCalibrationSetup().processMessage(message);

        // 根据不同的Id选择不同处理办法
        switch (message.msgid) {
            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                processAttitude((msg_attitude) message);
                break;

            case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                processVfrHud((msg_vfr_hud) message);
                break;

            case msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT:
                drone.getMissionStats().setWpno(((msg_mission_current) message).seq);
                break;

            case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:
                msg_nav_controller_output m_nav = (msg_nav_controller_output) message;
                setDisttowpAndSpeedAltErrors(m_nav.wp_dist, m_nav.alt_error, m_nav.aspd_error);
                break;

            case msg_raw_imu.MAVLINK_MSG_ID_RAW_IMU:
                msg_raw_imu msg_imu = (msg_raw_imu) message;
                drone.getMagnetometer().newMag1Data(msg_imu);
                break;

            case msg_scaled_imu2.MAVLINK_MSG_ID_SCALED_IMU2:
                msg_scaled_imu2 msg_imu2 = (msg_scaled_imu2) message;
                drone.getMagnetometer().newMag2Data(msg_imu2);
                break;

            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                msg_heartbeat msg_heart = (msg_heartbeat) message;
                drone.getHeartBeat().onHeartbeat(msg_heart);
                drone.getType().setType(msg_heart.type);
                checkIfFlying(msg_heart);
                processState(msg_heart);
                ApmModes newMode = ApmModes.getMode(msg_heart.custom_mode, drone.getType().getType());
                drone.getState().setMode(newMode, (int)msg_heart.custom_mode);
                break;

            case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                processGlobalPositionInt((msg_global_position_int) message);
                break;

            case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
                msg_sys_status m_sys = (msg_sys_status) message;
                processSysStatus(m_sys);
                checkControlSensorsHealth(m_sys);
                break;

            case msg_radio.MAVLINK_MSG_ID_RADIO:
                msg_radio m_radio = (msg_radio) message;
                processSignalUpdate(m_radio.rxerrors, m_radio.fixed, m_radio.rssi, m_radio.remrssi, m_radio.txbuf, m_radio.noise, m_radio.remnoise);
                break;

            case msg_radio_status.MAVLINK_MSG_ID_RADIO_STATUS:
                msg_radio_status m_radio_status = (msg_radio_status) message;
                processSignalUpdate(m_radio_status.rxerrors, m_radio_status.fixed, m_radio_status.rssi, m_radio_status.remrssi, m_radio_status.txbuf, m_radio_status.noise, m_radio_status.remnoise);
                break;

            case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT:
                processGpsState((msg_gps_raw_int) message);
                break;

            case msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW:
                drone.getVehicleRC().setRcInputValues((msg_rc_channels_raw) message);
                break;

            case msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
                drone.getVehicleRC().setRcOutputValues((msg_servo_output_raw) message);
                break;

            case msg_statustext.MAVLINK_MSG_ID_STATUSTEXT:
                // These are any warnings sent from APM:Copter with gcs_send_text_P()
                // This includes important thing like arm fails, prearm fails, low battery, etc.
                // also less important things like "erasing logs" and "calibrating barometer"
                msg_statustext msg_statustext = (com.MAVLink.common.msg_statustext) message;
                processStatusText(msg_statustext);
                break;

            case msg_camera_feedback.MAVLINK_MSG_ID_CAMERA_FEEDBACK:
                drone.getCamera().newImageLocation((msg_camera_feedback) message);
                break;

            case msg_mount_status.MAVLINK_MSG_ID_MOUNT_STATUS:
                drone.getCamera().updateMountOrientation(((msg_mount_status) message));
                break;

            case msg_named_value_int.MAVLINK_MSG_ID_NAMED_VALUE_INT:
                processNamedValueInt((msg_named_value_int) message);
                break;

            //*************** Magnetometer calibration messages handling *************//
            case msg_mag_cal_progress.MAVLINK_MSG_ID_MAG_CAL_PROGRESS:
            case msg_mag_cal_report.MAVLINK_MSG_ID_MAG_CAL_REPORT:
                drone.getMagnetometerCalibration().processCalibrationMessage(message);
                break;

            //*************** EKF State handling ******************//
            case msg_ekf_status_report.MAVLINK_MSG_ID_EKF_STATUS_REPORT:
                drone.getState().setEkfStatus((msg_ekf_status_report) message);
                break;

            case msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM:
                processHomeUpdate((msg_mission_item) message);
                break;

            case msg_vibration.MAVLINK_MSG_ID_VIBRATION:
                msg_vibration vibrationMsg = (msg_vibration) message;
                processVibrationMessage(vibrationMsg);
                break;

            case msg_mission_item_reached.MAVLINK_MSG_ID_MISSION_ITEM_REACHED:
                drone.getMissionStats().setLastReachedWaypointNumber(((msg_mission_item_reached) message).seq);
                break;

            default:
                break;
        }
    }

    private void processVibrationMessage(msg_vibration vibrationMsg) {
        boolean wasUpdated = false;

        Vibration vibration = drone.getVibration();

        if (vibration.getVibrationX() != vibrationMsg.vibration_x) {
            vibration.setVibrationX(vibrationMsg.vibration_x);
            wasUpdated = true;
        }

        if (vibration.getVibrationY() != vibrationMsg.vibration_y) {
            vibration.setVibrationY(vibrationMsg.vibration_y);
            wasUpdated = true;
        }

        if (vibration.getVibrationZ() != vibrationMsg.vibration_z) {
            vibration.setVibrationZ(vibrationMsg.vibration_z);
            wasUpdated = true;
        }

        if (vibration.getFirstAccelClipping() != vibrationMsg.clipping_0) {
            vibration.setFirstAccelClipping(vibrationMsg.clipping_0);
            wasUpdated = true;
        }

        if (vibration.getSecondAccelClipping() != vibrationMsg.clipping_1) {
            vibration.setSecondAccelClipping(vibrationMsg.clipping_1);
            wasUpdated = true;
        }

        if (vibration.getThirdAccelClipping() != vibrationMsg.clipping_2) {
            vibration.setThirdAccelClipping(vibrationMsg.clipping_2);
            wasUpdated = true;
        }

        if (wasUpdated) {
            EventBus.getDefault().post(AttributeEvent.STATE_VEHICLE_VIBRATION);
        }
    }

    private void processNamedValueInt(msg_named_value_int message) {
        if (message == null)
            return;

        switch (message.getName()) {
            case "ARMMASK":
                //Give information about the vehicle's ability to arm successfully.
                ApmModes vehicleMode = drone.getState().getMode();
                if (ApmModes.isCopter(vehicleMode.getType())) {
                    int value = message.value;
                    boolean isReadyToArm = (value & (1 << vehicleMode.getNumber())) != 0;
                    String armReadinessMsg = isReadyToArm ? "READY TO ARM" : "UNREADY FOR ARMING";
                    EventBus.getDefault().post(new LogMessageEvent(Log.INFO, armReadinessMsg));
                }
                break;
        }
    }

    private void processState(msg_heartbeat msg_heart) {
        checkArmState(msg_heart);
        checkFailsafe(msg_heart);
    }

    private void checkFailsafe(msg_heartbeat msg_heart) {
        boolean failsafe2 = msg_heart.system_status == MAV_STATE.MAV_STATE_CRITICAL || msg_heart.system_status == MAV_STATE.MAV_STATE_EMERGENCY;

        if (failsafe2) {
            drone.getState().repeatWarning();
        }
    }

    private void checkControlSensorsHealth(msg_sys_status sysStatus) {
        boolean isRCFailsafe = (sysStatus.onboard_control_sensors_health & MAV_SYS_STATUS_SENSOR.MAV_SYS_STATUS_SENSOR_RC_RECEIVER) == 0;
        if (isRCFailsafe) {
            drone.getState().parseAutopilotError("RC FAILSAFE");
        }
    }

    private void checkIfFlying(msg_heartbeat msg_heart) {
        short systemStatus = msg_heart.system_status;
        boolean wasFlying = drone.getState().isFlying();

        boolean isFlying = systemStatus == MAV_STATE.MAV_STATE_ACTIVE
                || (wasFlying
                && (systemStatus == MAV_STATE.MAV_STATE_CRITICAL || systemStatus == MAV_STATE.MAV_STATE_EMERGENCY));

        drone.getState().setIsFlying(isFlying);
    }

    // 检查是否上锁
    private void checkArmState(msg_heartbeat msg_heart) {
        drone.getState().setArmed((msg_heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) == MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED);
    }

    /**
     * 处理消息
     *
     * @param statusText
     */

    protected void processStatusText(msg_statustext statusText) {
        String message = statusText.getText();
        if (TextUtils.isEmpty(message))
            return;

        if (message.startsWith("ArduCopter") || message.startsWith("ArduPlane")
                || message.startsWith("ArduRover") || message.startsWith("Solo")
                || message.startsWith("APM:Copter") || message.startsWith("APM:Plane")
                || message.startsWith("APM:Rover")) {
            drone.getType().setFirmwareVersion(message);
        } else {
            // Try parsing as an error.
            if (!drone.getState().parseAutopilotError(message)) {
                //Relay to the connected client.
                int logLevel;
                switch (statusText.severity) {
                    case APMConstants.Severity.SEVERITY_CRITICAL:
                        logLevel = Log.ERROR;
                        break;

                    case APMConstants.Severity.SEVERITY_HIGH:
                        logLevel = Log.WARN;
                        break;

                    case APMConstants.Severity.SEVERITY_MEDIUM:
                        logLevel = Log.INFO;
                        break;

                    default:
                    case APMConstants.Severity.SEVERITY_LOW:
                        logLevel = Log.VERBOSE;
                        break;

                    case APMConstants.Severity.SEVERITY_USER_RESPONSE:
                        logLevel = Log.DEBUG;
                        break;
                }
                EventBus.getDefault().post(new LogMessageEvent(logLevel, message));
            }
        }

        // [获取Pixhawk的UID]Parse pixhawk serial number.
        final Matcher matcher = PIXHAWK_SERIAL_NUMBER_PATTERN.matcher(message);
        if (matcher.matches()) {
            final String serialNumber = matcher.group(2) + matcher.group(3) + matcher.group(4);
            if (!serialNumber.equalsIgnoreCase(drone.getState().getPixhawkSerialNumber())) {
                drone.getState().setPixhawkSerialNumber(serialNumber);
                EventBus.getDefault().post(AttributeEvent.STATE_VEHICLE_UID);
            }
        }
    }

    /**
     * 处理飞行器姿态
     *
     * @param m_att
     */
    private void processAttitude(msg_attitude m_att) {
        Attitude attitude = drone.getAttitude();
        attitude.setRoll(Math.toDegrees(m_att.roll));
        attitude.setRollSpeed((float) Math.toDegrees(m_att.rollspeed));

        attitude.setPitch(Math.toDegrees(m_att.pitch));
        attitude.setPitchSpeed((float) Math.toDegrees(m_att.pitchspeed));

        attitude.setYaw(Math.toDegrees(m_att.yaw));
        attitude.setYawSpeed((float) Math.toDegrees(m_att.yawspeed));

        EventBus.getDefault().post(AttributeEvent.ATTITUDE_UPDATED);
    }

    protected void processVfrHud(msg_vfr_hud vfrHud) {
        if (vfrHud == null)
            return;

        setAltitudeGroundAndAirSpeeds(vfrHud.alt, vfrHud.groundspeed, vfrHud.airspeed, vfrHud.climb);
    }

    /**
     * 设置高度，速度
     *
     * @param altitude    高度
     * @param groundSpeed 地速
     * @param airSpeed    空速
     * @param climb       爬升率
     */
    protected void setAltitudeGroundAndAirSpeeds(double altitude, double groundSpeed, double airSpeed, double climb) {
        Altitude droneAltitude = drone.getAltitude();
        if (droneAltitude.getAltitude() != altitude) {
            droneAltitude.setAltitude(altitude);
            EventBus.getDefault().post(AttributeEvent.ALTITUDE_UPDATED);
        }

        Speed droneSpeed = drone.getSpeed();
        if (droneSpeed.getGroundSpeed() != groundSpeed || droneSpeed.getAirSpeed() != airSpeed || droneSpeed.getVerticalSpeed() != climb) {
            droneSpeed.setGroundSpeed(groundSpeed);
            droneSpeed.setAirSpeed(airSpeed);
            droneSpeed.setVerticalSpeed(climb);

            // 检查是否面临坠机！
            checkForGroundCollision();

            EventBus.getDefault().post(AttributeEvent.SPEED_UPDATED);
        }
    }

    /**
     * if drone will crash in 2 seconds at constant climb rate and climb rate < -3 m/s and altitude > 1 meter
     */
    private void checkForGroundCollision() {
        double verticalSpeed = drone.getSpeed().getVerticalSpeed();
        double altitudeValue = drone.getAltitude().getAltitude();

        if (altitudeValue + (verticalSpeed * COLLISION_SECONDS_BEFORE_COLLISION) < 0
                && verticalSpeed < COLLISION_DANGEROUS_SPEED_METERS_PER_SECOND
                && altitudeValue > COLLISION_SAFE_ALTITUDE_METERS)
            // 发送事件，通知全局
            EventBus.getDefault().post(ActionEvent.ACTION_GROUND_COLLISION_IMMINENT);
    }

    /**
     * 设置距离下一航点的距离和高度差
     *
     * @param disttowp
     * @param alt_error
     * @param aspd_error
     */
    private void setDisttowpAndSpeedAltErrors(double disttowp, double alt_error, double aspd_error) {
        drone.getMissionStats().setDistanceToWp(disttowp);

        drone.getAltitude().setTargetAltitude(drone.getAltitude().getAltitude() + alt_error);
        EventBus.getDefault().post(AttributeEvent.ATTITUDE_UPDATED);
    }

    /**
     * Used to update the vehicle location.更新飞行器的位置信息
     *
     * @param gpi
     */
    protected void processGlobalPositionInt(msg_global_position_int gpi) {
        if (gpi == null)
            return;

        double newLat = gpi.lat / 1E7;
        double newLong = gpi.lon / 1E7;

        boolean positionUpdated = false;
        LatLong gpsPosition = drone.getVehicleGps().getPosition();
        if (gpsPosition == null) {
            gpsPosition = new LatLong(newLat, newLong);
            drone.getVehicleGps().setPosition(gpsPosition);
            positionUpdated = true;
        } else if (gpsPosition.getLatitude() != newLat || gpsPosition.getLongitude() != newLong) {
            gpsPosition.setLatitude(newLat);
            gpsPosition.setLongitude(newLong);
            positionUpdated = true;
        }

        if (positionUpdated) {
            EventBus.getDefault().post(AttributeEvent.GPS_POSITION);
        }
    }

    /**
     * 处理系统消息
     *
     * @param m_sys
     */
    protected void processSysStatus(msg_sys_status m_sys) {
        processBatteryUpdate(m_sys.voltage_battery / 1000.0, m_sys.battery_remaining, m_sys.current_battery / 100.0);
    }

    /**
     * 电池电量消息
     *
     * @param voltage 电压
     * @param remain  剩余百分比
     * @param current 电流
     */
    protected void processBatteryUpdate(double voltage, double remain, double current) {
        Battery battery = drone.getBattery();
        if (battery.getBatteryVoltage() != voltage || battery.getBatteryRemain() != remain || battery.getBatteryCurrent() != current) {
            battery.setBatteryVoltage(voltage);
            battery.setBatteryRemain(remain);
            battery.setBatteryCurrent(current);
            EventBus.getDefault().post(AttributeEvent.BATTERY_UPDATED);
        }
    }

    /**
     * 数传信号消息
     *
     * @param rxerrors
     * @param fixed
     * @param rssi
     * @param remrssi
     * @param txbuf
     * @param noise
     * @param remnoise
     */
    protected void processSignalUpdate(int rxerrors, int fixed, short rssi, short remrssi, short txbuf, short noise, short remnoise) {
        Signal signal = drone.getSignal();
        signal.setValid(true);
        signal.setRxerrors(rxerrors & 0xFFFF);
        signal.setFixed(fixed & 0xFFFF);
        signal.setRssi(SikValueToDB(rssi & 0xFF));
        signal.setRemrssi(SikValueToDB(remrssi & 0xFF));
        signal.setNoise(SikValueToDB(noise & 0xFF));
        signal.setRemnoise(SikValueToDB(remnoise & 0xFF));
        signal.setTxbuf(txbuf & 0xFF);
        signal.setSignalStrength(MathUtils.getSignalStrength(signal.getFadeMargin(), signal.getRemFadeMargin()));

        EventBus.getDefault().post(AttributeEvent.SIGNAL_UPDATED);
    }

    /**
     * Scalling done at the Si1000 radio More info can be found at:
     * http://copter.ardupilot.com/wiki/common-using-the-3dr-radio-for-telemetry-with-apm-and-px4/#Power_levels
     */
    protected double SikValueToDB(int value) {
        return (value / 1.9) - 127;
    }

    /**
     * 处理GPS消息
     *
     * @param gpsState
     */
    private void processGpsState(msg_gps_raw_int gpsState) {
        if (gpsState == null)
            return;

        Gps vehicleGps = drone.getVehicleGps();
        double newEph = gpsState.eph / 100.0; // convert from eph(cm) to gps_eph(m)
        if (vehicleGps.getSatellitesCount() != gpsState.satellites_visible || vehicleGps.getGpsEph() != newEph) {
            vehicleGps.setSatCount(gpsState.satellites_visible);
            vehicleGps.setGpsEph(newEph);
            EventBus.getDefault().post(AttributeEvent.GPS_COUNT);
        }

        if (vehicleGps.getFixType() != gpsState.fix_type) {
            vehicleGps.setFixType(gpsState.fix_type);
            EventBus.getDefault().post(AttributeEvent.GPS_FIX);
        }
    }

    /**
     * 更新Home点位置
     *
     * @param missionItem
     */
    public void processHomeUpdate(msg_mission_item missionItem) {
        if (missionItem.seq != APMConstants.HOME_WAYPOINT_INDEX) {
            return;
        }

        float latitude = missionItem.x;
        float longitude = missionItem.y;
        float altitude = missionItem.z;
        boolean homeUpdated = false;

        LatLongAlt homeCoord = drone.getVehicleHome().getCoordinate();
        if (homeCoord == null) {
            drone.getVehicleHome().setCoordinate(new LatLongAlt(latitude, longitude, altitude));
            homeUpdated = true;
        } else {
            if (homeCoord.getLatitude() != latitude || homeCoord.getLongitude() != longitude || homeCoord.getAltitude() != altitude) {
                homeCoord.setLatitude(latitude);
                homeCoord.setLongitude(longitude);
                homeCoord.setAltitude(altitude);
                homeUpdated = true;
            }
        }

        if (homeUpdated) {
            EventBus.getDefault().post(AttributeEvent.HOME_UPDATED);
        }
    }
}
