package com.dronekit.core.drone.profiles;

public class VehicleProfile {

    private String parameterMetadataType;
    private Default default_ = new Default();

    public String getParameterMetadataType() {
        return parameterMetadataType;
    }

    public void setParameterMetadataType(String parameterMetadataType) {
        this.parameterMetadataType = parameterMetadataType;
    }

    public Default getDefault() {
        return default_;
    }

    public void setDefault(Default default_) {
        this.default_ = default_;
    }

    public static class Default {
        private int wpNavSpeed;
        private int maxAltitude;

        public int getWpNavSpeed() {
            return wpNavSpeed;
        }

        public void setWpNavSpeed(int wpNavSpeed) {
            this.wpNavSpeed = wpNavSpeed;
        }

        public int getMaxAltitude() {
            return maxAltitude;
        }

        public void setMaxAltitude(int maxAltitude) {
            this.maxAltitude = maxAltitude;
        }
    }
}
