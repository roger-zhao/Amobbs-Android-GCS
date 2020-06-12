package com.dronekit.core.gcs.follow;

import android.os.Handler;

import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.gcs.location.Location;
import com.dronekit.core.helpers.coordinates.LatLong;

public class FollowAbove extends FollowAlgorithm {

    protected final Drone drone;

    public FollowAbove(DroneManager droneMgr, Handler handler) {
        super(droneMgr, handler);
        this.drone = droneMgr.getDrone();
    }

    @Override
    public FollowModes getType() {
        return FollowModes.ABOVE;
    }

    @Override
    protected void processNewLocation(Location location) {
        final LatLong gcsCoord = new LatLong(location.getCoord());
        drone.getGuidedPoint().newGuidedCoord(gcsCoord);
    }
}
