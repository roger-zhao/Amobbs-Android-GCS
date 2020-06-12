package com.dronekit.core.survey.grid;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.geoTools.LineLatLong;
import com.dronekit.core.polygon.Polygon;
import com.dronekit.core.survey.SurveyData;

import java.util.List;

public class GridBuilder {

    private Polygon poly;
    private Double angle;
    private Double lineDist;
    private LatLong origin;
    private Double wpDistance;

    private Grid grid;

    public GridBuilder(Polygon polygon, SurveyData surveyData, LatLong originPoint) {
        this.poly = polygon;
        this.origin = originPoint;
        this.angle = surveyData.getAngle();
        this.lineDist = surveyData.getLateralPictureDistance();
        this.wpDistance = surveyData.getLongitudinalPictureDistance();
    }

    public GridBuilder(Polygon polygon, double angle, double distance, LatLong originPoint) {
        this.poly = polygon;
        this.origin = originPoint;
        this.angle = angle;
        this.lineDist = distance;
        this.wpDistance = distance;
    }

    public void setAngle(double newAngle) {
        angle = newAngle;
    }

    public Grid generate(boolean sort) throws Exception {
        List<LatLong> polygonPoints = poly.getPoints();

        List<LineLatLong> circumscribedGrid = new CircumscribedGrid(polygonPoints, angle, lineDist).getGrid();
        List<LineLatLong> trimedGrid = new Trimmer(circumscribedGrid, poly.getLines()).getTrimmedGrid();
        EndpointSorter gridSorter = new EndpointSorter(trimedGrid, wpDistance);
        gridSorter.sortGrid(origin, sort);
        grid = new Grid(gridSorter.getSortedGrid(), gridSorter.getCameraLocations());
        return grid;
    }

}
