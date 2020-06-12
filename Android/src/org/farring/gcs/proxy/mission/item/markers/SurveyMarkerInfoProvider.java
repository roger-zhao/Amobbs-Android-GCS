package org.farring.gcs.proxy.mission.item.markers;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.mission.survey.SurveyImpl;

import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SurveyMarkerInfoProvider {

    protected final MissionItemProxy markerOrigin;
    private final SurveyImpl mSurvey;
    private final List<MarkerInfo> mPolygonMarkers = new ArrayList<>();

    protected SurveyMarkerInfoProvider(MissionItemProxy origin) {
        this.markerOrigin = origin;
        mSurvey = (SurveyImpl) origin.getMissionItem();
        updateMarkerInfoList();
    }

    private void updateMarkerInfoList() {
        List<LatLong> points = mSurvey.polygon.getPoints();
        if (points != null) {
            final int pointsCount = points.size();
            for (int i = 0; i < pointsCount; i++) {
                mPolygonMarkers.add(new PolygonMarkerInfo(points.get(i), markerOrigin, mSurvey, i));
            }
        }
    }

    public List<MarkerInfo> getMarkersInfos() {
        return mPolygonMarkers;
    }

    public MissionItemProxy getMarkerOrigin() {
        return markerOrigin;
    }
}
