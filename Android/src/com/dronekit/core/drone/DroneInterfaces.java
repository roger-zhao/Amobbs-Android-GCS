package com.dronekit.core.drone;

import com.dronekit.core.MAVLink.WaypointManager;
import com.dronekit.core.drone.property.Parameter;

public class DroneInterfaces {

    public interface OnParameterManagerListener {
        void onBeginReceivingParameters();

        void onParameterReceived(Parameter parameter, int index, int count);

        void onEndReceivingParameters();
    }

    public interface OnWaypointManagerListener {
        void onBeginWaypointEvent(WaypointManager.WaypointEvent_Type wpEvent);

        void onWaypointEvent(WaypointManager.WaypointEvent_Type wpEvent, int index, int count);

        void onEndWaypointEvent(WaypointManager.WaypointEvent_Type wpEvent);
    }
}
