package com.dronekit.core.gcs.follow;

import android.os.Handler;

import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.GuidedPoint;
import com.dronekit.core.gcs.location.Location;
import com.dronekit.core.gcs.roi.ROIEstimator;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FollowAlgorithm {
    protected final DroneManager droneMgr;
    private final ROIEstimator roiEstimator;
    private final AtomicBoolean isFollowEnabled = new AtomicBoolean(false);

    public FollowAlgorithm(DroneManager droneMgr, Handler handler) {
        this.droneMgr = droneMgr;

        final Drone drone = droneMgr.getDrone();
        this.roiEstimator = initROIEstimator(drone, handler);
    }

    protected boolean isFollowEnabled() {
        return isFollowEnabled.get();
    }

    public void enableFollow() {
        isFollowEnabled.set(true);
        if (roiEstimator != null)
            roiEstimator.enableFollow();
    }

    public void disableFollow() {
        if (isFollowEnabled.compareAndSet(true, false)) {
            final Drone drone = droneMgr.getDrone();
            if (GuidedPoint.isGuidedMode(drone)) {
                drone.getGuidedPoint().pauseAtCurrentLocation(null);
            }

            if (roiEstimator != null)
                roiEstimator.disableFollow();
        }
    }

    public void updateAlgorithmParams(Map<String, ?> paramsMap) {
    }

    protected ROIEstimator initROIEstimator(Drone drone, Handler handler) {
        return new ROIEstimator(drone, handler);
    }

    protected ROIEstimator getROIEstimator() {
        return roiEstimator;
    }

    public final void onLocationReceived(Location location) {
        if (isFollowEnabled.get()) {
            if (roiEstimator != null)
                roiEstimator.onLocationUpdate(location);
            processNewLocation(location);
        }
    }

    protected abstract void processNewLocation(Location location);

    public abstract FollowModes getType();

    public Map<String, Object> getParams() {
        return Collections.emptyMap();
    }

    public enum FollowModes {
        LEASH("牵引模式(Leash)"),
        LEAD("前跟模式(Lead)"),
        RIGHT("右跟模式(Right)"),
        LEFT("左跟模式(Left)"),
        CIRCLE("绕圈跟随(Orbit)"),
        ABOVE("头顶跟随(Above)") {
            @Override
            public boolean hasParam(String paramKey) {
                return false;
            }
        },
        SPLINE_LEASH("曲线牵引(Vector Leash)"),
        SPLINE_ABOVE("曲线头顶跟随(Vector Above)"),
        GUIDED_SCAN("地面引导模式(Guided Scan)") {
            @Override
            public boolean hasParam(String paramKey) {
                switch (paramKey) {
                    case EXTRA_FOLLOW_ROI_TARGET:
                        return true;

                    default:
                        return false;
                }
            }
        },
        LOOK_AT_ME("看着我(Look At Me)");

        public static final String EXTRA_FOLLOW_RADIUS = "extra_follow_radius";
        public static final String EXTRA_FOLLOW_ROI_TARGET = "extra_follow_roi_target";
        private String name;

        FollowModes(String str) {
            name = str;
        }

        @Override
        public String toString() {
            return name;
        }

        public FollowModes next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean hasParam(String paramKey) {
            switch (paramKey) {
                case EXTRA_FOLLOW_RADIUS:
                    return true;

                case EXTRA_FOLLOW_ROI_TARGET:
                    return false;

                default:
                    return false;
            }
        }

        public FollowAlgorithm getAlgorithmType(DroneManager droneMgr) {
            final Handler handler = droneMgr.getHandler();

            switch (this) {
                case LEASH:
                default:
                    return new FollowLeash(droneMgr, handler, 8.0);
                case LEAD:
                    return new FollowLead(droneMgr, handler, 15.0);
                case RIGHT:
                    return new FollowRight(droneMgr, handler, 10.0);
                case LEFT:
                    return new FollowLeft(droneMgr, handler, 10.0);
                case CIRCLE:
                    return new FollowCircle(droneMgr, handler, 15.0, 10.0);
                case ABOVE:
                    return new FollowAbove(droneMgr, handler);
                case SPLINE_LEASH:
                    return new FollowSplineLeash(droneMgr, handler, 8.0);
                case SPLINE_ABOVE:
                    return new FollowSplineAbove(droneMgr, handler);
                case GUIDED_SCAN:
                    return new FollowGuidedScan(droneMgr, handler);
                case LOOK_AT_ME:
                    return new FollowLookAtMe(droneMgr, handler);
            }
        }
    }
}
