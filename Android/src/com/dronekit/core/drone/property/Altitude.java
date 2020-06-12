package com.dronekit.core.drone.property;

public class Altitude {

    private double altitude;
    private double targetAltitude;

    public Altitude() {
    }

    public Altitude(double altitude, double targetAltitude) {
        this.altitude = altitude;
        this.targetAltitude = targetAltitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getTargetAltitude() {
        return targetAltitude;
    }

    public void setTargetAltitude(double targetAltitude) {
        this.targetAltitude = targetAltitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Altitude)) return false;

        Altitude altitude1 = (Altitude) o;

        return Double.compare(altitude1.altitude, altitude) == 0 && Double.compare(altitude1.targetAltitude, targetAltitude) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(altitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(targetAltitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Altitude{altitude=" + altitude + ", targetAltitude=" + targetAltitude + '}';
    }
}
