package com.dronekit.core.survey;

public class CameraInfo {
    public String name;
    public Double sensorWidth;
    public Double sensorHeight;
    public Double sensorResolution;
    public Double focalLength;
    public Double overlap;
    public Double sidelap;
    public boolean isInLandscapeOrientation;

    public CameraInfo() {
        name = "Canon SX260";
        sensorWidth = 6.12;
        sensorHeight = 4.22;
        sensorResolution = 12.1;
        focalLength = 5.0;
        overlap = 50.0;
        sidelap = 60.0;
        isInLandscapeOrientation = true;
    }

    public CameraInfo(String name, double sensorWidth, double sensorHeight, double sensorResolution,
                      double focalLength, double overlap, double sidelap, boolean isInLandscapeOrientation) {
        this.name = name;
        this.sensorWidth = sensorWidth;
        this.sensorHeight = sensorHeight;
        this.sensorResolution = sensorResolution;
        this.focalLength = focalLength;
        this.overlap = overlap;
        this.sidelap = sidelap;
        this.isInLandscapeOrientation = isInLandscapeOrientation;
    }

    public String getName() {
        return name;
    }

    public double getSensorWidth() {
        return sensorWidth;
    }

    public double getSensorHeight() {
        return sensorHeight;
    }

    public double getSensorResolution() {
        return sensorResolution;
    }

    public double getFocalLength() {
        return focalLength;
    }

    public double getOverlap() {
        return overlap;
    }

    public double getSidelap() {
        return sidelap;
    }

    public boolean isInLandscapeOrientation() {
        return isInLandscapeOrientation;
    }

    public Double getSensorLateralSize() {
        if (isInLandscapeOrientation) {
            return sensorWidth;
        } else {
            return sensorHeight;
        }
    }

    public Double getSensorLongitudinalSize() {
        if (isInLandscapeOrientation) {
            return sensorHeight;
        } else {
            return sensorWidth;
        }
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        temp = Double.doubleToLongBits(sensorWidth);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(sensorHeight);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(sensorResolution);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(focalLength);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(overlap);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(sidelap);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (isInLandscapeOrientation ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CameraDetail{" +
                "name='" + name + '\'' +
                ", sensorWidth=" + sensorWidth +
                ", sensorHeight=" + sensorHeight +
                ", sensorResolution=" + sensorResolution +
                ", focalLength=" + focalLength +
                ", overlap=" + overlap +
                ", sidelap=" + sidelap +
                ", isInLandscapeOrientation=" + isInLandscapeOrientation +
                '}';
    }

}