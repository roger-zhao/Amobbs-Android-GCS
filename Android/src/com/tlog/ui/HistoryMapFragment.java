package com.tlog.ui;

import android.widget.Toast;

import com.dronekit.core.helpers.coordinates.LatLong;

import org.farring.gcs.fragments.DroneMap;
import org.farring.gcs.graphic.map.GraphicLocator;
import org.farring.gcs.utils.prefs.AutoPanMode;

public class HistoryMapFragment extends DroneMap {

    private final GraphicLocator graphicLocator = new GraphicLocator();

    @Override
    protected boolean isMissionDraggable() {
        return false;
    }

    @Override
    public boolean setAutoPanMode(AutoPanMode target) {
        if (target == AutoPanMode.DISABLED)
            return true;

        Toast.makeText(getActivity(), "地图中心点自动跟随并不适用于该页面.", Toast.LENGTH_LONG).show();
        return false;
    }

    public void updateMarkerPosition(LatLong lastPosition) {
        graphicLocator.setLastPosition(lastPosition);
        mMapFragment.updateMarker(graphicLocator);
    }

    public void updateMarkerHeading(float heading) {
        graphicLocator.setHeading(heading);
        mMapFragment.updateMarker(graphicLocator);
    }
}