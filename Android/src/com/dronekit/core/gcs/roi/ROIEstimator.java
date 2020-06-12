package com.dronekit.core.gcs.roi;

import android.os.Handler;

import com.dronekit.core.MAVLink.command.doCmd.MavLinkDoCmds;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.gcs.location.Location;
import com.dronekit.core.gcs.location.Location.LocationReceiver;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.dronekit.core.helpers.geoTools.GeoTools;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Uses location data from Android's FusedLocation LocationManager at 1Hz and
 * calculates new points at 10Hz based on Last Location and Last Velocity.
 */
public class ROIEstimator implements LocationReceiver {

    // 估算时间：10Hz====100ms
    private static final int TIMEOUT = 2000;

    protected final Drone drone;
    protected final AtomicBoolean isFollowEnabled = new AtomicBoolean(false);
    protected Location realLocation;
    protected long timeOfLastLocation;
    protected Handler watchdog;
    protected Runnable watchdogCallback = new Runnable() {
        @Override
        public void run() {
            updateROI();
        }

    };

    public ROIEstimator(Drone drone, Handler handler) {
        this.watchdog = handler;
        this.drone = drone;
    }

    public void enableFollow() {
        MavLinkDoCmds.resetROI(drone, null);
        isFollowEnabled.set(true);
    }

    public void disableFollow() {
        if (isFollowEnabled.compareAndSet(true, false)) {
            realLocation = null;
            MavLinkDoCmds.resetROI(drone, null);
            disableWatchdog();
        }
    }

    @Override
    public final void onLocationUpdate(Location location) {
        if (!isFollowEnabled.get())
            return;

        realLocation = location;
        timeOfLastLocation = System.currentTimeMillis();

        disableWatchdog();
        updateROI();
    }

    @Override
    public void onLocationUnavailable() {
        disableWatchdog();
    }

    protected void disableWatchdog() {
        watchdog.removeCallbacks(watchdogCallback);
    }

    protected void updateROI() {
        if (realLocation == null) {
            return;
        }

        LatLong gcsCoord = realLocation.getCoord();

        double bearing = realLocation.getBearing();
        double distanceTraveledSinceLastPoint = realLocation.getSpeed() * (System.currentTimeMillis() - timeOfLastLocation) / 1000f;
        LatLong goCoord = GeoTools.newCoordFromBearingAndDistance(gcsCoord, bearing, distanceTraveledSinceLastPoint);

        sendUpdateROI(goCoord);

        if (realLocation.getSpeed() > 0)
            watchdog.postDelayed(watchdogCallback, getUpdatePeriod());
    }

    protected void sendUpdateROI(LatLong goCoord) {
        MavLinkDoCmds.setROI(drone, new LatLongAlt(goCoord.getLatitude(), goCoord.getLongitude(), (0.0)), null);
    }

    public boolean isFollowEnabled() {
        return isFollowEnabled.get();
    }

    protected long getUpdatePeriod() {
        return TIMEOUT;
    }
}
