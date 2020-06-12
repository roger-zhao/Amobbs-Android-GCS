package com.dronekit.core.gcs.follow;

import android.os.Handler;

import com.dronekit.core.drone.DroneManager;

public class FollowLead extends FollowHeadingAngle {

    public FollowLead(DroneManager droneMgr, Handler handler, double radius) {
        super(droneMgr, handler, radius, 0.0);
    }

    @Override
    public FollowModes getType() {
        return FollowModes.LEAD;
    }
}
