package com.dronekit.core.mission;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.mission.commands.CameraTriggerImpl;
import com.dronekit.core.mission.commands.ChangeSpeedImpl;
import com.dronekit.core.mission.commands.ConditionYawImpl;
import com.dronekit.core.mission.commands.DoJumpImpl;
import com.dronekit.core.mission.commands.EpmGripperImpl;
import com.dronekit.core.mission.commands.ReturnToHomeImpl;
import com.dronekit.core.mission.commands.SetRelayImpl;
import com.dronekit.core.mission.commands.SetServoImpl;
import com.dronekit.core.mission.commands.TakeoffImpl;
import com.dronekit.core.mission.survey.SplineSurveyImpl;
import com.dronekit.core.mission.survey.SurveyImpl;
import com.dronekit.core.mission.waypoints.CircleImpl;
import com.dronekit.core.mission.waypoints.DoLandStartImpl;
import com.dronekit.core.mission.waypoints.LandImpl;
import com.dronekit.core.mission.waypoints.RegionOfInterestImpl;
import com.dronekit.core.mission.waypoints.SplineWaypointImpl;
import com.dronekit.core.mission.waypoints.StructureScannerImpl;
import com.dronekit.core.mission.waypoints.WaypointImpl;

import java.util.Collections;

public enum MissionItemType {
    TAKEOFF("起飞点(Takeoff)"),
    LAND("着陆点(Land)"),
    CIRCLE("环绕点(Circle)"),
    RTL("返航点(Return to Launch)"),
    WAYPOINT("航点(Waypoint)"),
    SPLINE_WAYPOINT("曲线航点(Spline Waypoint)"),
    ROI("兴趣点(Region of Interest)"),
    CHANGE_SPEED("改变航速(Change Speed)"),
    CONDITION_YAW("航向设置(Set Yaw)"),
    SET_SERVO("舵机设置(Set Servo)"),
    CAMERA_TRIGGER("相机快门(Camera Trigger)"),
    EPM_GRIPPER("电磁阀开关(EPM)"),
    SET_RELAY("设置继电器(Set Relay)"),
    SURVEY("作业区域"),
    SPLINE_SURVEY("曲线作业区域"),
    CYLINDRICAL_SURVEY("结构建模(Structure Scan)"),
    DO_LAND_START("开始着陆(Do Land Start)"),
    DO_JUMP("跳转至(Do Jump)"),
    RESET_ROI("重置兴趣点(Reset ROI)");

    private final String name;

    MissionItemType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MissionItemImpl getNewItem(MissionItemImpl referenceItem) throws IllegalArgumentException {
        switch (this) {
            case WAYPOINT:
                return new WaypointImpl(referenceItem);
            case SPLINE_WAYPOINT:
                return new SplineWaypointImpl(referenceItem);
            case TAKEOFF:
                return new TakeoffImpl(referenceItem);
            case CHANGE_SPEED:
                return new ChangeSpeedImpl(referenceItem);
            case CAMERA_TRIGGER:
                return new CameraTriggerImpl(referenceItem);
            case EPM_GRIPPER:
                return new EpmGripperImpl(referenceItem);
            case RTL:
                return new ReturnToHomeImpl(referenceItem);
            case LAND:
                return new LandImpl(referenceItem);
            case CIRCLE:
                return new CircleImpl(referenceItem);
            case ROI:
                return new RegionOfInterestImpl(referenceItem);
            case SURVEY:
                return new SurveyImpl(referenceItem.getMission(), Collections.<LatLong>emptyList());
            case SPLINE_SURVEY:
                return new SplineSurveyImpl(referenceItem.getMission(), Collections.<LatLong>emptyList());
            case CYLINDRICAL_SURVEY:
                return new StructureScannerImpl(referenceItem);
            case SET_SERVO:
                return new SetServoImpl(referenceItem);
            case CONDITION_YAW:
                return new ConditionYawImpl(referenceItem);
            case SET_RELAY:
                return new SetRelayImpl(referenceItem);
            case DO_LAND_START:
                return new DoLandStartImpl(referenceItem);
            case DO_JUMP:
                return new DoJumpImpl(referenceItem);
            default:
                throw new IllegalArgumentException("Unrecognized mission item type (" + name + ")" + "");
        }
    }
}
