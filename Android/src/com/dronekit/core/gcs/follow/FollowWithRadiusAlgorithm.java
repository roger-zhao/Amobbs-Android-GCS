package com.dronekit.core.gcs.follow;

import android.os.Handler;

import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.Drone;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Fredia Huya-Kouadio on 1/27/15.
 */
public abstract class FollowWithRadiusAlgorithm extends FollowAlgorithm {

    public static final String EXTRA_FOLLOW_RADIUS = "extra_follow_radius";

    protected final Drone drone;
    protected double radius;

    public FollowWithRadiusAlgorithm(DroneManager droneMgr, Handler handler, double radius) {
        super(droneMgr, handler);
        this.radius = radius;
        this.drone = droneMgr.getDrone();
    }

    @Override
    public Map<String, Object> getParams() {
        Map<String, Object> params = new HashMap<>();
        params.put(EXTRA_FOLLOW_RADIUS, radius);
        return params;
    }

    @Override
    public void updateAlgorithmParams(Map<String, ?> params) {
        super.updateAlgorithmParams(params);

        Double updatedRadius = (Double) params.get(EXTRA_FOLLOW_RADIUS);
        if (updatedRadius != null)
            this.radius = Math.max(0, updatedRadius);
    }
}
