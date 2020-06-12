package com.dronekit.core.mission.commands;

import com.MAVLink.common.msg_mission_item;
import com.dronekit.core.mission.Mission;
import com.dronekit.core.mission.MissionItemImpl;

import java.util.List;

public abstract class MissionCMD extends MissionItemImpl {

    public MissionCMD(Mission mission) {
        super(mission);
    }

    public MissionCMD(MissionItemImpl item) {
        super(item);
    }

    @Override
    public List<msg_mission_item> packMissionItem() {
        return super.packMissionItem();
    }
}