package com.dronekit.core.firmware;

import com.MAVLink.enums.MAV_AUTOPILOT;

// 枚举类：定义固件类型
public enum FirmwareType {
    /* APM firmware types */
    ARDU_PLANE(MAV_AUTOPILOT.MAV_AUTOPILOT_ARDUPILOTMEGA, "ArduPlane", "ArduPlane"),
    ARDU_COPTER(MAV_AUTOPILOT.MAV_AUTOPILOT_ARDUPILOTMEGA, "ArduCopter2", "ArduCopter"),
    ARDU_ROVER(MAV_AUTOPILOT.MAV_AUTOPILOT_ARDUPILOTMEGA, "ArduRover", "ArduRover"),

    /**
     * Generic firmware type
     */
    GENERIC(MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC, "", "Generic");

    private final String type;
    private final int family;
    private final String parameterMetadataGroup;

    /**
     * 构造方法
     *
     * @param family 家族
     * @param group  参数分组
     * @param type   类型
     */
    FirmwareType(int family, String group, String type) {
        this.family = family;
        this.type = type;
        this.parameterMetadataGroup = group;
    }

    public int getFamily() {
        return family;
    }

    public String getParameterMetadataGroup() {
        return parameterMetadataGroup;
    }

    @Override
    public String toString() {
        return type;
    }
}
