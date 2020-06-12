package org.farring.gcs.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.variables.Camera;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.graphic.map.GraphicDrone;
import org.farring.gcs.graphic.map.GraphicGuided;
import org.farring.gcs.graphic.map.GraphicHome;
import org.farring.gcs.maps.DPMap;
import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.maps.providers.DPMapProvider;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.utils.prefs.AutoPanMode;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class DroneMap extends BaseFragment {
    private static final List<MarkerInfo> NO_EXTERNAL_MARKERS = Collections.emptyList();
    private final Handler mHandler = new Handler();
    private final ConcurrentLinkedQueue<MapMarkerProvider> markerProviders = new ConcurrentLinkedQueue<>();
    public GraphicDrone graphicDrone;
    public GraphicGuided guided;
    public Drone drone;
    protected DPMap mMapFragment;

    protected DroidPlannerPrefs mAppPrefs;
    protected MissionProxy missionProxy;
    protected Context context;
    private GraphicHome home;

    private final Runnable mUpdateMap = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null && mMapFragment == null)
                return;

            final List<MarkerInfo> missionMarkerInfos = missionProxy.getMarkersInfos();
            final List<MarkerInfo> externalMarkers = collectMarkersFromProviders();

            final boolean isThereMissionMarkers = !missionMarkerInfos.isEmpty();
            final boolean isThereExternalMarkers = !externalMarkers.isEmpty();
            final boolean isHomeValid = home.isValid();
            final boolean isGuidedVisible = guided.isVisible();

            // Get the list of markers currently on the map.
            final Set<MarkerInfo> markersOnTheMap = mMapFragment.getMarkerInfoList();

            if (!markersOnTheMap.isEmpty()) {
                if (isHomeValid) {
                    markersOnTheMap.remove(home);
                }

                if (isGuidedVisible) {
                    markersOnTheMap.remove(guided);
                }

                if (isThereMissionMarkers) {
                    markersOnTheMap.removeAll(missionMarkerInfos);
                }

                if (isThereExternalMarkers)
                    markersOnTheMap.removeAll(externalMarkers);

                mMapFragment.removeMarkers(markersOnTheMap);
            }

            if (isHomeValid) {
                mMapFragment.updateMarker(home);
            }

            if (isGuidedVisible) {
                mMapFragment.updateMarker(guided);
            }

            if (isThereMissionMarkers) {
                mMapFragment.updateMarkers(missionMarkerInfos, isMissionDraggable());
            }

            if (isThereExternalMarkers)
                mMapFragment.updateMarkers(externalMarkers, false);

            mMapFragment.updateMissionPath(missionProxy);

            mMapFragment.updatePolygonsPaths(missionProxy.getPolygonsPath());

            mHandler.removeCallbacks(this);
        }
    };

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case HOME_UPDATED:
                postUpdate();
                break;

            case GPS_POSITION: {
                mMapFragment.updateMarker(graphicDrone);
                mMapFragment.updateDroneLeashPath(guided);
                final Gps droneGps = drone.getVehicleGps();
                if (droneGps != null && droneGps.isValid()) {
                    mMapFragment.addFlightPathPoint(droneGps.getPosition());
                }
                break;
            }

            case GUIDED_POINT_UPDATED:
                mMapFragment.updateMarker(guided);
                mMapFragment.updateDroneLeashPath(guided);
                break;

            case HEARTBEAT_FIRST:
            case HEARTBEAT_RESTORED:
            case STATE_CONNECTED:
                mMapFragment.updateMarker(graphicDrone);
                break;

            case STATE_DISCONNECTED:
            case HEARTBEAT_TIMEOUT:
                mMapFragment.updateMarker(graphicDrone);
                break;

            case CAMERA_FOOTPRINTS_UPDATED: {
                if (mAppPrefs.isRealtimeFootprintsEnabled()) {
                    Camera camera = drone.getCamera();
                    if (camera != null && camera.getLastFootprint() != null)
                        mMapFragment.addCameraFootprint(camera.getLastFootprint());
                }
                break;
            }

            case ATTITUDE_UPDATED: {
                if (mAppPrefs.isRealtimeFootprintsEnabled()) {
                    final Gps droneGps = drone.getVehicleGps();
                    if (droneGps.isValid()) {
                        Camera camera = drone.getCamera();
                        if (camera != null && camera.getCurrentFieldOfView() != null)
                            mMapFragment.updateRealTimeFootprint(camera.getCurrentFieldOfView());
                    }
                } else {
                    mMapFragment.updateRealTimeFootprint(null);
                }
                break;
            }
        }
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_UPDATE_MAP:
            case ACTION_MISSION_PROXY_UPDATE:
                postUpdate();
                break;
        }
    }

    protected abstract boolean isMissionDraggable();

    public DPMap getMapFragment() {
        return mMapFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        final View view = inflater.inflate(R.layout.fragment_drone_map, viewGroup, false);

        if (mMapFragment != null)
            mMapFragment.clearMarkers();

        drone = getDrone();
        missionProxy = getMissionProxy();

        home = new GraphicHome(drone, getContext());
        graphicDrone = new GraphicDrone(drone);
        guided = new GraphicGuided(drone);

        postUpdate();

        updateMapFragment();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void updateMapFragment() {
        // Add the map fragment instance (based on user preference)
        final DPMapProvider mapProvider = mAppPrefs.getMapProvider();

        final FragmentManager fm = getChildFragmentManager();
        mMapFragment = (DPMap) fm.findFragmentById(R.id.map_fragment_container);

        if (mMapFragment == null || mMapFragment.getProvider() != mapProvider) {
            final Bundle mapArgs = new Bundle();
            mapArgs.putInt(DPMap.EXTRA_MAX_FLIGHT_PATH_SIZE, getMaxFlightPathSize());
            mMapFragment = mapProvider.getMapFragment();
            ((Fragment) mMapFragment).setArguments(mapArgs);
            fm.beginTransaction().replace(R.id.map_fragment_container, (Fragment) mMapFragment).commit();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapFragment.saveCameraPosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapFragment.loadCameraPosition();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMapFragment();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity.getApplicationContext();
        mAppPrefs = DroidPlannerPrefs.getInstance(context);
    }

    public final void postUpdate() {
        mHandler.post(mUpdateMap);
    }

    protected int getMaxFlightPathSize() {
        return 500;
    }

    /**
     * Adds padding around the edges of the map.
     *
     * @param left   the number of pixels of padding to be added on the left of the
     *               map.
     * @param top    the number of pixels of padding to be added on the top of the
     *               map.
     * @param right  the number of pixels of padding to be added on the right of
     *               the map.
     * @param bottom the number of pixels of padding to be added on the bottom of
     *               the map.
     */
    public void setMapPadding(int left, int top, int right, int bottom) {
        mMapFragment.setMapPadding(left, top, right, bottom);
    }

    public void saveCameraPosition() {
        mMapFragment.saveCameraPosition();
    }

    public List<LatLong> projectPathIntoMap(List<LatLong> path) {
        return mMapFragment.projectPathIntoMap(path);
    }

    /**
     * Set map panning mode on the specified target.
     *
     * @param target
     */
    public abstract boolean setAutoPanMode(AutoPanMode target);

    /**
     * Move the map to the user location.
     */
    public void goToMyLocation() {
        mMapFragment.goToMyLocation();
    }

    /**
     * Move the map to the drone location.
     */
    public void goToDroneLocation() {
        mMapFragment.goToDroneLocation();
    }

    /**
     * Update the map rotation.
     *
     * @param bearing
     */
    public void updateMapBearing(float bearing) {
        mMapFragment.updateCameraBearing(bearing);
    }

    /**
     * Ignore marker clicks on the map and instead report the event as a
     * mapClick
     *
     * @param skip if it should skip further events
     */
    public void skipMarkerClickEvents(boolean skip) {
        mMapFragment.skipMarkerClickEvents(skip);
    }

    public void addMapMarkerProvider(MapMarkerProvider provider) {
        if (provider != null) {
            markerProviders.add(provider);
            postUpdate();
        }
    }

    public void removeMapMarkerProvider(MapMarkerProvider provider) {
        if (provider != null) {
            markerProviders.remove(provider);
            postUpdate();
        }
    }

    private List<MarkerInfo> collectMarkersFromProviders() {
        if (markerProviders.isEmpty())
            return NO_EXTERNAL_MARKERS;

        List<MarkerInfo> markers = new ArrayList<>();
        for (MapMarkerProvider provider : markerProviders) {
            MarkerInfo[] externalMarkers = provider.getMapMarkers();
            Collections.addAll(markers, externalMarkers);
        }

        if (markers.isEmpty())
            return NO_EXTERNAL_MARKERS;

        return markers;
    }

    public DPMap.VisibleMapArea getVisibleMapArea() {
        return mMapFragment == null ? null : mMapFragment.getVisibleMapArea();
    }

    public interface MapMarkerProvider {
        MarkerInfo[] getMapMarkers();
    }
}
