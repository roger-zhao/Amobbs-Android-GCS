package com.dronekit.core.drone.variables;

import android.os.Handler;

import com.dronekit.core.MAVLink.MavLinkCommands;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.drone.property.Altitude;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.error.CommandExecutionError;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class GuidedPoint extends DroneVariable {

    private final Handler handler;
    private GuidedStates state = GuidedStates.UNINITIALIZED;
    private LatLong coord = new LatLong(0, 0);
    private double altitude = 0.0; //altitude in meters
    private Runnable mPostInitializationTask;

    public GuidedPoint(Drone myDrone, Handler handler) {
        super(myDrone);
        this.handler = handler;
        EventBus.getDefault().register(this);
    }

    public static boolean isGuidedMode(Drone drone) {
        if (drone == null)
            return false;

        final int droneType = drone.getType().getType();
        final ApmModes droneMode = drone.getState().getMode();

        if (Type.isCopter(droneType)) {
            return droneMode == ApmModes.ROTOR_GUIDED;
        }

        if (Type.isPlane(droneType)) {
            return droneMode == ApmModes.FIXED_WING_GUIDED;
        }

        if (Type.isRover(droneType)) {
            return droneMode == ApmModes.ROVER_GUIDED || droneMode == ApmModes.ROVER_HOLD;
        }

        return false;
    }

    private static LatLong getGpsPosition(Drone drone) {
        final Gps droneGps = drone.getVehicleGps();
        return droneGps == null ? null : droneGps.getPosition();
    }

    public static void changeToGuidedMode(Drone drone, ICommandListener listener) {
        final State droneState = drone.getState();
        final int droneType = drone.getType().getType();

        if (Type.isCopter(droneType)) {
            droneState.changeFlightMode(ApmModes.ROTOR_GUIDED, listener);
        } else if (Type.isPlane(droneType)) {
            // You have to send a guided point to the plane in order to trigger guided mode.
            forceSendGuidedPoint(drone, getGpsPosition(drone), getDroneAltConstrained(drone));
        } else if (Type.isRover(droneType)) {
            droneState.changeFlightMode(ApmModes.ROVER_GUIDED, listener);
        }
    }

    public static void forceSendGuidedPoint(Drone drone, LatLong coord, double altitudeInMeters) {
        EventBus.getDefault().post(AttributeEvent.GUIDED_POINT_UPDATED);
        if (coord != null) {
            MavLinkCommands.setGuidedMode(drone, coord.getLatitude(), coord.getLongitude(), altitudeInMeters);
        }
    }

    public static void forceSendGuidedPointAndVelocity(Drone drone, LatLong coord, double altitudeInMeters, double xVel, double yVel, double zVel) {
        EventBus.getDefault().post(AttributeEvent.GUIDED_POINT_UPDATED);
        if (coord != null) {
            MavLinkCommands.sendGuidedPositionAndVelocity(drone, coord.getLatitude(), coord.getLongitude(), altitudeInMeters, xVel, yVel, zVel);
        }
    }

    private static double getDroneAltConstrained(Drone drone) {
        final Altitude droneAltitude = drone.getAltitude();
        double alt = Math.floor(droneAltitude.getAltitude());
        return Math.max(alt, getDefaultMinAltitude(drone));
    }

    public static float getDefaultMinAltitude(Drone drone) {
        final int droneType = drone.getType().getType();
        if (Type.isCopter(droneType)) {
            return 2f;
        } else if (Type.isPlane(droneType)) {
            return 15f;
        } else {
            return 0f;
        }
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case HEARTBEAT_FIRST:
            case HEARTBEAT_RESTORED:
            case STATE_VEHICLE_MODE:
                if (isGuidedMode(myDrone)) {
                    initialize();
                } else {
                    disable();
                }
                break;

            case STATE_DISCONNECTED:
            case HEARTBEAT_TIMEOUT:
                disable();
        }
    }


    public void pauseAtCurrentLocation(ICommandListener listener) {
        if (state == GuidedStates.UNINITIALIZED) {
            changeToGuidedMode(myDrone, listener);
        } else {
            newGuidedCoord(getGpsPosition());
            state = GuidedStates.IDLE;
        }
    }

    private LatLong getGpsPosition() {
        return getGpsPosition(myDrone);
    }

    public void doGuidedTakeoff(final double alt) {
        if (Type.isCopter(myDrone.getType().getType())) {
            coord = getGpsPosition();
            altitude = alt;
            state = GuidedStates.IDLE;
            changeToGuidedMode(myDrone, null);
            MavLinkCommands.sendTakeoff(myDrone, alt, null);
            EventBus.getDefault().post(AttributeEvent.GUIDED_POINT_UPDATED);
        }
    }

    public void newGuidedCoord(LatLong coord) {
        changeCoord(coord);
    }

    public void newGuidedPosition(double latitude, double longitude, double altitude) {
        MavLinkCommands.sendGuidedPosition(myDrone, latitude, longitude, altitude);
    }

    public void newGuidedVelocity(double xVel, double yVel, double zVel) {
        MavLinkCommands.sendGuidedVelocity(myDrone, xVel, yVel, zVel);
    }

    public void newGuidedCoordAndVelocity(LatLong coord, double xVel, double yVel, double zVel) {
        changeCoordAndVelocity(coord, xVel, yVel, zVel);
    }

    public void changeGuidedAltitude(double alt) {
        changeAlt(alt);
    }

    public void forcedGuidedCoordinate(final LatLong coord, final ICommandListener listener) {
        final Gps droneGps = myDrone.getVehicleGps();
        if (!droneGps.has3DLock()) {
            postErrorEvent(handler, listener, CommandExecutionError.COMMAND_FAILED);
            return;
        }

        if (isInitialized()) {
            changeCoord(coord);
            postSuccessEvent(handler, listener);
        } else {
            mPostInitializationTask = new Runnable() {
                @Override
                public void run() {
                    changeCoord(coord);
                }
            };

            changeToGuidedMode(myDrone, listener);
        }
    }

    public void forcedGuidedCoordinate(final LatLong coord, final double alt, final ICommandListener listener) {
        final Gps droneGps = myDrone.getVehicleGps();
        if (!droneGps.has3DLock()) {
            postErrorEvent(handler, listener, CommandExecutionError.COMMAND_FAILED);
            return;
        }

        if (isInitialized()) {
            changeCoord(coord);
            changeAlt(alt);
            postSuccessEvent(handler, listener);
        } else {
            mPostInitializationTask = new Runnable() {
                @Override
                public void run() {
                    changeCoord(coord);
                    changeAlt(alt);
                }
            };

            changeToGuidedMode(myDrone, listener);
        }
    }

    private void initialize() {
        if (state == GuidedStates.UNINITIALIZED) {
            coord = getGpsPosition();
            altitude = getDroneAltConstrained(myDrone);
            state = GuidedStates.IDLE;
            EventBus.getDefault().post(AttributeEvent.GUIDED_POINT_UPDATED);
        }

        if (mPostInitializationTask != null) {
            mPostInitializationTask.run();
            mPostInitializationTask = null;
        }
    }

    private void disable() {
        if (state == GuidedStates.UNINITIALIZED)
            return;

        state = GuidedStates.UNINITIALIZED;
        EventBus.getDefault().post(AttributeEvent.GUIDED_POINT_UPDATED);
    }

    private void changeAlt(double alt) {
        switch (state) {
            case UNINITIALIZED:
                break;

            case IDLE:
                state = GuidedStates.ACTIVE;
                /** FALL THROUGH **/

            case ACTIVE:
                altitude = alt;
                sendGuidedPoint();
                break;
        }
    }

    private void changeCoord(LatLong coord) {
        switch (state) {
            case UNINITIALIZED:
                break;

            case IDLE:
                state = GuidedStates.ACTIVE;
                /** FALL THROUGH **/
            case ACTIVE:
                this.coord = coord;
                sendGuidedPoint();
                break;
        }
    }

    private void changeCoordAndVelocity(LatLong coord, double xVel, double yVel, double zVel) {
        switch (state) {
            case UNINITIALIZED:
                break;

            case IDLE:
                state = GuidedStates.ACTIVE;
                /** FALL THROUGH **/
            case ACTIVE:
                this.coord = coord;
                sendGuidedPointAndVelocity(xVel, yVel, zVel);
                break;
        }
    }

    private void sendGuidedPointAndVelocity(double xVel, double yVel, double zVel) {
        if (state == GuidedStates.ACTIVE) {
            forceSendGuidedPointAndVelocity(myDrone, coord, altitude, xVel, yVel, zVel);
        }
    }

    private void sendGuidedPoint() {
        if (state == GuidedStates.ACTIVE) {
            forceSendGuidedPoint(myDrone, coord, altitude);
        }
    }

    public LatLong getCoord() {
        return coord;
    }

    public double getAltitude() {
        return this.altitude;
    }

    // 获取引导坐标和高度
    public LatLongAlt getLatLongAlt() {
        return new LatLongAlt(coord, altitude);
    }

    public boolean isActive() {
        return (state == GuidedStates.ACTIVE);
    }

    public boolean isIdle() {
        return (state == GuidedStates.IDLE);
    }

    public boolean isInitialized() {
        return !(state == GuidedStates.UNINITIALIZED);
    }

    public GuidedStates getState() {
        return state;
    }

    public enum GuidedStates {
        UNINITIALIZED, IDLE, ACTIVE
    }
}
