package org.farring.gcs.graphic.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.helpers.coordinates.LatLong;

import org.farring.gcs.R;
import org.farring.gcs.maps.MarkerInfo;

public class GraphicDrone extends MarkerInfo.SimpleMarkerInfo {

    private Drone drone;

    public GraphicDrone(Drone drone) {
        this.drone = drone;
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
        Gps droneGps = drone.getVehicleGps();
        return isValid() ? droneGps.getPosition() : null;
    }

    @Override
    public Bitmap getIcon(Resources res) {
        if (drone.isConnected()) {
            return BitmapFactory.decodeResource(res, R.drawable.quad);
        }
        return BitmapFactory.decodeResource(res, R.drawable.quad_disconnect);
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public boolean isFlat() {
        return true;
    }

    @Override
    public float getRotation() {
        return (float) drone.getAttitude().getYaw();
    }

    public boolean isValid() {
        return drone.getVehicleGps().isValid();
    }
}
