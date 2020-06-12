package com.dronekit.core.drone.variables;

import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;

public class MissionStats extends DroneVariable {
    private double distanceToWp = 0;
    private int currentWP = -1;
    private int lastReachedWP = -1;

    public MissionStats(Drone myDrone) {
        super(myDrone);
    }

    public void setDistanceToWp(double disttowp) {
        this.distanceToWp = disttowp;
    }

    public void setWpno(int seq) {
        if (seq != currentWP) {
            this.currentWP = seq;
            EventBus.getDefault().post(AttributeEvent.MISSION_ITEM_UPDATED);
        }
    }

    public void setLastReachedWaypointNumber(int seq) {
        if (seq != lastReachedWP) {
            this.lastReachedWP = seq;
            EventBus.getDefault().post(AttributeEvent.MISSION_ITEM_REACHED);
        }
    }

    public int getCurrentWP() {
        return currentWP;
    }

    public int getLastReachedWP() {
        return lastReachedWP;
    }

    public double getDistanceToWP() {
        return distanceToWp;
    }
}
