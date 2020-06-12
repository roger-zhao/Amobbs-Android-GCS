package com.dronekit.core.gcs.location;

import com.dronekit.core.helpers.coordinates.LatLongAlt;

public class Location {

    private LatLongAlt coordinate;
    private double heading = 0.0;
    private double speed = 0.0;
    private boolean isAccurate;
    private float accuracy;


    public Location(LatLongAlt coord3d, float heading, float speed, boolean isAccurate, float accuracy) {
        coordinate = coord3d;
        this.heading = heading;
        this.speed = speed;
        this.accuracy = accuracy;
        this.isAccurate = isAccurate;
    }

    public LatLongAlt getCoord() {
        return coordinate;
    }

    public boolean isAccurate() {
        return isAccurate;
    }

    public double getBearing() {
        return heading;
    }

    public double getSpeed() {
        return speed;
    }

    public float getAccuracy() {
        return accuracy;
    }

    @Override
    public String toString() {
        return "Location{" +
                "coordinate=" + coordinate +
                ", heading=" + heading +
                ", speed=" + speed +
                ", isAccurate=" + isAccurate +
                '}';
    }

    public interface LocationReceiver {
        void onLocationUpdate(Location location);

        void onLocationUnavailable();
    }

    public interface LocationFinder {
        void enableLocationUpdates();

        void disableLocationUpdates();

        void addLocationListener(String tag, LocationReceiver receiver);

        void removeLocationListener(String tag);
    }
}
