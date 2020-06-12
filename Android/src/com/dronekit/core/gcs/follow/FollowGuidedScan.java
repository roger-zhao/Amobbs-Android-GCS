package com.dronekit.core.gcs.follow;

import android.os.Handler;

import com.dronekit.core.MAVLink.command.doCmd.MavLinkDoCmds;
import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.gcs.roi.ROIEstimator;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.orhanobut.logger.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fhuya on 1/9/15.
 */
public class FollowGuidedScan extends FollowAbove {

    public static final String EXTRA_FOLLOW_ROI_TARGET = "extra_follow_roi_target";
    public static final double DEFAULT_FOLLOW_ROI_ALTITUDE = 10; //meters
    private static final long TIMEOUT = 1000; //ms
    private static final double sDefaultRoiAltitude = (DEFAULT_FOLLOW_ROI_ALTITUDE);

    public FollowGuidedScan(DroneManager droneMgr, Handler handler) {
        super(droneMgr, handler);
    }

    @Override
    public FollowModes getType() {
        return FollowModes.GUIDED_SCAN;
    }

    @Override
    public void updateAlgorithmParams(Map<String, ?> params) {
        super.updateAlgorithmParams(params);

        final LatLongAlt target;

        LatLong tempCoord = (LatLong) params.get(EXTRA_FOLLOW_ROI_TARGET);
        Logger.i("GuideScan:" + tempCoord.toString());
        if (tempCoord == null || tempCoord instanceof LatLongAlt) {
            target = (LatLongAlt) tempCoord;
        } else {
            target = new LatLongAlt(tempCoord, sDefaultRoiAltitude);
        }

        getROIEstimator().updateROITarget(target);
    }

    @Override
    protected ROIEstimator initROIEstimator(Drone drone, Handler handler) {
        return new GuidedROIEstimator(drone, handler);
    }

    @Override
    public Map<String, Object> getParams() {
        Map<String, Object> params = new HashMap<>();
        params.put(EXTRA_FOLLOW_ROI_TARGET, getROIEstimator().roiTarget);
        return params;
    }

    @Override
    protected GuidedROIEstimator getROIEstimator() {
        return (GuidedROIEstimator) super.getROIEstimator();
    }

    private static class GuidedROIEstimator extends ROIEstimator {
        private LatLongAlt roiTarget;

        public GuidedROIEstimator(Drone drone, Handler handler) {
            super(drone, handler);
        }

        void updateROITarget(LatLongAlt roiTarget) {
            this.roiTarget = roiTarget;
            onLocationUpdate(null);
        }

        @Override
        protected void updateROI() {
            if (roiTarget == null) {
                System.out.println("Cancelling ROI lock.");
                //Fallback to the default behavior
                super.updateROI();
            } else {
                Logger.d("ROI Target: " + roiTarget.toString());

                //Track the target until told otherwise.
                MavLinkDoCmds.setROI(drone, roiTarget, null);
                watchdog.postDelayed(watchdogCallback, TIMEOUT);
            }
        }
    }
}
