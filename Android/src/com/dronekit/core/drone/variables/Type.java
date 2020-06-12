package com.dronekit.core.drone.variables;

import com.MAVLink.enums.MAV_TYPE;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.firmware.FirmwareType;
import com.evenbus.AttributeEvent;
import com.github.zafarkhaja.semver.Version;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores information about the drone's type.
 */
public class Type extends DroneVariable {

    // 判断是否为固定翼，直升机，多旋翼……
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_PLANE = 1;
    public static final int TYPE_COPTER = 2;
    public static final int TYPE_ROVER = 10;

    public static final String FIRMWARE_VERSION_NUMBER_REGEX = "\\d+(\\.\\d{1,2})?";

    private static final int DEFAULT_TYPE = MAV_TYPE.MAV_TYPE_GENERIC;
    public Version firmwareVersionNumber = Version.forIntegers(0, 0, 0);
    private int type = DEFAULT_TYPE;
    private String firmwareVersion = null;

    public Type(Drone myDrone) {
        super(myDrone);
        EventBus.getDefault().register(this);
    }

    // 是否多旋翼
    public static boolean isCopter(int type) {
        switch (type) {
            case MAV_TYPE.MAV_TYPE_TRICOPTER:
            case MAV_TYPE.MAV_TYPE_QUADROTOR:
            case MAV_TYPE.MAV_TYPE_HEXAROTOR:
            case MAV_TYPE.MAV_TYPE_OCTOROTOR:
            case MAV_TYPE.MAV_TYPE_HELICOPTER:
                return true;

            default:
                return false;
        }
    }

    // 是否固定翼
    public static boolean isPlane(int type) {
        return type == MAV_TYPE.MAV_TYPE_FIXED_WING;
    }

    // 是否为Rover
    public static boolean isRover(int type) {
        return type == MAV_TYPE.MAV_TYPE_GROUND_ROVER;
    }

    /**
     * 匹配固件号
     *
     * @param firmwareVersion
     * @return
     */
    private static Version extractVersionNumber(String firmwareVersion) {
        Version version = Version.forIntegers(0, 0, 0);

        Pattern pattern = Pattern.compile(FIRMWARE_VERSION_NUMBER_REGEX);
        Matcher matcher = pattern.matcher(firmwareVersion);
        if (matcher.find()) {
            String versionNumber = matcher.group(0) + ".0"; // Adding a default patch version number for successful parsing.

            try {
                version = Version.valueOf(versionNumber);
            } catch (Exception e) {
                Logger.e(e, "Firmware version invalid");
            }
        }

        return version;
    }

    public int getDroneType() {
        if (isCopter(type))
            return TYPE_COPTER;
        else if (isPlane(type))
            return TYPE_PLANE;
        else if (isRover(type))
            return TYPE_ROVER;
        else
            return TYPE_UNKNOWN;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        if (this.type != type) {
            this.type = type;
            EventBus.getDefault().post(AttributeEvent.TYPE_UPDATED);
        }
    }

    // 获取固件版本
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    // 设置固件版本
    public void setFirmwareVersion(String message) {
        if (firmwareVersion == null || !firmwareVersion.equals(message)) {
            firmwareVersion = message;
            // 版本号
            firmwareVersionNumber = extractVersionNumber(message);
            EventBus.getDefault().post(AttributeEvent.TYPE_UPDATED);
        }
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_DISCONNECTED:
                setType(DEFAULT_TYPE);
                break;
        }
    }

    public FirmwareType getFirmwareType() {
        if (myDrone.getMavClient().isConnected()) {
            switch (this.type) {
                case MAV_TYPE.MAV_TYPE_FIXED_WING:
                    return FirmwareType.ARDU_PLANE;

                case MAV_TYPE.MAV_TYPE_GENERIC:
                case MAV_TYPE.MAV_TYPE_QUADROTOR:
                case MAV_TYPE.MAV_TYPE_COAXIAL:
                case MAV_TYPE.MAV_TYPE_HELICOPTER:
                case MAV_TYPE.MAV_TYPE_HEXAROTOR:
                case MAV_TYPE.MAV_TYPE_OCTOROTOR:
                case MAV_TYPE.MAV_TYPE_TRICOPTER:
                    return FirmwareType.ARDU_COPTER;

                case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                case MAV_TYPE.MAV_TYPE_SURFACE_BOAT:
                    return FirmwareType.ARDU_ROVER;

                default:
                    // unsupported - fall thru to offline condition
            }
        }
        return FirmwareType.ARDU_COPTER;
    }

    public Version getFirmwareVersionNumber() {
        return firmwareVersionNumber;
    }
}