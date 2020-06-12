package com.dronekit.core.survey;

import com.dronekit.core.helpers.units.Area;

import java.util.Locale;

public class SurveyData {

    private CameraInfo camera = new CameraInfo();
    private double altitude;
    private double angle;
    private double overlap;
    private double sidelap;
    private double width;
    private double delay;
    private FootPrint footprint;

    public SurveyData() {
        update(0, (5.0), 0, 0, 5, 0);
    }

    public void setCamera(CameraInfo camera) {
        this.camera = camera;
    }

    public void setFootprint(FootPrint footprint) {
        this.footprint = footprint;
    }

    public void update(double angle, double altitude, double overlap, double sidelap, double width, double delay) {
        this.angle = angle;
        this.overlap = overlap;
        this.sidelap = sidelap;
        this.width = width;
        this.delay = delay;
        setAltitude(altitude);
    }

    public CameraInfo getCameraInfo() {
        return this.camera;
    }

    public void setCameraInfo(CameraInfo info) {
        this.camera = info;
        this.footprint = new FootPrint(this.camera, this.altitude);
        tryToLoadOverlapFromCamera();
    }

    private void tryToLoadOverlapFromCamera() {
        if (camera.overlap != null) {
            this.overlap = camera.overlap;
        }
        if (camera.sidelap != null) {
            this.sidelap = camera.sidelap;
        }
    }

    public double getLongitudinalPictureDistance() {
        return getLongitudinalFootPrint() * (1 - overlap * .01);
    }

    public double getLateralPictureDistance() {
        return this.width;
        // return getLateralFootPrint() * (1 - sidelap * .01);
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
        this.footprint = new FootPrint(camera, this.altitude);
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    public double getWidth() {
        return width;
    }
    public double getDelay() {
        return this.delay;
    }
    public double getSidelap() {
        return sidelap;
    }
    public void setSidelap(Double sidelap) {
        this.sidelap = sidelap;
    }
    public void setWidth(Double width) {
        this.width = width;
    }
    public void setDelay(Double delay) {
        this.delay = delay;
    }

    public double getOverlap() {
        return overlap;
    }

    public void setOverlap(Double overlap) {
        this.overlap = overlap;
    }

    public double getLateralFootPrint() {
        return footprint.getLateralSize();
    }

    public double getLongitudinalFootPrint() {
        return footprint.getLongitudinalSize();
    }

    public Area getGroundResolution() {
        return new Area(footprint.getGSD() * 0.01);
    }

    public String getCameraName() {
        return camera.name;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Altitude: %f Angle %f Overlap: %f Sidelap: %f, width: %f, delay: %f", altitude, angle, overlap, sidelap, width, delay);
    }
}