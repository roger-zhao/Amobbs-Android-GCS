package org.farring.gcs.maps.providers.AMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMapLoadedListener;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.survey.FootPrint;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.R;
import org.farring.gcs.graphic.map.GraphicHome;
import org.farring.gcs.maps.DPMap;
import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.maps.providers.DPMapProvider;
import org.farring.gcs.utils.DroneMapHelper;
import org.farring.gcs.utils.collection.HashBiMap;
import org.farring.gcs.utils.prefs.AutoPanMode;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class AMapFragment extends Fragment implements DPMap {

    private static final float GO_TO_MY_LOCATION_ZOOM = 17f;

    private final HashBiMap<MarkerInfo, Marker> mBiMarkersMap = new HashBiMap<>();
    private final AtomicReference<AutoPanMode> mPanMode = new AtomicReference<>(AutoPanMode.DISABLED);
    protected boolean useMarkerClickAsMapClick = false;
    protected FishDroneGCSApp dpApp;
    private DroidPlannerPrefs mAppPrefs;
    private Marker userMarker;
    private Polyline flightPath;
    private Polyline missionPath;
    private Polyline mDroneLeashPath;
    private int maxFlightPathSize;
    /**
     * 高德地图对象
     */
    private AMap aMap;
    // 地图视图
    private MapView mapView;
    /*
     * DP Map listeners
     */
    private DPMap.OnMapClickListener mMapClickListener;
    private DPMap.OnMapLongClickListener mMapLongClickListener;
    private DPMap.OnMarkerClickListener mMarkerClickListener;
    private DPMap.OnMarkerDragListener mMarkerDragListener;
    private LocationListener mLocationListener;
    private List<Polygon> polygonsPaths = new ArrayList<>();
    private Polygon footprintPoly;
    private AMapLocation aMapLocation;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dpApp = (FishDroneGCSApp) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        setHasOptionsMenu(true);
        final FragmentActivity activity = getActivity();
        final Context context = activity.getApplicationContext();

        mAppPrefs = DroidPlannerPrefs.getInstance(context);

        final Bundle args = getArguments();
        if (args != null) {
            maxFlightPathSize = args.getInt(EXTRA_MAX_FLIGHT_PATH_SIZE);
        }

        if (mapView == null) {
            mapView = (MapView) inflater.inflate(R.layout.fragment_amap, null);
        }

        mapView.onCreate(bundle);
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        aMap.setOnMapLoadedListener(new OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                aMap.showMapText(false);
            }
        });
        return mapView;
    }

    @Override
    public void onStart() {
        super.onStart();
        setupMap();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case GPS_POSITION:
                if (mPanMode.get() == AutoPanMode.DRONE) {
                    final Drone drone = getDrone();
                    if (!drone.isConnected())
                        return;

                    final Gps droneGps = drone.getVehicleGps();
                    if (droneGps != null && droneGps.isValid()) {
                        final LatLong droneLocation = droneGps.getPosition();
                        updateCamera(droneLocation);
                    }
                }
                break;
        }
    }

    public void setupMap() {
        setupMapUI();
        setupMapListeners();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void clearFlightPath() {
        if (flightPath != null) {
            List<LatLng> oldFlightPath = flightPath.getPoints();
            oldFlightPath.clear();
            flightPath.setPoints(oldFlightPath);
        }
    }

    @Override
    public LatLong getMapCenter() {
        return DroneMapHelper.GaodeLatLngToCoord(aMap.getCameraPosition().target);
    }

    @Override
    public float getMapZoomLevel() {
        return aMap.getCameraPosition().zoom;
    }

    @Override
    public float getMaxZoomLevel() {
        return aMap.getMaxZoomLevel();
    }

    @Override
    public float getMinZoomLevel() {
        return aMap.getMinZoomLevel();
    }

    @Override
    public void selectAutoPanMode(AutoPanMode target) {
        final AutoPanMode currentMode = mPanMode.get();
        if (currentMode == target)
            return;

        setAutoPanMode(currentMode, target);
    }

    private Drone getDrone() {
        return dpApp.getDrone();
    }

    private void setAutoPanMode(AutoPanMode current, AutoPanMode update) {
        mPanMode.compareAndSet(current, update);
    }

    @Override
    public DPMapProvider getProvider() {
        return DPMapProvider.高德地图;
    }

    @Override
    public void addFlightPathPoint(LatLong coord) {
        final LatLng position = DroneMapHelper.CoordToGaodeLatLang(coord);

        if (maxFlightPathSize > 0) {
            if (flightPath == null) {
                PolylineOptions flightPathOptions = new PolylineOptions();
                flightPathOptions.color(FLIGHT_PATH_DEFAULT_COLOR).width(FLIGHT_PATH_DEFAULT_WIDTH).zIndex(1);
                flightPath = aMap.addPolyline(flightPathOptions);
            }

            List<LatLng> oldFlightPath = flightPath.getPoints();
            if (oldFlightPath.size() > maxFlightPathSize) {
                oldFlightPath.remove(0);
            }
            oldFlightPath.add(position);
            flightPath.setPoints(oldFlightPath);
        }
    }

    @Override
    public void clearMarkers() {
        for (Marker marker : mBiMarkersMap.valueSet()) {
            marker.remove();
        }

        mBiMarkersMap.clear();
    }

    @Override
    public void updateMarker(MarkerInfo markerInfo) {
        updateMarker(markerInfo, markerInfo.isDraggable());
    }

    @Override
    public void updateMarker(MarkerInfo markerInfo, boolean isDraggable) {
        // if the drone hasn't received a gps signal yet
        final LatLong coord = markerInfo.getPosition();
        if (coord == null) {
            return;
        }

        final LatLng position = DroneMapHelper.CoordToGaodeLatLang(coord);
        Marker marker = mBiMarkersMap.getValue(markerInfo);
        if (marker == null) {
            // Generate the marker
            generateMarker(markerInfo, position, isDraggable);
        } else {
            // Update the marker
            updateMarker(marker, markerInfo, position, isDraggable);
        }
    }

    private void generateMarker(MarkerInfo markerInfo, LatLng position, boolean isDraggable) {
        final MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .draggable(isDraggable)
                //.alpha(markerInfo.getAlpha())
                .anchor(markerInfo.getAnchorU(), markerInfo.getAnchorV())
                .setInfoWindowOffset((int) markerInfo.getInfoWindowAnchorU(), (int) markerInfo.getInfoWindowAnchorV())
                //.rotation(markerInfo.getRotation())
                .snippet(markerInfo.getSnippet()).title(markerInfo.getTitle())
                .setFlat(markerInfo.isFlat())
                .visible(markerInfo.isVisible());

        final Bitmap markerIcon = markerInfo.getIcon(getResources());
        if (markerIcon != null) {
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerIcon));
        }

        Marker marker = aMap.addMarker(markerOptions);
        mBiMarkersMap.put(markerInfo, marker);
    }

    private void updateMarker(Marker marker, MarkerInfo markerInfo, LatLng position, boolean isDraggable) {
        final Bitmap markerIcon = markerInfo.getIcon(getResources());
        if (markerIcon != null) {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(markerIcon));
        }

        // marker.setAlpha(markerInfo.getAlpha());
        marker.setAnchor(markerInfo.getAnchorU(), markerInfo.getAnchorV());
        // marker.setInfoWindowOffset(markerInfo.getInfoWindowAnchorU(),markerInfo.getInfoWindowAnchorV());
        marker.setPosition(position);
        marker.setRotateAngle(-markerInfo.getRotation());
        marker.setSnippet(markerInfo.getSnippet());
        marker.setTitle(markerInfo.getTitle());
        marker.setDraggable(isDraggable);
        marker.setFlat(markerInfo.isFlat());
        marker.setVisible(markerInfo.isVisible());
    }

    @Override
    public void updateMarkers(List<MarkerInfo> markersInfos) {
        for (MarkerInfo info : markersInfos) {
            updateMarker(info);
        }
    }

    @Override
    public void updateMarkers(List<MarkerInfo> markersInfos, boolean isDraggable) {
        for (MarkerInfo info : markersInfos) {
            updateMarker(info, isDraggable);
        }
    }

    @Override
    public Set<MarkerInfo> getMarkerInfoList() {
        return new HashSet<>(mBiMarkersMap.keySet());
    }

    @Override
    public List<LatLong> projectPathIntoMap(List<LatLong> path) {
        List<LatLong> coords = new ArrayList<>();
        Projection projection = aMap.getProjection();

        for (LatLong point : path) {
            LatLng coord = projection.fromScreenLocation(new Point((int) point.getLatitude(), (int) point.getLongitude()));
            coords.add(DroneMapHelper.GaodeLatLngToCoord(coord));
        }

        return coords;
    }

    @Override
    public void removeMarkers(Collection<MarkerInfo> markerInfoList) {
        if (markerInfoList == null || markerInfoList.isEmpty()) {
            return;
        }

        for (MarkerInfo markerInfo : markerInfoList) {
            Marker marker = mBiMarkersMap.getValue(markerInfo);
            if (marker != null) {
                marker.remove();
                mBiMarkersMap.removeKey(markerInfo);
            }
        }
    }

    @Override
    public void setMapPadding(int left, int top, int right, int bottom) {
        // getMap().setPadding(left, top, right, bottom);
    }

    @Override
    public void setOnMapClickListener(OnMapClickListener listener) {
        mMapClickListener = listener;
    }

    @Override
    public void setOnMapLongClickListener(OnMapLongClickListener listener) {
        mMapLongClickListener = listener;
    }

    @Override
    public void setOnMarkerDragListener(OnMarkerDragListener listener) {
        mMarkerDragListener = listener;
    }

    @Override
    public void setOnMarkerClickListener(OnMarkerClickListener listener) {
        mMarkerClickListener = listener;
    }

    @Override
    public void setLocationListener(LocationListener receiver) {
        mLocationListener = receiver;
    }

    private void updateCamera(final LatLong coord) {
        if (coord != null) {
            final float zoomLevel = aMap.getCameraPosition().zoom;
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DroneMapHelper.CoordToGaodeLatLang(coord), zoomLevel));
        }
    }

    @Override
    public void updateCamera(final LatLong coord, final float zoomLevel) {
        if (coord != null) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DroneMapHelper.CoordToGaodeLatLang(coord), zoomLevel));
        }
    }

    @Override
    public void updateCameraBearing(float bearing) {
        final CameraPosition cameraPosition = new CameraPosition(DroneMapHelper.CoordToGaodeLatLang(getMapCenter()), getMapZoomLevel(), 0, bearing);
        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void updateDroneLeashPath(PathSource pathSource) {
        List<LatLong> pathCoords = pathSource.getPathPoints();
        final List<LatLng> pathPoints = new ArrayList<>(pathCoords.size());
        for (LatLong coord : pathCoords) {
            pathPoints.add(DroneMapHelper.CoordToGaodeLatLang(coord));
        }

        if (mDroneLeashPath == null) {
            final PolylineOptions flightPath = new PolylineOptions();
            flightPath.color(DRONE_LEASH_DEFAULT_COLOR).width(DroneMapHelper.scaleDpToPixels(DRONE_LEASH_DEFAULT_WIDTH, getResources()));

            mDroneLeashPath = aMap.addPolyline(flightPath);
            mDroneLeashPath.setPoints(pathPoints);
        } else {
            mDroneLeashPath.setPoints(pathPoints);
        }
    }

    @Override
    public void updateMissionPath(PathSource pathSource) {
        List<LatLong> pathCoords = pathSource.getPathPoints();
        final List<LatLng> pathPoints = new ArrayList<>(pathCoords.size());
        for (LatLong coord : pathCoords) {
            pathPoints.add(DroneMapHelper.CoordToGaodeLatLang(coord));
        }

        if (missionPath == null) {
            final PolylineOptions pathOptions = new PolylineOptions();
            pathOptions.color(MISSION_PATH_DEFAULT_COLOR).width(MISSION_PATH_DEFAULT_WIDTH);

            missionPath = aMap.addPolyline(pathOptions);
            missionPath.setPoints(pathPoints);
        } else {
            missionPath.setPoints(pathPoints);
        }
    }

    @Override
    public void updatePolygonsPaths(List<List<LatLong>> paths) {
        for (Polygon poly : polygonsPaths) {
            poly.remove();
        }

        for (List<LatLong> contour : paths) {
            PolygonOptions pathOptions = new PolygonOptions();
            pathOptions.strokeColor(POLYGONS_PATH_DEFAULT_COLOR).strokeWidth(POLYGONS_PATH_DEFAULT_WIDTH).zIndex(-2).fillColor(0);
            final List<LatLng> pathPoints = new ArrayList<>(contour.size());
            for (LatLong coord : contour) {
                pathPoints.add(DroneMapHelper.CoordToGaodeLatLang(coord));
            }
            pathOptions.addAll(pathPoints);
            polygonsPaths.add(aMap.addPolygon(pathOptions));
        }
    }

    @Override
    public void addCameraFootprint(FootPrint footprintToBeDraw) {
        PolygonOptions pathOptions = new PolygonOptions();
        pathOptions.strokeColor(FOOTPRINT_DEFAULT_COLOR).strokeWidth(FOOTPRINT_DEFAULT_WIDTH).fillColor(FOOTPRINT_FILL_COLOR);

        for (LatLong vertex : footprintToBeDraw.getVertexInGlobalFrame()) {
            pathOptions.add(DroneMapHelper.CoordToGaodeLatLang(vertex));
        }
        aMap.addPolygon(pathOptions);
    }

    /**
     * Save the map camera state on a preference file http://stackoverflow.com/questions /16697891/google-maps-android-api-v2-restoring -map-state/16698624#16698624
     */
    @Override
    public void saveCameraPosition() {
        final AMap aMap = mapView.getMap();
        if (aMap == null)
            return;

        CameraPosition camera = aMap.getCameraPosition();
        mAppPrefs.prefs.edit()
                .putFloat(PREF_LAT, (float) camera.target.latitude)
                .putFloat(PREF_LNG, (float) camera.target.longitude)
                .putFloat(PREF_BEA, camera.bearing)
                .putFloat(PREF_TILT, camera.tilt)
                .putFloat(PREF_ZOOM, camera.zoom).apply();
    }

    @Override
    public void loadCameraPosition() {
        final SharedPreferences settings = mAppPrefs.prefs;

        final CameraPosition.Builder camera = new CameraPosition.Builder();
        camera.bearing(settings.getFloat(PREF_BEA, DEFAULT_BEARING));
        camera.tilt(settings.getFloat(PREF_TILT, DEFAULT_TILT));
        camera.zoom(settings.getFloat(PREF_ZOOM, DEFAULT_ZOOM_LEVEL));
        camera.target(new LatLng(settings.getFloat(PREF_LAT, DEFAULT_LATITUDE), settings.getFloat(PREF_LNG, DEFAULT_LONGITUDE)));

        aMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera.build()));
    }

    @Override
    public void zoomToFit(List<LatLong> coords) {
        if (!coords.isEmpty()) {
            final List<LatLng> points = new ArrayList<LatLng>();
            for (LatLong coord : coords)
                points.add(DroneMapHelper.CoordToGaodeLatLang(coord));

            final LatLngBounds bounds = getBounds(points);

            final Activity activity = getActivity();
            if (activity == null)
                return;

            final View rootView = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            if (rootView == null)
                return;

            final int height = rootView.getHeight();
            final int width = rootView.getWidth();
            if (height > 0 && width > 0) {
                CameraUpdate animation = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 100);
                aMap.animateCamera(animation);
            }
        }
    }

    @Override
    public void zoomToFitMyLocation(final List<LatLong> coords) {
        if (aMapLocation != null) {
            final List<LatLong> updatedCoords = new ArrayList<LatLong>(coords);
            updatedCoords.add(new LatLong(aMapLocation.getLatitude(), aMapLocation.getLongitude()));
            zoomToFit(updatedCoords);
        } else {
            zoomToFit(coords);
        }
    }

    @Override
    public void goToMyLocation() {
        if (aMapLocation != null) {
            updateCamera(DroneMapHelper.AMapLocationToCoord(aMapLocation), GO_TO_MY_LOCATION_ZOOM);

            if (mLocationListener != null)
                mLocationListener.onLocationChanged(aMapLocation);
        }
    }

    @Override
    public void goToDroneLocation() {
        Drone drone = getDrone();
        if (!drone.isConnected())
            return;

        Gps gps = drone.getVehicleGps();
        if (!gps.isValid()) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.drone_no_location, Toast.LENGTH_SHORT).show();
            return;
        }

        final float currentZoomLevel = aMap.getCameraPosition().zoom;
        final LatLong droneLocation = gps.getPosition();
        updateCamera(droneLocation, (int) currentZoomLevel);
    }

    private void setupMapListeners() {
        final AMap.OnMapClickListener onMapClickListener = new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mMapClickListener != null) {
                    mMapClickListener.onMapClick(DroneMapHelper.GaodeLatLngToCoord(latLng));
                }
            }
        };
        aMap.setOnMapClickListener(onMapClickListener);

        aMap.setOnMapLongClickListener(new AMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (mMapLongClickListener != null) {
                    mMapLongClickListener.onMapLongClick(DroneMapHelper.GaodeLatLngToCoord(latLng));
                }
            }
        });

        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if (!(markerInfo instanceof GraphicHome)) {
                        markerInfo.setPosition(DroneMapHelper.GaodeLatLngToCoord(marker.getPosition()));
                        mMarkerDragListener.onMarkerDragStart(markerInfo);
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if (!(markerInfo instanceof GraphicHome)) {
                        markerInfo.setPosition(DroneMapHelper.GaodeLatLngToCoord(marker.getPosition()));
                        mMarkerDragListener.onMarkerDrag(markerInfo);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    markerInfo.setPosition(DroneMapHelper.GaodeLatLngToCoord(marker.getPosition()));
                    mMarkerDragListener.onMarkerDragEnd(markerInfo);
                }
            }
        });

        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (useMarkerClickAsMapClick) {
                    onMapClickListener.onMapClick(marker.getPosition());
                    return true;
                }

                if (mMarkerClickListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if (markerInfo != null)
                        return mMarkerClickListener.onMarkerClick(markerInfo);
                }
                return false;
            }
        });
    }

    private void setupMapUI() {
        aMap.setMyLocationEnabled(false);
        aMap.setMapType(AMapPrefFragment.PrefManager.getMapType(getContext()));
        UiSettings mUiSettings = aMap.getUiSettings();
        mUiSettings.setMyLocationButtonEnabled(false);
        mUiSettings.setScaleControlsEnabled(true);
        mUiSettings.setCompassEnabled(false);
        mUiSettings.setTiltGesturesEnabled(false);
        mUiSettings.setZoomControlsEnabled(false);
        mUiSettings.setRotateGesturesEnabled(mAppPrefs.isMapRotationEnabled());
    }

    private LatLngBounds getBounds(List<LatLng> pointsList) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : pointsList) {
            builder.include(point);
        }
        return builder.build();
    }

    @Override
    public void skipMarkerClickEvents(boolean skip) {
        useMarkerClickAsMapClick = skip;
    }

    @Override
    public void updateRealTimeFootprint(FootPrint footprint) {
        List<LatLong> pathPoints = footprint == null
                ? Collections.<LatLong>emptyList()
                : footprint.getVertexInGlobalFrame();

        if (pathPoints.isEmpty()) {
            if (footprintPoly != null) {
                footprintPoly.remove();
                footprintPoly = null;
            }
        } else {
            if (footprintPoly == null) {
                PolygonOptions pathOptions = new PolygonOptions()
                        .strokeColor(FOOTPRINT_DEFAULT_COLOR)
                        .strokeWidth(FOOTPRINT_DEFAULT_WIDTH)
                        .fillColor(FOOTPRINT_FILL_COLOR);

                for (LatLong vertex : pathPoints) {
                    pathOptions.add(DroneMapHelper.CoordToGaodeLatLang(vertex));
                }
                footprintPoly = aMap.addPolygon(pathOptions);
            } else {
                List<LatLng> list = new ArrayList<LatLng>();
                for (LatLong vertex : pathPoints) {
                    list.add(DroneMapHelper.CoordToGaodeLatLang(vertex));
                }
                footprintPoly.setPoints(list);
            }
        }
    }

    @Override
    public VisibleMapArea getVisibleMapArea() {
        return null;
    }

    @Subscribe
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null)
            return;

        this.aMapLocation = aMapLocation;
        LatLng latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
        // Update the user location icon.
        if (userMarker == null) {
            final MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .draggable(false)
                    .visible(true)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location));
            userMarker = aMap.addMarker(options);
        } else {
            userMarker.setPosition(latLng);
        }

        if (mPanMode.get() == AutoPanMode.USER) {
            updateCamera(DroneMapHelper.AMapLocationToCoord(aMapLocation), (int) aMap.getCameraPosition().zoom);
        }

        if (mLocationListener != null) {
            mLocationListener.onLocationChanged(aMapLocation);
        }
    }
}
