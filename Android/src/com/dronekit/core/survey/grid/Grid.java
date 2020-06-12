package com.dronekit.core.survey.grid;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.geoTools.PolylineTools;

import java.util.List;

public class Grid {
    public List<LatLong> gridPoints;
    private List<LatLong> cameraLocations;

    public Grid(List<LatLong> list, List<LatLong> cameraLocations) {
        this.gridPoints = list;
        this.cameraLocations = cameraLocations;
    }

    public double getLength() {
        return PolylineTools.getPolylineLength(gridPoints);
    }

    public int getNumberOfLines() {
        return gridPoints.size() / 2;
    }

    public List<LatLong> getCameraLocations() {
        return cameraLocations;
    }

    public int getCameraCount() {
        return cameraLocations.size();
    }
}