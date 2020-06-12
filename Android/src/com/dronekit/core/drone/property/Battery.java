package com.dronekit.core.drone.property;

public class Battery {

    private double batteryVoltage;
    private double batteryRemain;
    private double batteryCurrent;
    private Double batteryDischarge;

    public Battery() {
    }

    public Battery(double batteryVoltage, double batteryRemain, double batteryCurrent,
                   Double batteryDischarge) {
        this.batteryVoltage = batteryVoltage;
        this.batteryRemain = batteryRemain;
        this.batteryCurrent = batteryCurrent;
        this.batteryDischarge = batteryDischarge;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public double getBatteryRemain() {
        return batteryRemain;
    }

    public void setBatteryRemain(double batteryRemain) {
        this.batteryRemain = batteryRemain;
    }

    public double getBatteryCurrent() {
        return batteryCurrent;
    }

    public void setBatteryCurrent(double batteryCurrent) {
        this.batteryCurrent = batteryCurrent;
    }

    public Double getBatteryDischarge() {
        return batteryDischarge;
    }

    public void setBatteryDischarge(Double batteryDischarge) {
        this.batteryDischarge = batteryDischarge;
    }
}
