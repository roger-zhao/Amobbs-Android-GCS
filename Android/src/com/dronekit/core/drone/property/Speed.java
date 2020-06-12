package com.dronekit.core.drone.property;

public class Speed {

    private double verticalSpeed; // m/s
    private double groundSpeed; // m/s
    private double airSpeed; // m/s

    public Speed() {
    }

    public Speed(double verticalSpeed, double groundSpeed, double airSpeed) {
        this.verticalSpeed = verticalSpeed;
        this.groundSpeed = groundSpeed;
        this.airSpeed = airSpeed;
    }

    public double getVerticalSpeed() {
        return verticalSpeed;
    }

    public void setVerticalSpeed(double verticalSpeed) {
        this.verticalSpeed = verticalSpeed;
    }

    public double getGroundSpeed() {
        return groundSpeed;
    }

    public void setGroundSpeed(double groundSpeed) {
        this.groundSpeed = groundSpeed;
    }

    public double getAirSpeed() {
        return airSpeed;
    }

    public void setAirSpeed(double airSpeed) {
        this.airSpeed = airSpeed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Speed)) return false;

        Speed speed = (Speed) o;

        if (Double.compare(speed.airSpeed, airSpeed) != 0) return false;
        if (Double.compare(speed.groundSpeed, groundSpeed) != 0) return false;
        return Double.compare(speed.verticalSpeed, verticalSpeed) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(verticalSpeed);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(groundSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(airSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Speed{" +
                "verticalSpeed=" + verticalSpeed +
                ", groundSpeed=" + groundSpeed +
                ", airSpeed=" + airSpeed +
                '}';
    }
}
