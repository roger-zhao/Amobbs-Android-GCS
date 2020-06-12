package com.dronekit.core.drone.property;

import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;

public class GimbalOrientation extends DroneVariable {

    private float pitch;
    private float roll;
    private float yaw;

    public GimbalOrientation(Drone myDrone) {
        super(myDrone);
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
    }

    public float getYaw() {
        return yaw;
    }

    public void updateOrientation(float pitch, float roll, float yaw) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
    }
}
