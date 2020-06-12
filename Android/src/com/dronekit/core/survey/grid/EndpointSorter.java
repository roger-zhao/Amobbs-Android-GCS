package com.dronekit.core.survey.grid;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.geoTools.LineLatLong;
import com.dronekit.core.helpers.geoTools.LineSampler;
import com.dronekit.core.helpers.geoTools.LineTools;

import java.util.ArrayList;
import java.util.List;

public class EndpointSorter {
    private static final int MAX_NUMBER_OF_CAMERAS = 2000;

    private List<LatLong> gridPoints = new ArrayList<LatLong>();
    private List<LineLatLong> grid;
    private Double sampleDistance;
    private List<LatLong> cameraLocations = new ArrayList<LatLong>();

    public EndpointSorter(List<LineLatLong> grid, Double sampleDistance) {
        this.grid = grid;
        this.sampleDistance = sampleDistance;
    }

    public void sortGrid(LatLong lastpnt, boolean sort) throws Exception {
        while (grid.size() > 0) {
            if (sort) {
                LineLatLong closestLine = LineTools.findClosestLineToPoint(lastpnt, grid);
                LatLong secondWp = processOneGridLine(closestLine, lastpnt, sort);
                lastpnt = secondWp;
            } else {
                LineLatLong closestLine = grid.get(0);
                LatLong secondWp = processOneGridLine(closestLine, lastpnt, sort);
                lastpnt = secondWp;
            }
        }
    }

    private LatLong processOneGridLine(LineLatLong closestLine, LatLong lastpnt, boolean sort)
            throws Exception {
        LatLong firstWP, secondWp;
        firstWP = closestLine.getClosestEndpointTo(lastpnt);
        secondWp = closestLine.getFarthestEndpointTo(lastpnt);

        grid.remove(closestLine);

        // DB ZhaoYJ@2018-03-20 for tmply, no camera need for farming now
        // updateCameraLocations(firstWP, secondWp);
        gridPoints.add(firstWP);
        gridPoints.add(secondWp);

        // DB ZhaoYJ@2018-03-20 for tmply, no camera need for farming now
        /*
        if (cameraLocations.size() > MAX_NUMBER_OF_CAMERAS) {
            throw new Exception("Too many camera positions");
        }*/
        return secondWp;
    }

    private void updateCameraLocations(LatLong firstWP, LatLong secondWp) {
        List<LatLong> cameraLocationsOnThisStrip = new LineSampler(firstWP, secondWp)
                .sample(sampleDistance);
        cameraLocations.addAll(cameraLocationsOnThisStrip);
    }

    public List<LatLong> getSortedGrid() {
        return gridPoints;
    }

    public List<LatLong> getCameraLocations() {
        return cameraLocations;
    }
}
