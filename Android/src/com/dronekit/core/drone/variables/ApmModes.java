package com.dronekit.core.drone.variables;

import com.MAVLink.enums.MAV_TYPE;

import java.util.ArrayList;
import java.util.List;

public enum ApmModes {
    FIXED_WING_MANUAL(0, "手动模式", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_CIRCLE(1, "绕圈", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_STABILIZE(2, "自稳模式", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_TRAINING(3, "练习模式", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_ACRO(4, "特技模式", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_FLY_BY_WIRE_A(5, "FBW A", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_FLY_BY_WIRE_B(6, "FBW B", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_CRUISE(7, "巡航模式", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_AUTOTUNE(8, "自动调参", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_AUTO(10, "自动模式", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_RTL(11, "返航", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_LOITER(12, "留待模式", MAV_TYPE.MAV_TYPE_FIXED_WING),
    FIXED_WING_GUIDED(15, "引导模式", MAV_TYPE.MAV_TYPE_FIXED_WING),

    ROTOR_STABILIZE(0, "姿态模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_ACRO(1, "特技模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_ALT_HOLD(2, "手动增稳", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_AUTO(3, "自动航线", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_GUIDED(4, "起飞模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_LOITER(5, "自动悬停", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_RTL(6, "返航降落", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_CIRCLE(7, "绕圈模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_LAND(9, "降落", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_TOY(11, "漂移模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_SPORT(13, "运动模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_AUTOTUNE(15, "自动调参", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_POSHOLD(16, "定点模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_BRAKE(17, "刹车模式", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_OBJTRACK(22, "目标跟踪", MAV_TYPE.MAV_TYPE_QUADROTOR),
    ROTOR_AB_POINT(0x4F, "AB作业", MAV_TYPE.MAV_TYPE_QUADROTOR),

    ROVER_MANUAL(0, "MANUAL", MAV_TYPE.MAV_TYPE_GROUND_ROVER),
    ROVER_LEARNING(2, "LEARNING", MAV_TYPE.MAV_TYPE_GROUND_ROVER),
    ROVER_STEERING(3, "STEERING", MAV_TYPE.MAV_TYPE_GROUND_ROVER),
    ROVER_HOLD(4, "HOLD", MAV_TYPE.MAV_TYPE_GROUND_ROVER),
    ROVER_AUTO(10, "AUTO", MAV_TYPE.MAV_TYPE_GROUND_ROVER),
    ROVER_RTL(11, "RTL", MAV_TYPE.MAV_TYPE_GROUND_ROVER),
    ROVER_GUIDED(15, "GUIDED", MAV_TYPE.MAV_TYPE_GROUND_ROVER),
    ROVER_INITIALIZING(16, "INITIALIZING", MAV_TYPE.MAV_TYPE_GROUND_ROVER),

    UNKNOWN(-1, "未知模式", MAV_TYPE.MAV_TYPE_GENERIC);

    private final long number;
    private final String label;
    private final int type;

    ApmModes(long number, String label, int type) {
        this.number = number;
        this.label = label;
        this.type = type;
    }

    public static ApmModes getMode(long i, int type) {
        if (isCopter(type)) {
            type = MAV_TYPE.MAV_TYPE_QUADROTOR;
        }

        for (ApmModes mode : ApmModes.values()) {
            if (i == mode.getNumber() && type == mode.getType()) {
                return mode;
            }
        }
        return UNKNOWN;
    }

    public static ApmModes getMode(String str, int type) {
        if (isCopter(type)) {
            type = MAV_TYPE.MAV_TYPE_QUADROTOR;
        }

        for (ApmModes mode : ApmModes.values()) {
            if (str.equals(mode.getLabel()) && type == mode.getType()) {
                return mode;
            }
        }
        return UNKNOWN;
    }

    public static List<ApmModes> getModeList(int type) {
        List<ApmModes> modeList = new ArrayList<>();

        if (isCopter(type)) {
            type = MAV_TYPE.MAV_TYPE_QUADROTOR;
        }

        for (ApmModes mode : ApmModes.values()) {
            if (mode.getType() == type) {
                if((mode.number == 2) // alt_hold
                        || (mode.number == 3) // auto
                        || (mode.number == 4) // takeoff when guide in app-gcs
                        || (mode.number == 5) // loiter
                        || (mode.number == 6) // RTL
                        || (mode.number == 9) // land
                        || (mode.number == 22) // objtrack
                        || (mode.number == 0x4F) // AB point
                    )
                    modeList.add(mode);
            }
        }
        return modeList;
    }

    public static boolean isValid(ApmModes mode) {
        // AB point， guided, and objtrack for now, can only switched in RC
        return (mode != ApmModes.UNKNOWN && mode != ApmModes.ROTOR_AB_POINT && mode != ApmModes.ROTOR_OBJTRACK && mode != ApmModes.ROTOR_GUIDED);
    }

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

    public long getNumber() {
        return number;
    }

    public String getLabel() {
        return label;
    }

    public int getType() {
        return type;
    }
}
