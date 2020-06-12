package com.dronekit.core.mission.commands;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;
import com.dronekit.core.helpers.geoTools.GeoTools;
import com.dronekit.core.mission.Mission;
import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;

import java.util.List;

public class ConditionYawImpl extends MissionCMD {
    private boolean isRelative = false;
    private double angle = 0;
    private double angularSpeed = 0;

    public ConditionYawImpl(MissionItemImpl item) {
        super(item);
    }

    public ConditionYawImpl(msg_mission_item msg, Mission mission) {
        super(mission);
        unpackMAVMessage(msg);
    }

    public ConditionYawImpl(Mission mission, double angle, boolean isRelative) {
        super(mission);
        setAngle(angle);
        setRelative(isRelative);
    }

    @Override
    public List<msg_mission_item> packMissionItem() {
        List<msg_mission_item> list = super.packMissionItem();
        msg_mission_item mavMsg = list.get(0);
        mavMsg.command = MAV_CMD.MAV_CMD_CONDITION_YAW;
        mavMsg.param1 = (float) GeoTools.warpToPositiveAngle(angle);
        mavMsg.param2 = (float) Math.abs(angularSpeed);
        mavMsg.param3 = (angularSpeed < 0) ? 1 : -1;
        mavMsg.param4 = isRelative ? 1 : 0;
        return list;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item mavMsg) {
        isRelative = mavMsg.param4 != 0;
        angle = mavMsg.param1;
        angularSpeed = mavMsg.param2 * (mavMsg.param3 > 0 ? -1 : +1);
    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.CONDITION_YAW;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getAngularSpeed() {
        return angularSpeed;
    }

    public void setAngularSpeed(double angularSpeed) {
        this.angularSpeed = angularSpeed;
    }

    public boolean isRelative() {
        return isRelative;
    }

    public void setRelative(boolean isRelative) {
        this.isRelative = isRelative;
    }
}