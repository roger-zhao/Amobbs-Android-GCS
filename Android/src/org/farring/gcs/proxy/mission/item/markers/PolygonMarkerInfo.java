package org.farring.gcs.proxy.mission.item.markers;


import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.mission.survey.SurveyImpl;

import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

/**
 */
public class PolygonMarkerInfo extends MarkerInfo.SimpleMarkerInfo {

    private final MissionItemProxy markerOrigin;
    private final SurveyImpl survey;
    private final int polygonIndex;
    private LatLong mPoint;

    public PolygonMarkerInfo(LatLong point, MissionItemProxy origin, SurveyImpl mSurvey, int index) {
        this.markerOrigin = origin;
        mPoint = point;
        survey = mSurvey;
        polygonIndex = index;
    }

    public SurveyImpl getSurvey() {
        return survey;
    }

    public int getIndex() {
        return polygonIndex;
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
        return mPoint;
    }

    @Override
    public void setPosition(LatLong coord) {
        mPoint = coord;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public boolean isFlat() {
        return true;
    }

    public MissionItemProxy getMarkerOrigin() {
        return markerOrigin;
    }
}
