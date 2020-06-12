package com.dronekit.core.gcs.follow;

import android.widget.Toast;

import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.GuidedPoint;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.gcs.follow.FollowAlgorithm.FollowModes;
import com.dronekit.core.gcs.location.Location;
import com.dronekit.core.gcs.location.Location.LocationFinder;
import com.dronekit.core.gcs.location.Location.LocationReceiver;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.FishDroneGCSApp;


public class Follow implements LocationReceiver {

    // TAG标签
    private static final String TAG = Follow.class.getSimpleName();
    private final DroneManager droneMgr;
    private final LocationFinder locationFinder;
    private final Drone drone;
    private Location lastLocation;
    private FollowStates state = FollowStates.FOLLOW_INVALID_STATE;
    private FollowAlgorithm followAlgorithm;

    public Follow(DroneManager droneMgr, LocationFinder locationFinder) {
        this.droneMgr = droneMgr;
        drone = droneMgr.getDrone();
        EventBus.getDefault().register(this);

        followAlgorithm = FollowModes.LEASH.getAlgorithmType(droneMgr);

        // 设置监听器
        this.locationFinder = locationFinder;
        locationFinder.addLocationListener(TAG, this);
    }

    // 触发跟随
    public void toggleFollowMeState() {
        final Drone drone = droneMgr.getDrone();
        final State droneState = drone == null ? null : drone.getState();
        if (droneState == null) {
            state = FollowStates.FOLLOW_INVALID_STATE;
            return;
        }

        if (isEnabled()) {
            disableFollowMe();
        } else {
            if (droneMgr.isConnected()) {
                if (droneState.isArmed()) {
                    GuidedPoint.changeToGuidedMode(drone, null);
                    enableFollowMe();
                } else {
                    state = FollowStates.FOLLOW_DRONE_NOT_ARMED;
                }
            } else {
                state = FollowStates.FOLLOW_DRONE_DISCONNECTED;
            }
        }
    }

    public void enableFollowMe() {
        lastLocation = null;
        state = FollowStates.FOLLOW_START;

        locationFinder.enableLocationUpdates();
        followAlgorithm.enableFollow();

        EventBus.getDefault().post(AttributeEvent.FOLLOW_START);
    }

    public void disableFollowMe() {
        followAlgorithm.disableFollow();
        locationFinder.disableLocationUpdates();

        lastLocation = null;

        if (isEnabled()) {
            state = FollowStates.FOLLOW_END;
            EventBus.getDefault().post(AttributeEvent.FOLLOW_STOP);
        }
    }

    public boolean isEnabled() {
        return state == FollowStates.FOLLOW_RUNNING || state == FollowStates.FOLLOW_START;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_VEHICLE_MODE:
                if (isEnabled() && !GuidedPoint.isGuidedMode(drone)) {
                    disableFollowMe();
                }
                break;

            case HEARTBEAT_TIMEOUT:
            case STATE_DISCONNECTED:
                if (isEnabled()) {
                    disableFollowMe();
                }
                break;
        }
    }

    @Override
    public void onLocationUpdate(Location location) {
        if (location.isAccurate()) {
            state = FollowStates.FOLLOW_RUNNING;
            lastLocation = location;
            followAlgorithm.onLocationReceived(location);
        } else {
            state = FollowStates.FOLLOW_START;
            Toast.makeText(FishDroneGCSApp.getContext(),
                    "无法“跟随”,当前定位精度为：" + location.getAccuracy() + "米", Toast.LENGTH_SHORT).show();
        }

        EventBus.getDefault().post(AttributeEvent.FOLLOW_UPDATE);
    }

    @Override
    public void onLocationUnavailable() {
        disableFollowMe();
    }

    public void setAlgorithm(FollowAlgorithm algorithm) {
        if (followAlgorithm != null && followAlgorithm != algorithm) {
            followAlgorithm.disableFollow();
        }

        followAlgorithm = algorithm;
        if (isEnabled()) {
            followAlgorithm.enableFollow();

            if (lastLocation != null)
                followAlgorithm.onLocationReceived(lastLocation);
        }

        EventBus.getDefault().post(AttributeEvent.FOLLOW_UPDATE);
    }

    public FollowAlgorithm getFollowAlgorithm() {
        return followAlgorithm;
    }

    public FollowStates getState() {
        return state;
    }

    /**
     * Set of return value for the 'toggleFollowMeState' method.
     */
    public enum FollowStates {
        FOLLOW_INVALID_STATE, FOLLOW_DRONE_NOT_ARMED, FOLLOW_DRONE_DISCONNECTED, FOLLOW_START, FOLLOW_RUNNING, FOLLOW_END
    }
}
