package com.dronekit.core.gcs;

import android.support.annotation.IntDef;

import com.dronekit.api.CommonApiUtils;
import com.dronekit.core.MAVLink.MavLinkWaypoint;
import com.dronekit.core.MAVLink.command.doCmd.MavLinkDoCmds;
import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.APMConstants;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.drone.property.Home;
import com.dronekit.core.gcs.location.Location;
import com.dronekit.core.gcs.location.Location.LocationFinder;
import com.dronekit.core.gcs.location.Location.LocationReceiver;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.evenbus.AttributeEvent;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Return to me implementation.
 * If enabled, listen for user's gps location updates, and accordingly updates the vehicle RTL location.
 * Created by Fredia Huya-Kouadio on 9/21/15.
 */
public class ReturnToMe implements LocationReceiver {

    public static final int UPDATE_MINIMAL_DISPLACEMENT = 5; //meters
    public static final int STATE_IDLE = 0;                         // 空闲
    public static final int STATE_USER_LOCATION_UNAVAILABLE = 1;    // 用户位置不可用
    public static final int STATE_USER_LOCATION_INACCURATE = 2;     // 用户位置获取中
    public static final int STATE_WAITING_FOR_VEHICLE_GPS = 3;      // 等待飞行器的GPS定位
    public static final int STATE_UPDATING_HOME = 4;                // 更新Home点成功
    public static final int STATE_ERROR_UPDATING_HOME = 5;          // 更新Home点错误！
    private static final String TAG = ReturnToMe.class.getSimpleName();
    private final AtomicBoolean isEnabled = new AtomicBoolean(false);
    private final DroneManager droneMgr;
    private final LocationFinder locationFinder;
    // Home点原始位置
    private LatLongAlt originalHomeLocation;
    // Home点当前位置
    private LatLongAlt currentHomeLocation;
    @ReturnToMeStates
    private int state = STATE_IDLE;
    private ICommandListener commandListener;

    public ReturnToMe(DroneManager droneMgr, LocationFinder locationFinder) {
        this.droneMgr = droneMgr;
        this.locationFinder = locationFinder;
        locationFinder.addLocationListener(TAG, this);

        EventBus.getDefault().register(this);
    }

    // 使能
    public void enable(ICommandListener listener) {
        if (isEnabled.compareAndSet(false, true)) {
            this.commandListener = listener;

            final Home droneHome = droneMgr.getDrone().getVehicleHome();
            if (droneHome.isValid()) {
                // 记录飞行器的起飞原始位置
                originalHomeLocation = droneHome.getCoordinate();
            }

            // Enable return to me
            Logger.i("Enabling return to me.");
            locationFinder.enableLocationUpdates();
            updateCurrentState(STATE_WAITING_FOR_VEHICLE_GPS);
        }
    }

    // 失能
    public void disable() {
        if (isEnabled.compareAndSet(true, false)) {
            // Disable return to me
            Logger.i("Disabling return to me.");
            locationFinder.disableLocationUpdates();

            currentHomeLocation = null;
            updateCurrentState(STATE_IDLE);

            this.commandListener = null;
        }
    }

    @Override
    public void onLocationUpdate(Location location) {
        if (location.isAccurate()) {
            final Home home = droneMgr.getDrone().getVehicleHome();
            if (!home.isValid()) {
                updateCurrentState(STATE_WAITING_FOR_VEHICLE_GPS);
                return;
            }

            // 家的位置
            final LatLongAlt homePosition = home.getCoordinate();
            // Calculate the displacement between the home location and the user location.
            final LatLongAlt locationCoord = location.getCoord();

            final float[] results = new float[3];
            android.location.Location.distanceBetween(homePosition.getLatitude(), homePosition.getLongitude(),
                    locationCoord.getLatitude(), locationCoord.getLongitude(), results);
            final float displacement = results[0];

            if (displacement >= UPDATE_MINIMAL_DISPLACEMENT) {
                MavLinkDoCmds.setVehicleHome(droneMgr.getDrone(),
                        new LatLongAlt(locationCoord.getLatitude(), locationCoord.getLongitude(), homePosition.getAltitude()),
                        new ICommandListener() {
                            @Override
                            public void onSuccess() {
                                Logger.i("Updated vehicle home location to %s", locationCoord.toString());
                                MavLinkWaypoint.requestWayPoint(droneMgr.getDrone(), APMConstants.HOME_WAYPOINT_INDEX);
                                CommonApiUtils.postSuccessEvent(commandListener);
                                updateCurrentState(STATE_UPDATING_HOME);
                            }

                            @Override
                            public void onError(int executionError) {
                                Logger.e("Unable to update vehicle home location: %d", executionError);
                                CommonApiUtils.postErrorEvent(executionError, commandListener);
                                updateCurrentState(STATE_ERROR_UPDATING_HOME);

                            }

                            @Override
                            public void onTimeout() {
                                Logger.w("Vehicle home update timed out!");
                                CommonApiUtils.postTimeoutEvent(commandListener);
                                updateCurrentState(STATE_ERROR_UPDATING_HOME);
                            }
                        });
            }
        } else {
            updateCurrentState(STATE_USER_LOCATION_INACCURATE);
        }
    }

    @Override
    public void onLocationUnavailable() {
        if (isEnabled.get()) {
            updateCurrentState(STATE_USER_LOCATION_UNAVAILABLE);
            disable();
        }
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_DISCONNECTED:
                // Stops updating the vehicle RTL location
                disable();
                break;

            case HOME_UPDATED:
                if (isEnabled.get()) {
                    final LatLongAlt homeCoord = droneMgr.getDrone().getVehicleHome().getCoordinate();
                    if (originalHomeLocation == null)
                        originalHomeLocation = homeCoord;
                    else {
                        currentHomeLocation = homeCoord;
                    }
                }
                break;
        }
    }

    private void updateCurrentState(int state) {
        this.setState(state);
        EventBus.getDefault().post(AttributeEvent.RETURN_TO_ME_STATE_UPDATE);
    }

    @ReturnToMeStates
    public int getState() {
        return state;
    }

    public void setState(@ReturnToMeStates int state) {
        this.state = state;
    }

    public LatLongAlt getCurrentHomeLocation() {
        return currentHomeLocation;
    }

    public LatLongAlt getOriginalHomeLocation() {
        return originalHomeLocation;
    }

    @IntDef({STATE_IDLE, STATE_USER_LOCATION_UNAVAILABLE, STATE_USER_LOCATION_INACCURATE,
            STATE_WAITING_FOR_VEHICLE_GPS, STATE_UPDATING_HOME, STATE_ERROR_UPDATING_HOME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReturnToMeStates {
    }
}