package org.farring.gcs.graphic.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.variables.GuidedPoint;
import com.dronekit.core.helpers.coordinates.LatLong;

import org.farring.gcs.R;
import org.farring.gcs.maps.DPMap.PathSource;
import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.maps.MarkerWithText;

import java.util.ArrayList;
import java.util.List;

public class GraphicGuided extends MarkerInfo.SimpleMarkerInfo implements PathSource {

    private final static String TAG = GraphicGuided.class.getSimpleName();

    private final Drone drone;

    public GraphicGuided(Drone drone) {
        this.drone = drone;
    }

    @Override
    public List<LatLong> getPathPoints() {
        List<LatLong> path = new ArrayList<LatLong>();
        GuidedPoint guidedPoint = drone.getGuidedPoint();
        if (guidedPoint != null && guidedPoint.isActive()) {
            Gps gps = drone.getVehicleGps();
            if (gps != null && gps.isValid()) {
                path.add(gps.getPosition());
            }
            path.add(guidedPoint.getCoord());
        }
        return path;
    }

    @Override
    public boolean isVisible() {
        GuidedPoint guidedPoint = drone.getGuidedPoint();
        return guidedPoint != null && guidedPoint.isActive();
    }

    @Override
    public float getAnchorU() {
        return 0.5f;
    }

    @Override
    public float getAnchorV() {
        return 0.5f;
    }

    @Override
    public LatLong getPosition() {
        GuidedPoint guidedPoint = drone.getGuidedPoint();
        return guidedPoint == null ? null : guidedPoint.getCoord();
    }

    @Override
    public void setPosition(LatLong coord) {
        try {
            drone.getGuidedPoint().forcedGuidedCoordinate(coord, null);
        } catch (Exception e) {
            Log.e(TAG, "Unable to update guided point position.", e);
        }
    }

    @Override
    public Bitmap getIcon(Resources res) {
        return MarkerWithText.getMarkerWithTextAndDetail(R.drawable.ic_wp_map, "Guided", "", res);
    }

    @Override
    public boolean isDraggable() {
        return true;
    }
}