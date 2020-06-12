package com.dronekit.core.gcs.follow;

import android.os.Handler;

import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.gcs.location.Location;

/**
 * Created by Fredia Huya-Kouadio on 3/23/15.
 */
public class FollowLookAtMe extends FollowAlgorithm {

    public FollowLookAtMe(DroneManager droneMgr, Handler handler) {
        super(droneMgr, handler);
    }

    @Override
    protected void processNewLocation(Location location) {
    }

    @Override
    public FollowModes getType() {
        return FollowModes.LOOK_AT_ME;
    }
}
