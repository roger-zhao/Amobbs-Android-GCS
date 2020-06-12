package com.dronekit.core.drone.property;

public class Signal {

    public static final int MAX_FADE_MARGIN = 50;
    public static final int MIN_FADE_MARGIN = 6;

    private boolean isValid;
    private int rxerrors;
    private int fixed;
    private int txbuf;
    private double rssi;
    private double remrssi;
    private double noise;
    private double remnoise;
    private double signalStrength;

    public Signal() {
    }

    public Signal(boolean isValid, int rxerrors, int fixed, int txbuf, double rssi, double remrssi, double noise, double remnoise, double signalStrength) {
        this.isValid = isValid;
        this.rxerrors = rxerrors;
        this.fixed = fixed;
        this.txbuf = txbuf;
        this.rssi = rssi;
        this.remrssi = remrssi;
        this.noise = noise;
        this.remnoise = remnoise;
        this.signalStrength = signalStrength;
    }

    public double getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(double signalStrength) {
        this.signalStrength = signalStrength;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public int getRxerrors() {
        return rxerrors;
    }

    public void setRxerrors(int rxerrors) {
        this.rxerrors = rxerrors;
    }

    public int getFixed() {
        return fixed;
    }

    public void setFixed(int fixed) {
        this.fixed = fixed;
    }

    public int getTxbuf() {
        return txbuf;
    }

    public void setTxbuf(int txbuf) {
        this.txbuf = txbuf;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public double getRemrssi() {
        return remrssi;
    }

    public void setRemrssi(double remrssi) {
        this.remrssi = remrssi;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }

    public double getRemnoise() {
        return remnoise;
    }

    public void setRemnoise(double remnoise) {
        this.remnoise = remnoise;
    }

    public double getFadeMargin() {
        return rssi - noise;
    }

    public double getRemFadeMargin() {
        return remrssi - remnoise;
    }
}

