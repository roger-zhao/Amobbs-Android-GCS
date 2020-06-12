package com.dronekit.core.gcs.follow;

import android.os.Handler;

import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.gcs.location.Location;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.dronekit.core.helpers.geoTools.GeoTools;


/**
 * Created by fhuya on 1/5/15.
 */
public class FollowSplineLeash extends FollowWithRadiusAlgorithm {
    public FollowSplineLeash(DroneManager droneMgr, Handler handler, double length) {
        super(droneMgr, handler, length);
    }

    @Override
    public void processNewLocation(Location location) {
        final LatLongAlt userLoc = location.getCoord();

        final Gps droneGps = drone.getVehicleGps();
        final LatLong droneLoc = droneGps.getPosition();

        if (userLoc == null || droneLoc == null)
            return;

        if (GeoTools.getDistance(userLoc, droneLoc) > radius) {
            double headingGCSToDrone = GeoTools.getHeadingFromCoordinates(userLoc, droneLoc);
            LatLong goCoord = GeoTools.newCoordFromBearingAndDistance(userLoc, headingGCSToDrone, radius);

            //TODO: some device (nexus 6) do not report the speed (always 0).. figure out workaround.
            double speed = location.getSpeed();
            double bearing = location.getBearing();
            double bearingInRad = Math.toRadians(bearing);
            double xVel = speed * Math.cos(bearingInRad);
            double yVel = speed * Math.sin(bearingInRad);
            drone.getGuidedPoint().newGuidedCoordAndVelocity(goCoord, xVel, yVel, 0);
        }
    }

    @Override
    public FollowModes getType() {
        return FollowModes.SPLINE_LEASH;
    }
}
