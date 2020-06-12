package org.farring.gcs.proxy.mission;

import android.content.Context;
import android.support.v4.util.CircularArray;
import android.util.Pair;
import android.widget.Toast;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.dronekit.core.mission.Mission;
import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.MissionCMD;
import com.dronekit.core.mission.commands.ReturnToHomeImpl;
import com.dronekit.core.mission.commands.TakeoffImpl;
import com.dronekit.core.mission.survey.SplineSurveyImpl;
import com.dronekit.core.mission.survey.SurveyImpl;
import com.dronekit.core.mission.waypoints.CircleImpl;
import com.dronekit.core.mission.waypoints.LandImpl;
import com.dronekit.core.mission.waypoints.RegionOfInterestImpl;
import com.dronekit.core.mission.waypoints.SpatialCoordItem;
import com.dronekit.core.mission.waypoints.SplineWaypointImpl;
import com.dronekit.core.mission.waypoints.StructureScannerImpl;
import com.dronekit.core.mission.waypoints.WaypointImpl;
import com.dronekit.core.survey.SurveyData;
import com.dronekit.utils.MathUtils;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.maps.DPMap.PathSource;
import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;
import org.farring.gcs.proxy.mission.item.markers.MissionItemMarkerInfo;
import org.farring.gcs.proxy.mission.item.markers.PolygonMarkerInfo;
import org.farring.gcs.proxy.mission.item.markers.SurveyMarkerInfoProvider;
import org.farring.gcs.utils.file.IO.MissionReader;
import org.farring.gcs.utils.file.IO.MissionWriter;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissionProxy implements PathSource {

    private static final int UNDO_BUFFER_SIZE = 30;

    /**
     * Stores all the mission item renders for this mission render.
     */
    private final List<MissionItemProxy> missionItemProxies = new ArrayList<>();
    private final DroidPlannerPrefs dpPrefs;
    private final CircularArray<Mission> undoBuffer = new CircularArray<>(UNDO_BUFFER_SIZE);
    private final Mission mMission;
    public MissionSelection selection = new MissionSelection();
    private Mission currentMission;
    private Context context;
    private Drone drone;

    public MissionProxy(Context context, Drone drone) {
        this.context = context;
        this.drone = drone;
        this.currentMission = generateMission();
        this.mMission = drone.getMission();

        EventBus.getDefault().register(this);
        dpPrefs = DroidPlannerPrefs.getInstance(context);
    }

    public static List<LatLong> getVisibleCoords(List<MissionItemProxy> mipList) {
        List<LatLong> coords = new ArrayList<>();

        if (mipList == null || mipList.isEmpty()) {
            return coords;
        }

        for (MissionItemProxy itemProxy : mipList) {
            MissionItemImpl item = itemProxy.getMissionItem();
            if (!(item instanceof SpatialCoordItem))
                continue;

            LatLong coordinate = ((SpatialCoordItem) item).getCoordinate();
            if (coordinate.getLatitude() == 0 || coordinate.getLongitude() == 0)
                continue;

            coords.add(coordinate);
        }

        return coords;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case MISSION_DRONIE_CREATED:
            case MISSION_RECEIVED:
                load(mMission);
                break;
        }
    }

    public void notifyMissionUpdate() {
        notifyMissionUpdate(true);
    }

    public boolean canUndoMission() {
        return !undoBuffer.isEmpty();
    }

    public void undoMission() {
        if (!canUndoMission())
            throw new IllegalStateException("Invalid state for mission undoing.");

        Mission previousMission = undoBuffer.popLast();
        load(previousMission, false);
    }

    public void notifyMissionUpdate(boolean saveMission) {
        if (saveMission && currentMission != null) {
            // Store the current state of the mission.
            undoBuffer.addLast(currentMission);
        }

        // 更新当前任务
        currentMission = generateMission();
        EventBus.getDefault().post(ActionEvent.ACTION_MISSION_PROXY_UPDATE);
    }

    public List<MissionItemProxy> getItems() {
        return missionItemProxies;
    }

    private MissionItemImpl[] getMissionItems() {
        List<MissionItemImpl> missionItems = new ArrayList<>(missionItemProxies.size());
        for (MissionItemProxy mip : missionItemProxies)
            missionItems.add(mip.getMissionItem());

        return missionItems.toArray(new MissionItemImpl[missionItems.size()]);
    }

    /**
     * @return the map markers corresponding to this mission's command set.
     */
    public List<MarkerInfo> getMarkersInfos() {
        List<MarkerInfo> markerInfos = new ArrayList<>();

        for (MissionItemProxy itemProxy : missionItemProxies) {
            List<MarkerInfo> itemMarkerInfos = itemProxy.getMarkerInfos();
            if (itemMarkerInfos != null && !itemMarkerInfos.isEmpty()) {
                markerInfos.addAll(itemMarkerInfos);
            }
        }
        return markerInfos;
    }

    /**
     * Update the state for this object based on the state of the Mission object.
     */
    public void load(Mission mission) {
        load(mission, true);
    }

    private void load(Mission mission, boolean isNew) {
        if (mission == null)
            return;

        if (isNew) {
            currentMission = null;
            clearUndoBuffer();
        }

        selection.mSelectedItems.clear();
        missionItemProxies.clear();

        for (MissionItemImpl item : mission.getItems()) {
            missionItemProxies.add(new MissionItemProxy(this, item));
        }

        selection.notifySelectionUpdate();

        notifyMissionUpdate(isNew);
    }

    private void clearUndoBuffer() {
        while (!undoBuffer.isEmpty())
            undoBuffer.popLast();
    }

    /**
     * Checks if this mission render contains the passed argument.
     *
     * @param item mission item render object
     * @return true if this mission render contains the passed argument
     */
    public boolean contains(MissionItemProxy item) {
        return missionItemProxies.contains(item);
    }

    /**
     * Removes a waypoint mission item from the set of mission items commands.
     *
     * @param item item to remove
     */
    public void removeItem(MissionItemProxy item) {
        missionItemProxies.remove(item);
        selection.mSelectedItems.remove(item);

        selection.notifySelectionUpdate();
        notifyMissionUpdate();
    }

    /**
     * Adds a survey mission item to the set.
     *
     * @param points 2D points making up the survey
     */
    public void addSurveyPolygon(List<LatLong> points, boolean spline) {
        SurveyImpl survey;
        if (spline) {
            survey = new SplineSurveyImpl(mMission, points);
        } else {
            survey = new SurveyImpl(mMission, points);
        }
        addMissionItem(survey);

        try {
            survey.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a set of waypoints generated around the passed 2D points.
     *
     * @param points list of points used to generate the mission waypoints
     */
    public void addWaypoints(List<LatLong> points) {
        double alt = getLastAltitude();
        List<MissionItemImpl> missionItemsToAdd = new ArrayList<>(points.size());
        for (LatLong point : points) {
            WaypointImpl waypoint = new WaypointImpl(mMission, new LatLongAlt(point, (float) alt));
            missionItemsToAdd.add(waypoint);
        }

        addMissionItems(missionItemsToAdd);
    }

    public double getLastAltitude() {
        if (!missionItemProxies.isEmpty()) {
            MissionItemImpl lastItem = missionItemProxies.get(missionItemProxies.size() - 1).getMissionItem();
            if (lastItem instanceof SpatialCoordItem && !(lastItem instanceof RegionOfInterestImpl)) {
                return ((SpatialCoordItem) lastItem).getCoordinate().getAltitude();
            }
        }

        return dpPrefs.getDefaultAltitude();
    }

    /**
     * Add a set of spline waypoints generated around the passed 2D points.
     *
     * @param points list of points used as location for the spline waypoints
     */
    public void addSplineWaypoints(List<LatLong> points) {
        double alt = getLastAltitude();
        List<MissionItemImpl> missionItemsToAdd = new ArrayList<MissionItemImpl>(points.size());
        for (LatLong point : points) {
            SplineWaypointImpl splineWaypoint = new SplineWaypointImpl(mMission, new LatLongAlt(point, (float) alt));
            missionItemsToAdd.add(splineWaypoint);
        }

        addMissionItems(missionItemsToAdd);
    }

    private void addMissionItems(List<MissionItemImpl> missionItems) {
        for (MissionItemImpl missionItem : missionItems) {
            missionItemProxies.add(new MissionItemProxy(this, missionItem));
        }

        notifyMissionUpdate();
    }

    public void addSpatialWaypoint(SpatialCoordItem spatialItem, LatLong point) {
        double alt = getLastAltitude();
        spatialItem.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(), alt));
        addMissionItem(spatialItem);
    }

    /**
     * Add a waypoint generated around the passed 2D point.
     *
     * @param point point used to generate the mission waypoint
     */
    public void addWaypoint(LatLong point) {
        double alt = getLastAltitude();
        WaypointImpl waypoint = new WaypointImpl(mMission, new LatLongAlt(point, alt));
        addMissionItem(waypoint);
    }

    /**
     * Add a spline waypoint generated around the passed 2D point.
     *
     * @param point point used as location for the spline waypoint.
     */
    public void addSplineWaypoint(LatLong point) {
        double alt = getLastAltitude();
        SplineWaypointImpl splineWaypoint = new SplineWaypointImpl(mMission, new LatLongAlt(point, alt));
        addMissionItem(splineWaypoint);
    }

    // 添加兴趣点到集合中
    public void addROI(LatLong point) {
        final double alt = getLastAltitude();
        RegionOfInterestImpl regionOfInterest = new RegionOfInterestImpl(mMission, new LatLongAlt(point, alt));
        addMissionItem(regionOfInterest);
    }

    // 添加着陆点到集合中
    public void addLand(LatLong point) {
        LandImpl land = new LandImpl(mMission, new LatLong(point));
        addMissionItem(land);
    }

    // 添加返航点到集合中
    public void addRTL() {
        ReturnToHomeImpl returnToHome = new ReturnToHomeImpl(mMission);
        addMissionItem(returnToHome);
    }

    // 添加绕圈点到集合中
    public void addCircle(LatLong point) {
        final double alt = getLastAltitude();
        CircleImpl circle = new CircleImpl(mMission, new LatLongAlt(point, alt));
        addMissionItem(circle);
    }

    // 添加结构扫描到集合中
    public void addStructureScan(LatLong point) {
        final double alt = getLastAltitude();
        StructureScannerImpl structureScanner = new StructureScannerImpl(mMission, new LatLongAlt(point, alt));
        addMissionItem(structureScanner);
    }

    private void addMissionItem(MissionItemImpl missionItem) {
        missionItemProxies.add(new MissionItemProxy(this, missionItem));
        notifyMissionUpdate();
    }

    private void addMissionItem(int index, MissionItemImpl missionItem) {
        missionItemProxies.add(index, new MissionItemProxy(this, missionItem));
        notifyMissionUpdate();
    }

    public void addTakeoff() {
        TakeoffImpl takeoff = new TakeoffImpl(mMission, dpPrefs.getDefaultAltitude());
        addMissionItem(takeoff);
    }

    public boolean hasTakeoffAndLandOrRTL() {
        if (missionItemProxies.size() >= 2) {
            if (isFirstItemTakeoff() && isLastItemLandOrRTL()) {
                return true;
            }
        }
        return false;
    }

    public boolean isFirstItemTakeoff() {
        return !missionItemProxies.isEmpty() && missionItemProxies.get(0).getMissionItem().getType() == MissionItemType.TAKEOFF;
    }

    public boolean isLastItemLandOrRTL() {
        int itemsCount = missionItemProxies.size();
        if (itemsCount == 0) return false;

        MissionItemType itemType = missionItemProxies.get(itemsCount - 1).getMissionItem().getType();
        return itemType == MissionItemType.RTL || itemType == MissionItemType.LAND;
    }

    public void addTakeOffAndRTL() {
        if (!isFirstItemTakeoff()) {
            double defaultAlt = dpPrefs.getDefaultAltitude();
            if (!missionItemProxies.isEmpty()) {
                MissionItemImpl firstItem = missionItemProxies.get(0).getMissionItem();
                if (firstItem instanceof SpatialCoordItem)
                    defaultAlt = ((SpatialCoordItem) firstItem).getCoordinate().getAltitude();
                else if (firstItem instanceof SurveyImpl) {
                    SurveyData surveyDetail = ((SurveyImpl) firstItem).getSurveyData();
                    if (surveyDetail != null)
                        defaultAlt = surveyDetail.getAltitude();
                }
            }

            TakeoffImpl takeOff = new TakeoffImpl(mMission, defaultAlt);
            takeOff.setFinishedAlt(defaultAlt);
            addMissionItem(0, takeOff);
        }

        if (!isLastItemLandOrRTL()) {
            ReturnToHomeImpl rtl = new ReturnToHomeImpl(mMission);
            addMissionItem(rtl);
        }
    }

    /**
     * Returns the order for the given argument in the mission set.
     *
     * @param item
     * @return order of the given argument
     */
    public int getOrder(MissionItemProxy item) {
        return missionItemProxies.indexOf(item) + 1;
    }

    /**
     * @return The order of the first waypoint.
     */
    public int getFirstWaypoint() {
        List<MarkerInfo> markerInfos = getMarkersInfos();

        if (!markerInfos.isEmpty()) {
            MarkerInfo markerInfo = markerInfos.get(0);
            if (markerInfo instanceof MissionItemMarkerInfo) {
                return getOrder(((MissionItemMarkerInfo) markerInfo).getMarkerOrigin());
            } else if (markerInfo instanceof SurveyMarkerInfoProvider) {
                return getOrder(((SurveyMarkerInfoProvider) markerInfo).getMarkerOrigin());
            } else if (markerInfo instanceof PolygonMarkerInfo) {
                return getOrder(((PolygonMarkerInfo) markerInfo).getMarkerOrigin());
            }
        }

        return 0;
    }

    /**
     * @return The order for the last waypoint.
     */
    public int getLastWaypoint() {
        List<MarkerInfo> markerInfos = getMarkersInfos();

        if (!markerInfos.isEmpty()) {
            MarkerInfo markerInfo = markerInfos.get(markerInfos.size() - 1);
            if (markerInfo instanceof MissionItemMarkerInfo) {
                return getOrder(((MissionItemMarkerInfo) markerInfo).getMarkerOrigin());
            } else if (markerInfo instanceof SurveyMarkerInfoProvider) {
                return getOrder(((SurveyMarkerInfoProvider) markerInfo).getMarkerOrigin());
            } else if (markerInfo instanceof PolygonMarkerInfo) {
                return getOrder(((PolygonMarkerInfo) markerInfo).getMarkerOrigin());
            }
        }
        return 0;
    }

    /**
     * Updates a mission item render
     *
     * @param oldItem mission item render to update
     * @param newItem new mission item render
     */
    public void replace(MissionItemProxy oldItem, MissionItemProxy newItem) {
        int index = missionItemProxies.indexOf(oldItem);
        if (index == -1)
            return;

        missionItemProxies.remove(index);
        missionItemProxies.add(index, newItem);

        if (selection.selectionContains(oldItem)) {
            selection.removeItemFromSelection(oldItem);
            selection.addToSelection(newItem);
        }

        notifyMissionUpdate();
    }

    public void replaceAll(List<Pair<MissionItemProxy, List<MissionItemProxy>>> oldNewList) {
        if (oldNewList == null) {
            return;
        }

        int pairSize = oldNewList.size();
        if (pairSize == 0) {
            return;
        }

        List<MissionItemProxy> selectionsToRemove = new ArrayList<>(pairSize);
        List<MissionItemProxy> itemsToSelect = new ArrayList<>(pairSize);

        for (int i = 0; i < pairSize; i++) {
            MissionItemProxy oldItem = oldNewList.get(i).first;
            int index = missionItemProxies.indexOf(oldItem);
            if (index == -1) {
                continue;
            }

            missionItemProxies.remove(index);

            List<MissionItemProxy> newItems = oldNewList.get(i).second;
            missionItemProxies.addAll(index, newItems);

            if (selection.selectionContains(oldItem)) {
                selectionsToRemove.add(oldItem);
                itemsToSelect.addAll(newItems);
            }
        }

        // Update the selection list.
        selection.removeItemsFromSelection(selectionsToRemove);
        selection.addToSelection(itemsToSelect);

        notifyMissionUpdate();
    }

    /**
     * Reverse the order of the mission items renders.
     */
    public void reverse() {
        Collections.reverse(missionItemProxies);
    }

    public void swap(int fromIndex, int toIndex) {
        MissionItemProxy from = missionItemProxies.get(fromIndex);
        MissionItemProxy to = missionItemProxies.get(toIndex);

        missionItemProxies.set(toIndex, from);
        missionItemProxies.set(fromIndex, to);
        notifyMissionUpdate();
    }

    public void clear() {
        selection.clearSelection();
        missionItemProxies.clear();
        notifyMissionUpdate();
    }

    // 获取高度差！（当前航点与先前航点高度的差值）
    public double getAltitudeDiffFromPreviousItem(MissionItemProxy waypointRender) {
        int itemsCount = missionItemProxies.size();
        if (itemsCount < 2)
            return 0;

        MissionItemImpl waypoint = waypointRender.getMissionItem();
        if (!(waypoint instanceof SpatialCoordItem))
            return 0;

        int index = missionItemProxies.indexOf(waypointRender);
        if (index == -1 || index == 0)
            return 0;

        MissionItemImpl previous = missionItemProxies.get(index - 1).getMissionItem();
        if (previous instanceof SpatialCoordItem) {
            return ((SpatialCoordItem) waypoint).getCoordinate().getAltitude() - ((SpatialCoordItem) previous).getCoordinate().getAltitude();
        }

        return 0;
    }

    public double getDistanceFromLastWaypoint(MissionItemProxy waypointRender) {
        if (missionItemProxies.size() < 2)
            return 0;

        MissionItemImpl waypoint = waypointRender.getMissionItem();
        if (!(waypoint instanceof SpatialCoordItem))
            return 0;

        int index = missionItemProxies.indexOf(waypointRender);
        if (index == -1 || index == 0)
            return 0;

        MissionItemImpl previous = missionItemProxies.get(index - 1).getMissionItem();
        if (previous instanceof SpatialCoordItem) {
            return MathUtils.getDistance3D(((SpatialCoordItem) waypoint).getCoordinate(), ((SpatialCoordItem) previous).getCoordinate());
        }

        return 0;
    }

    @Override
    public List<LatLong> getPathPoints() {
        if (missionItemProxies.isEmpty()) {
            return Collections.emptyList();
        }

        // Partition the mission items into spline/non-spline buckets.
        List<Pair<Boolean, List<MissionItemProxy>>> bucketsList = new ArrayList<>();

        boolean isSpline = false;
        List<MissionItemProxy> currentBucket = new ArrayList<>();
        for (MissionItemProxy missionItemProxy : missionItemProxies) {

            MissionItemImpl missionItem = missionItemProxy.getMissionItem();
            if (missionItem instanceof MissionCMD) {
                //Skip commands
                continue;
            }

            if (missionItem instanceof SplineWaypointImpl || missionItem instanceof SplineSurveyImpl) {
                if (!isSpline) {
                    if (!currentBucket.isEmpty()) {
                        // Get the last item from the current bucket. It will become the first anchor point for the spline path.
                        MissionItemProxy lastItem = currentBucket.get(currentBucket.size() - 1);

                        // Store the previous item bucket.
                        bucketsList.add(new Pair<>(Boolean.FALSE, currentBucket));

                        // Create a new bucket for this category and update 'isSpline'
                        currentBucket = new ArrayList<>();
                        currentBucket.add(lastItem);
                    }

                    isSpline = true;
                }

                // Add the current element into the bucket
                currentBucket.add(missionItemProxy);
            } else {
                if (isSpline) {
                    // Add the current item to the spline bucket. It will act as the end anchor point for the spline path.
                    if (!currentBucket.isEmpty()) {
                        currentBucket.add(missionItemProxy);

                        // Store the previous item bucket.
                        bucketsList.add(new Pair<>(Boolean.TRUE, currentBucket));

                        currentBucket = new ArrayList<>();
                    }

                    isSpline = false;
                }

                // Add the current element into the bucket
                currentBucket.add(missionItemProxy);
            }
        }

        bucketsList.add(new Pair<>(isSpline, currentBucket));

        List<LatLong> pathPoints = new ArrayList<>();
        LatLong lastPoint = null;

        for (Pair<Boolean, List<MissionItemProxy>> bucketEntry : bucketsList) {

            List<MissionItemProxy> bucket = bucketEntry.second;
            if (bucketEntry.first) {
                List<LatLong> splinePoints = new ArrayList<>();
                int bucketSize = bucket.size();
                for (int i = 0; i < bucketSize; i++) {
                    MissionItemProxy missionItemProxy = bucket.get(i);
                    MissionItemType missionItemType = missionItemProxy.getMissionItem().getType();
                    List<LatLong> missionItemPath = missionItemProxy.getPath(lastPoint);

                    switch (missionItemType) {
                        case SURVEY:
                            if (!missionItemPath.isEmpty()) {
                                if (i == 0)
                                    splinePoints.add(missionItemPath.get(0));
                                else {
                                    splinePoints.add(missionItemPath.get(missionItemPath.size() - 1));
                                }
                            }
                            break;

                        default:
                            splinePoints.addAll(missionItemPath);
                            break;
                    }

                    if (!splinePoints.isEmpty()) {
                        lastPoint = splinePoints.get(splinePoints.size() - 1);
                    }
                }
                pathPoints.addAll(MathUtils.SplinePath.process(splinePoints));
            } else {
                for (MissionItemProxy missionItemProxy : bucket) {
                    pathPoints.addAll(missionItemProxy.getPath(lastPoint));

                    if (!pathPoints.isEmpty()) {
                        lastPoint = pathPoints.get(pathPoints.size() - 1);
                    }
                }
            }
        }

        return pathPoints;
    }

    public void removeSelection(MissionSelection missionSelection) {
        missionItemProxies.removeAll(missionSelection.mSelectedItems);
        missionSelection.clearSelection();
        notifyMissionUpdate();
    }

    public void move(MissionItemProxy item, LatLong position) {
        MissionItemImpl missionItem = item.getMissionItem();
        if (missionItem instanceof SpatialCoordItem) {
            SpatialCoordItem spatialItem = (SpatialCoordItem) missionItem;
            spatialItem.setCoordinate(new LatLongAlt(position.getLatitude(), position.getLongitude(), spatialItem.getCoordinate().getAltitude()));
            notifyMissionUpdate();
        }
    }

    public List<LatLong> getVisibleCoords() {
        return getVisibleCoords(missionItemProxies);
    }

    public void movePolygonPoint(SurveyImpl survey, int index, LatLong position) {
        survey.polygon.movePoint(position, index);
        try {
            survey.build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        notifyMissionUpdate();
    }

    private Mission generateMission() {
        Mission mission = new Mission(drone);
        if (!missionItemProxies.isEmpty()) {
            for (MissionItemProxy itemProxy : missionItemProxies) {
                MissionItemImpl sourceItem = itemProxy.getMissionItem();
                mission.addMissionItem(sourceItem);
            }
        }

        return mission;
    }

    // 发送任务到APM中
    public void sendMissionToAPM() {
        // 清空mMission任务栏
        mMission.clearMissionItems();
        // 获取实际执行的任务子项，并添加到任务对象中
        for (MissionItemImpl item : getMissionItems()) {
            mMission.addMissionItem(item);
        }
        mMission.sendMissionToAPM();

        Toast.makeText(context, "正在发送航点任务，请耐心等待……", Toast.LENGTH_LONG).show();
    }

    // 获取任务长度
    public double getMissionLength() {
        List<LatLong> points = getPathPoints();
        double length = 0;
        if (points.size() > 1) {
            for (int i = 1; i < points.size(); i++) {
                length += MathUtils.getDistance2D(points.get(i - 1), points.get(i));
            }
        }

        return length;
    }

    // 调用Dronie自拍神器！
    public float makeAndUploadDronie() {
        final double bearing = mMission.makeAndUploadDronie();
        // 清空集合（选中的任务子项）
        selection.mSelectedItems.clear();
        // 清空集合（任务子项管理器）
        missionItemProxies.clear();

        // 遍历任务子项集合
        for (MissionItemImpl item : mMission.getItems()) {
            // 将任务子项添加到集合中
            missionItemProxies.add(new MissionItemProxy(this, item));
        }

        // 通知全局更新！【回调】
        selection.notifySelectionUpdate();
        return (float) bearing;
    }

    public List<List<LatLong>> getPolygonsPath() {
        ArrayList<List<LatLong>> polygonPaths = new ArrayList<>();
        for (MissionItemProxy itemProxy : missionItemProxies) {
            MissionItemImpl item = itemProxy.getMissionItem();
            if (item instanceof SurveyImpl) {
                polygonPaths.add(((SurveyImpl) item).polygon.getPoints());
            }
        }
        return polygonPaths;
    }

    // 写入航点
    public boolean writeMissionToFile(String filename) {
        return MissionWriter.write(generateMission().getMsgMissionItems(), filename);
    }

    public boolean readMissionFromFile(MissionReader reader) {
        if (reader == null)
            return false;

        // 获取Mission对象，并设置任务
        mMission.clearMissionItems();
        mMission.onMissionLoaded(reader.getMsgMissionItems());

        // 重新载入任务~
        load(mMission);
        return true;
    }
}
