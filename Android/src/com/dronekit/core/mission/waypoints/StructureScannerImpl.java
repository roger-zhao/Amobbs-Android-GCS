package com.dronekit.core.mission.waypoints;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.dronekit.core.helpers.geoTools.GeoTools;
import com.dronekit.core.mission.Mission;
import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.survey.SurveyImpl;
import com.dronekit.core.polygon.Polygon;
import com.dronekit.core.survey.CameraInfo;
import com.dronekit.core.survey.SurveyData;
import com.dronekit.core.survey.grid.GridBuilder;

import java.util.ArrayList;
import java.util.List;

public class StructureScannerImpl extends SpatialCoordItem {
    SurveyData survey = new SurveyData();
    private double radius = (10.0);
    private double heightStep = (5);
    private int numberOfSteps = 2;
    private boolean crossHatch = false;

    public StructureScannerImpl(Mission mission, LatLongAlt coord) {
        super(mission, coord);
    }

    public StructureScannerImpl(MissionItemImpl item) {
        super(item);
    }

    @Override
    public List<msg_mission_item> packMissionItem() {
        List<msg_mission_item> list = new ArrayList<msg_mission_item>();
        packROI(list);
        packCircles(list);
        if (crossHatch) {
            packHatch(list);
        }
        return list;
    }

    private void packROI(List<msg_mission_item> list) {
        RegionOfInterestImpl roi = new RegionOfInterestImpl(mission, new LatLongAlt(coordinate, (0.0)));
        list.addAll(roi.packMissionItem());
    }

    private void packCircles(List<msg_mission_item> list) {
        for (double altitude = coordinate.getAltitude(); altitude <= getTopHeight(); altitude += heightStep) {
            CircleImpl circleImpl = new CircleImpl(mission, new LatLongAlt(coordinate, (altitude)));
            circleImpl.setRadius(radius);
            list.addAll(circleImpl.packMissionItem());
        }
    }

    private void packHatch(List<msg_mission_item> list) {
        Polygon polygon = new Polygon();
        for (double angle = 0; angle <= 360; angle += 10) {
            polygon.addPoint(GeoTools.newCoordFromBearingAndDistance(coordinate, angle, radius));
        }

        LatLong corner = GeoTools.newCoordFromBearingAndDistance(coordinate, -45, radius * 2);

        survey.setAltitude(getTopHeight());

        try {
            survey.update(0.0, survey.getAltitude(), survey.getOverlap(), survey.getSidelap(), survey.getWidth(), survey.getDelay());
            GridBuilder grid = new GridBuilder(polygon, survey, corner);
            for (LatLong point : grid.generate(false).gridPoints) {
                list.add(SurveyImpl.packSurveyPoint(point, getTopHeight()));
            }

            survey.update(90.0, survey.getAltitude(), survey.getOverlap(), survey.getSidelap(), survey.getWidth(), survey.getDelay());
            GridBuilder grid2 = new GridBuilder(polygon, survey, corner);
            for (LatLong point : grid2.generate(false).gridPoints) {
                list.add(SurveyImpl.packSurveyPoint(point, getTopHeight()));
            }
        } catch (Exception e) { // Should never fail, since it has good polygons
        }

    }

    public List<LatLong> getPath() {
        List<LatLong> path = new ArrayList<LatLong>();
        for (com.MAVLink.common.msg_mission_item msg_mission_item : packMissionItem()) {
            if (msg_mission_item.command == MAV_CMD.MAV_CMD_NAV_WAYPOINT) {
                path.add(new LatLong(msg_mission_item.x, msg_mission_item.y));
            }
            if (msg_mission_item.command == MAV_CMD.MAV_CMD_NAV_LOITER_TURNS) {
                for (double angle = 0; angle <= 360; angle += 12) {
                    path.add(GeoTools.newCoordFromBearingAndDistance(coordinate, angle, radius));
                }
            }
        }
        return path;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item mavMsg) {
    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.CYLINDRICAL_SURVEY;
    }

    private double getTopHeight() {
        return (coordinate.getAltitude() + (numberOfSteps - 1) * heightStep);
    }

    public double getEndAltitude() {
        return heightStep;
    }

    public int getNumberOfSteps() {
        return numberOfSteps;
    }

    public void setNumberOfSteps(int newValue) {
        numberOfSteps = newValue;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double newValue) {
        radius = newValue;
    }

    public LatLong getCenter() {
        return coordinate;
    }

    public void enableCrossHatch(boolean isEnabled) {
        crossHatch = isEnabled;
    }

    public boolean isCrossHatchEnabled() {
        return crossHatch;
    }

    public void setAltitudeStep(int newValue) {
        heightStep = (newValue);
    }

    public String getCamera() {
        return survey.getCameraName();
    }

    public void setCamera(CameraInfo cameraInfo) {
        survey.setCameraInfo(cameraInfo);
    }

    public SurveyData getSurveyData() {
        return survey;
    }
}
