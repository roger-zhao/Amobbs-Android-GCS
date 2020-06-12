package com.dronekit.core.gcs;

import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.enums.MAV_AUTOPILOT;
import com.MAVLink.enums.MAV_TYPE;
import com.dronekit.core.drone.autopilot.Drone;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to send periodic heartbeat messages to the drone.
 */
public class GCSHeartbeat {

    /**
     * This is the msg heartbeat used to check the drone is present, and responding.
     */
    private static final msg_heartbeat sMsg = new msg_heartbeat();

    static {
        sMsg.type = MAV_TYPE.MAV_TYPE_GCS;
        sMsg.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC;
    }

    /**
     * This is the heartbeat period in seconds.
     */
    private final int period;
    /**
     * This is the drone to send the heartbeat message to.
     */
    private final Drone drone;

    /**
     * Runnable used to send the heartbeat.
     */
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (drone != null)
                drone.getMavClient().sendMavMessage(sMsg, null);
        }
    };

    /**
     * ScheduledExecutorService used to periodically schedule the heartbeat.
     */
    private ScheduledExecutorService heartbeatExecutor;

    public GCSHeartbeat(Drone drone, int freqHz) {
        this.drone = drone;
        this.period = freqHz;
    }

    /**
     * Set the state of the heartbeat.
     *
     * @param active true to activate the heartbeat, false to deactivate it
     */
    public synchronized void setActive(boolean active) {
        if (active) {
            if (heartbeatExecutor == null || heartbeatExecutor.isShutdown()) {
                heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
                heartbeatExecutor.scheduleWithFixedDelay(heartbeatRunnable, 0, period, TimeUnit.SECONDS);
            }
        } else if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
    }
}
