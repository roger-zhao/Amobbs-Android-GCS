package org.farring.gcs.proxy.mission.item.markers;

import android.content.res.Resources;
import android.graphics.Bitmap;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.mission.waypoints.SpatialCoordItem;

import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.maps.MarkerWithText;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * Template class and factory for a mission item's marker source.
 */
public abstract class MissionItemMarkerInfo extends MarkerInfo.SimpleMarkerInfo {

    protected final MissionItemProxy mMarkerOrigin;

    protected MissionItemMarkerInfo(MissionItemProxy origin) {
        mMarkerOrigin = origin;
    }

    public static List<MarkerInfo> newInstance(MissionItemProxy origin) {
        List<MarkerInfo> markerInfos = new ArrayList<MarkerInfo>();
        switch (origin.getMissionItem().getType()) {
            case LAND:
                markerInfos.add(new LandMarkerInfo(origin));
                break;

            case CIRCLE:
                markerInfos.add(new LoiterMarkerInfo(origin));
                break;

            case ROI:
                markerInfos.add(new ROIMarkerInfo(origin));
                break;

            case WAYPOINT:
                markerInfos.add(new WaypointMarkerInfo(origin));
                break;

            case SPLINE_WAYPOINT:
                markerInfos.add(new SplineWaypointMarkerInfo(origin));
                break;

            case CYLINDRICAL_SURVEY:
                markerInfos.add(new StructureScannerMarkerInfoProvider(origin));
                break;

            case SPLINE_SURVEY:
            case SURVEY:
                markerInfos.addAll(new SurveyMarkerInfoProvider(origin).getMarkersInfos());
                break;

            default:
                break;
        }

        return markerInfos;
    }

    public MissionItemProxy getMarkerOrigin() {
        return mMarkerOrigin;
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
        return ((SpatialCoordItem) mMarkerOrigin.getMissionItem()).getCoordinate();
    }

    @Override
    public void setPosition(LatLong coord) {
        LatLong coordinate = ((SpatialCoordItem) mMarkerOrigin.getMissionItem())
                .getCoordinate();
        coordinate.setLatitude(coord.getLatitude());
        coordinate.setLongitude(coord.getLongitude());
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public Bitmap getIcon(Resources res) {
        int drawable;
        final MissionProxy missionProxy = mMarkerOrigin.getMissionProxy();
        if (missionProxy.selection.selectionContains(mMarkerOrigin)) {
            drawable = getSelectedIconResource();
        } else {
            drawable = getIconResource();
        }

        return MarkerWithText.getMarkerWithTextAndDetail(drawable,
                Integer.toString(missionProxy.getOrder(mMarkerOrigin)), getIconDetail(), res);
    }

    private String getIconDetail() {
        try {
            final MissionProxy missionProxy = mMarkerOrigin.getMissionProxy();
            if (missionProxy.getAltitudeDiffFromPreviousItem(mMarkerOrigin) == 0) {
                return null;
            } else {
                return null; // altitude.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    protected abstract int getSelectedIconResource();

    protected abstract int getIconResource();
}
