package org.farring.gcs.graphic.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dronekit.core.helpers.coordinates.LatLong;

import org.farring.gcs.R;
import org.farring.gcs.maps.MarkerInfo;

public class GraphicLocator extends MarkerInfo.SimpleMarkerInfo {

    private LatLong lastPosition;
    private float heading;

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
        return lastPosition;
    }

    @Override
    public Bitmap getIcon(Resources res) {
        return BitmapFactory.decodeResource(res, R.drawable.quad);
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
        return heading;
    }

    public void setLastPosition(LatLong lastPosition) {
        this.lastPosition = lastPosition;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }
}