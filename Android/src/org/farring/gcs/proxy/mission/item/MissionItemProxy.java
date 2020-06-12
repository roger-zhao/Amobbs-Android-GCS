package org.farring.gcs.proxy.mission.item;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.survey.SplineSurveyImpl;
import com.dronekit.core.mission.survey.SurveyImpl;
import com.dronekit.core.mission.waypoints.CircleImpl;
import com.dronekit.core.mission.waypoints.SpatialCoordItem;
import com.dronekit.core.mission.waypoints.StructureScannerImpl;
import com.dronekit.core.survey.grid.Grid;
import com.dronekit.utils.MathUtils;

import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.proxy.mission.item.fragments.MissionDetailFragment;
import org.farring.gcs.proxy.mission.item.markers.MissionItemMarkerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for providing logic to access and interpret the class on the Android layer, as well as providing methods for rendering it on the Android UI.
 */
public class MissionItemProxy {

    /**
     * This is the mission item object this class is built around.
     */
    private final MissionItemImpl mMissionItem;
    /**
     * This is the mission render to which this item belongs.
     */
    private final MissionProxy mMission;

    /**
     * This is the marker source for this mission item render.
     */
    private final List<MarkerInfo> mMarkerInfos;

    /**
     * Used by the mission item list adapter to provide drag and drop support.
     */
    private final long stableId;

    public MissionItemProxy(MissionProxy mission, MissionItemImpl missionItem) {
        this.stableId = System.nanoTime();

        mMission = mission;
        mMissionItem = missionItem;
        mMarkerInfos = MissionItemMarkerInfo.newInstance(this);

        if (mMissionItem instanceof SplineSurveyImpl) {
            // mMission.getDrone().buildMissionItemsAsync(new SplineSurveyImpl[]{(SplineSurveyImpl) mMissionItem}, missionItemBuiltListener);
        } else if (mMissionItem instanceof SurveyImpl) {
            // mMission.getDrone().buildMissionItemsAsync(new SurveyImpl[]{(SurveyImpl) mMissionItem}, missionItemBuiltListener);
        } else if (mMissionItem instanceof StructureScannerImpl) {
            // mMission.getDrone().buildMissionItemsAsync(new StructureScannerImpl[]{(StructureScannerImpl) mMissionItem}, missionItemBuiltListener);
        }
    }

    /**
     * Provides access to the owning mission render instance.
     *
     * @return
     */
    public MissionProxy getMissionProxy() {
        return mMission;
    }

    public MissionProxy getMission() {
        return mMission;
    }

    /**
     * Provides access to the mission item instance.
     */
    public MissionItemImpl getMissionItem() {
        return mMissionItem;
    }

    public MissionDetailFragment getDetailFragment() {
        return MissionDetailFragment.newInstance(mMissionItem.getType());
    }

    public List<MarkerInfo> getMarkerInfos() {
        return mMarkerInfos;
    }

    /**
     * @param previousPoint Previous point on the path, null if there wasn't a previous
     *                      point
     * @return the set of points/coords making up this mission item.
     */
    public List<LatLong> getPath(LatLong previousPoint) {
        List<LatLong> pathPoints = new ArrayList<LatLong>();
        switch (mMissionItem.getType()) {
            case LAND:
            case WAYPOINT:
            case SPLINE_WAYPOINT:
                pathPoints.add(((SpatialCoordItem) mMissionItem).getCoordinate());
                break;

            case CIRCLE:
                for (int i = 0; i <= 360; i += 10) {
                    CircleImpl circle = (CircleImpl) mMissionItem;
                    double startHeading = 0;
                    if (previousPoint != null) {
                        startHeading = MathUtils.getHeadingFromCoordinates(circle.getCoordinate(),
                                previousPoint);
                    }
                    pathPoints.add(MathUtils.newCoordFromBearingAndDistance(circle.getCoordinate(), startHeading + i, circle.getRadius()));
                }
                break;

            case SPLINE_SURVEY:
            case SURVEY:
                Grid grid = ((SurveyImpl) mMissionItem).grid;
                if (grid != null) {
                    pathPoints.addAll(grid.gridPoints);
                }

                break;

            case CYLINDRICAL_SURVEY:
                StructureScannerImpl survey = (StructureScannerImpl) mMissionItem;
                pathPoints.addAll(survey.getPath());
                break;

            default:
                break;
        }

        return pathPoints;
    }

    /**
     * @return stable id used by the recycler view adapter to provide drag and drop support.
     */
    public long getStableId() {
        return stableId;
    }
}
