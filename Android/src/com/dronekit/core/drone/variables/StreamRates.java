package com.dronekit.core.drone.variables;

import com.dronekit.core.MAVLink.MavLinkStreamRates;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class StreamRates extends DroneVariable {

    private Rates rates;

    public StreamRates(Drone myDrone) {
        super(myDrone);
        EventBus.getDefault().register(this);
    }

    public void setRates(Rates rates) {
        this.rates = rates;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTED:
            case HEARTBEAT_FIRST:
            case HEARTBEAT_RESTORED:
                setupStreamRatesFromPref();
                break;
        }
    }

    public void setupStreamRatesFromPref() {
        if (rates == null)
            return;

        MavLinkStreamRates.setupStreamRates(myDrone.getMavClient(), myDrone.getSysid(),
                myDrone.getCompid(), rates.extendedStatus, rates.extra1, rates.extra2,
                rates.extra3, rates.position, rates.rcChannels, rates.rawSensors,
                rates.rawController);
    }

    public static class Rates {
        public int extendedStatus;
        public int extra1;
        public int extra2;
        public int extra3;
        public int position;
        public int rcChannels;
        public int rawSensors;
        public int rawController;
    }
}
