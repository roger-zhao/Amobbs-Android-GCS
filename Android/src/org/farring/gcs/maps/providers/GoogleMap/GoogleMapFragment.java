package org.farring.gcs.maps.providers.GoogleMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.survey.FootPrint;
import com.evenbus.AttributeEvent;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.VisibleRegion;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.R;
import org.farring.gcs.graphic.map.GraphicHome;
import org.farring.gcs.maps.DPMap;
import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.maps.providers.DPMapProvider;
import org.farring.gcs.maps.providers.GoogleMap.offline.Tiles.OfflineTileProvider;
import org.farring.gcs.maps.providers.GoogleMap.offline.Tiles.OnlineTileProvider;
import org.farring.gcs.maps.providers.GoogleMap.offline.UI.GoogleMapPrefFragment;
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

public class GoogleMapFragment extends SupportMapFragment implements DPMap {
    private static final float GO_TO_MY_LOCATION_ZOOM = 17f;

    private static final int ONLINE_TILE_PROVIDER_Z_INDEX = -1;
    private static final int OFFLINE_TILE_PROVIDER_Z_INDEX = -2;

    private final HashBiMap<MarkerInfo, Marker> mBiMarkersMap = new HashBiMap<>();
    private final AtomicReference<AutoPanMode> mPanMode = new AtomicReference<>(AutoPanMode.DISABLED);
    protected boolean useMarkerClickAsMapClick = false;
    protected FishDroneGCSApp dpApp;
    private DroidPlannerPrefs mAppPrefs;

    private final OnMapReadyCallback loadCameraPositionTask = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            final SharedPreferences settings = mAppPrefs.prefs;

            final CameraPosition.Builder camera = new CameraPosition.Builder();
            camera.bearing(settings.getFloat(PREF_BEA, DEFAULT_BEARING));
            camera.tilt(settings.getFloat(PREF_TILT, DEFAULT_TILT));
            camera.zoom(settings.getFloat(PREF_ZOOM, DEFAULT_ZOOM_LEVEL));
            camera.target(new LatLng(settings.getFloat(PREF_LAT, DEFAULT_LATITUDE), settings.getFloat(PREF_LNG, DEFAULT_LONGITUDE)));

            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera.build()));
        }
    };

    private Marker userMarker;
    private Polyline flightPath;
    private Polyline missionPath;
    private Polyline mDroneLeashPath;
    private int maxFlightPathSize;
    /*
     * DP Map listeners
     */
    private OnMapClickListener mMapClickListener;
    private OnMapLongClickListener mMapLongClickListener;
    private OnMarkerClickListener mMarkerClickListener;
    private OnMarkerDragListener mMarkerDragListener;
    private List<Polygon> polygonsPaths = new ArrayList<>();
    private Polygon footprintPoly;
    private AMapLocation aMapLocation;
    private LocationListener mLocationListener;
    /**
     * 谷歌地图对象
     */
    private GoogleMap googleMap;

    /*
    Tile overlay
    */
    private TileOverlay onlineTileProvider;
    private TileOverlay offlineTileProvider;
    private final OnMapReadyCallback setupMapTask = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            setupMapUI(googleMap);
            setupMapOverlay(googleMap);
            setupMapListeners(googleMap);
        }
    };

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

        final View view = super.onCreateView(inflater, viewGroup, bundle);
        mAppPrefs = DroidPlannerPrefs.getInstance(context);

        final Bundle args = getArguments();
        if (args != null) {
            maxFlightPathSize = args.getInt(EXTRA_MAX_FLIGHT_PATH_SIZE);
        }

        // Load the map
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                GoogleMapFragment.this.googleMap = googleMap;
            }
        });
        return view;
    }

    private GoogleMap getMap() {
        return googleMap;
    }

    @Override
    public void onStart() {
        super.onStart();
        setupMap();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
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
        return DroneMapHelper.LatLngToCoord(getMap().getCameraPosition().target);
    }

    @Override
    public float getMapZoomLevel() {
        return getMap().getCameraPosition().zoom;
    }

    @Override
    public float getMaxZoomLevel() {
        return getMap().getMaxZoomLevel();
    }

    @Override
    public float getMinZoomLevel() {
        return getMap().getMinZoomLevel();
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
        return DPMapProvider.谷歌地图;
    }

    @Override
    public void addFlightPathPoint(LatLong coord) {
        final LatLng position = DroneMapHelper.CoordToLatLang(coord);

        if (maxFlightPathSize > 0) {
            if (flightPath == null) {
                PolylineOptions flightPathOptions = new PolylineOptions();
                flightPathOptions.color(FLIGHT_PATH_DEFAULT_COLOR).width(FLIGHT_PATH_DEFAULT_WIDTH).zIndex(1);
                flightPath = getMap().addPolyline(flightPathOptions);
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

        final LatLng position = DroneMapHelper.CoordToLatLang(coord);
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
                .alpha(markerInfo.getAlpha())
                .anchor(markerInfo.getAnchorU(), markerInfo.getAnchorV())
                .infoWindowAnchor(markerInfo.getInfoWindowAnchorU(), markerInfo.getInfoWindowAnchorV()).rotation(markerInfo.getRotation())
                .snippet(markerInfo.getSnippet()).title(markerInfo.getTitle())
                .flat(markerInfo.isFlat()).visible(markerInfo.isVisible());

        final Bitmap markerIcon = markerInfo.getIcon(getResources());
        if (markerIcon != null) {
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerIcon));
        }

        Marker marker = getMap().addMarker(markerOptions);
        mBiMarkersMap.put(markerInfo, marker);
    }

    private void updateMarker(Marker marker, MarkerInfo markerInfo, LatLng position, boolean isDraggable) {
        final Bitmap markerIcon = markerInfo.getIcon(getResources());
        if (markerIcon != null) {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(markerIcon));
        }

        marker.setAlpha(markerInfo.getAlpha());
        marker.setAnchor(markerInfo.getAnchorU(), markerInfo.getAnchorV());
        marker.setInfoWindowAnchor(markerInfo.getInfoWindowAnchorU(), markerInfo.getInfoWindowAnchorV());
        marker.setPosition(position);
        marker.setRotation(markerInfo.getRotation());
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
        List<LatLong> coords = new ArrayList<LatLong>();
        Projection projection = getMap().getProjection();

        for (LatLong point : path) {
            LatLng coord = projection.fromScreenLocation(new Point((int) point.getLatitude(), (int) point.getLongitude()));
            coords.add(DroneMapHelper.LatLngToCoord(coord));
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
        getMap().setPadding(left, top, right, bottom);
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
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    final float zoomLevel = googleMap.getCameraPosition().zoom;
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DroneMapHelper.CoordToLatLang(coord), zoomLevel));
                }
            });
        }
    }

    @Override
    public void updateCamera(final LatLong coord, final float zoomLevel) {
        if (coord != null) {
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DroneMapHelper.CoordToLatLang(coord), zoomLevel));
                }
            });
        }
    }

    @Override
    public void updateCameraBearing(float bearing) {
        final CameraPosition cameraPosition = new CameraPosition(DroneMapHelper.CoordToLatLang(getMapCenter()), getMapZoomLevel(), 0, bearing);
        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void updateDroneLeashPath(PathSource pathSource) {
        List<LatLong> pathCoords = pathSource.getPathPoints();
        final List<LatLng> pathPoints = new ArrayList<LatLng>(pathCoords.size());
        for (LatLong coord : pathCoords) {
            pathPoints.add(DroneMapHelper.CoordToLatLang(coord));
        }

        if (mDroneLeashPath == null) {
            final PolylineOptions flightPath = new PolylineOptions();
            flightPath.color(DRONE_LEASH_DEFAULT_COLOR).width(
                    DroneMapHelper.scaleDpToPixels(DRONE_LEASH_DEFAULT_WIDTH, getResources()));

            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mDroneLeashPath = getMap().addPolyline(flightPath);
                    mDroneLeashPath.setPoints(pathPoints);
                }
            });
        } else {
            mDroneLeashPath.setPoints(pathPoints);
        }
    }

    @Override
    public void updateMissionPath(PathSource pathSource) {
        List<LatLong> pathCoords = pathSource.getPathPoints();
        final List<LatLng> pathPoints = new ArrayList<>(pathCoords.size());
        for (LatLong coord : pathCoords) {
            pathPoints.add(DroneMapHelper.CoordToLatLang(coord));
        }

        if (missionPath == null) {
            final PolylineOptions pathOptions = new PolylineOptions();
            pathOptions.color(MISSION_PATH_DEFAULT_COLOR).width(MISSION_PATH_DEFAULT_WIDTH);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    missionPath = getMap().addPolyline(pathOptions);
                    missionPath.setPoints(pathPoints);
                }
            });
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
            pathOptions.strokeColor(POLYGONS_PATH_DEFAULT_COLOR).strokeWidth(
                    POLYGONS_PATH_DEFAULT_WIDTH);
            final List<LatLng> pathPoints = new ArrayList<LatLng>(contour.size());
            for (LatLong coord : contour) {
                pathPoints.add(DroneMapHelper.CoordToLatLang(coord));
            }
            pathOptions.addAll(pathPoints);
            polygonsPaths.add(getMap().addPolygon(pathOptions));
        }
    }

    @Override
    public void addCameraFootprint(FootPrint footprintToBeDraw) {
        PolygonOptions pathOptions = new PolygonOptions();
        pathOptions.strokeColor(FOOTPRINT_DEFAULT_COLOR).strokeWidth(FOOTPRINT_DEFAULT_WIDTH);
        pathOptions.fillColor(FOOTPRINT_FILL_COLOR);

        for (LatLong vertex : footprintToBeDraw.getVertexInGlobalFrame()) {
            pathOptions.add(DroneMapHelper.CoordToLatLang(vertex));
        }
        getMap().addPolygon(pathOptions);

    }

    /**
     * Save the map camera state on a preference file
     * http://stackoverflow.com/questions /16697891/google-maps-android-api-v2-restoring -map-state/16698624#16698624
     */
    @Override
    public void saveCameraPosition() {
        final GoogleMap googleMap = getMap();
        if (googleMap == null)
            return;

        CameraPosition camera = googleMap.getCameraPosition();
        mAppPrefs.prefs.edit()
                .putFloat(PREF_LAT, (float) camera.target.latitude)
                .putFloat(PREF_LNG, (float) camera.target.longitude)
                .putFloat(PREF_BEA, camera.bearing)
                .putFloat(PREF_TILT, camera.tilt)
                .putFloat(PREF_ZOOM, camera.zoom).apply();
    }

    @Override
    public void loadCameraPosition() {
        getMapAsync(loadCameraPositionTask);
    }

    private void setupMap() {
        // Make sure the map is initialized
        MapsInitializer.initialize(getActivity().getApplicationContext());
        getMapAsync(setupMapTask);
    }

    @Override
    public void zoomToFit(List<LatLong> coords) {
        if (!coords.isEmpty()) {
            final List<LatLng> points = new ArrayList<LatLng>();
            for (LatLong coord : coords)
                points.add(DroneMapHelper.CoordToLatLang(coord));

            final LatLngBounds bounds = getBounds(points);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
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
                        googleMap.animateCamera(animation);
                    }
                }
            });
        }
    }

    @Override
    public void zoomToFitMyLocation(final List<LatLong> coords) {
        if (aMapLocation != null) {
            final List<LatLong> updatedCoords = new ArrayList<>(coords);
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
        if (!getDrone().isConnected())
            return;

        Gps gps = getDrone().getVehicleGps();
        if (!gps.isValid()) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.drone_no_location, Toast.LENGTH_SHORT).show();
            return;
        }

        final float currentZoomLevel = getMap().getCameraPosition().zoom;
        final LatLong droneLocation = gps.getPosition();
        updateCamera(droneLocation, (int) currentZoomLevel);
    }

    private void setupMapListeners(GoogleMap googleMap) {
        final GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mMapClickListener != null) {
                    mMapClickListener.onMapClick(DroneMapHelper.LatLngToCoord(latLng));
                }
            }
        };
        googleMap.setOnMapClickListener(onMapClickListener);

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (mMapLongClickListener != null) {
                    mMapLongClickListener.onMapLongClick(DroneMapHelper.LatLngToCoord(latLng));
                }
            }
        });

        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if (!(markerInfo instanceof GraphicHome)) {
                        markerInfo.setPosition(DroneMapHelper.LatLngToCoord(marker.getPosition()));
                        mMarkerDragListener.onMarkerDragStart(markerInfo);
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if (!(markerInfo instanceof GraphicHome)) {
                        markerInfo.setPosition(DroneMapHelper.LatLngToCoord(marker.getPosition()));
                        mMarkerDragListener.onMarkerDrag(markerInfo);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    markerInfo.setPosition(DroneMapHelper.LatLngToCoord(marker.getPosition()));
                    mMarkerDragListener.onMarkerDragEnd(markerInfo);
                }
            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
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

    private void setupMapUI(GoogleMap map) {
        map.setMyLocationEnabled(false);
        UiSettings mUiSettings = map.getUiSettings();
        mUiSettings.setMyLocationButtonEnabled(false);
        mUiSettings.setMapToolbarEnabled(false);
        mUiSettings.setCompassEnabled(false);
        mUiSettings.setTiltGesturesEnabled(false);
        mUiSettings.setZoomControlsEnabled(false);
        mUiSettings.setRotateGesturesEnabled(mAppPrefs.isMapRotationEnabled());
    }

    private void setupMapOverlay(GoogleMap map) {
        final Context context = getContext();
        if (context == null)
            return;

        final @GoogleMapPrefConstants.TileProvider String tileProvider = GoogleMapPrefFragment.PrefManager.getMapTileProvider(context);
        switch (tileProvider) {
            case GoogleMapPrefConstants.GOOGLE_TILE_PROVIDER:
                setupGoogleTileProvider(context, map);
                break;

            case GoogleMapPrefConstants.MAPBOX_TILE_PROVIDER:
                setupMapboxTileProvider(context, map);
                break;
        }
    }

    private void setupGoogleTileProvider(Context context, GoogleMap map) {
        //Remove the mapbox tile providers
        if (offlineTileProvider != null) {
            offlineTileProvider.remove();
            offlineTileProvider = null;
        }

        if (onlineTileProvider != null) {
            onlineTileProvider.remove();
            onlineTileProvider = null;
        }

        map.setMapType(GoogleMapPrefFragment.PrefManager.getMapType(context));
    }

    private void setupMapboxTileProvider(Context context, GoogleMap map) {
        // Remove the default google map layer.
        map.setMapType(GoogleMap.MAP_TYPE_NONE);

        final int maxZoomLevel = 19;

        if (onlineTileProvider == null) {
            final TileProvider tileProvider = new OnlineTileProvider(maxZoomLevel);
            final TileOverlayOptions options = new TileOverlayOptions()
                    .tileProvider(tileProvider).zIndex(ONLINE_TILE_PROVIDER_Z_INDEX);

            onlineTileProvider = map.addTileOverlay(options);
        }

        // Check if the offline provider is enabled as well.
        if (offlineTileProvider == null) {
            final TileProvider tileProvider = new OfflineTileProvider(context, maxZoomLevel);
            final TileOverlayOptions options = new TileOverlayOptions().
                    tileProvider(tileProvider).zIndex(OFFLINE_TILE_PROVIDER_Z_INDEX);

            offlineTileProvider = map.addTileOverlay(options);
        }
    }

    private LatLngBounds getBounds(List<LatLng> pointsList) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : pointsList) {
            builder.include(point);
        }
        return builder.build();
    }

    public double getMapRotation() {
        GoogleMap map = getMap();
        if (map != null) {
            return map.getCameraPosition().bearing;
        } else {
            return 0;
        }
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
                    pathOptions.add(DroneMapHelper.CoordToLatLang(vertex));
                }
                footprintPoly = getMap().addPolygon(pathOptions);
            } else {
                List<LatLng> list = new ArrayList<LatLng>();
                for (LatLong vertex : pathPoints) {
                    list.add(DroneMapHelper.CoordToLatLang(vertex));
                }
                footprintPoly.setPoints(list);
            }
        }
    }

    private LatLong LatLngToTempCoord(LatLng point) {
        return new LatLong((float) point.latitude, (float) point.longitude);
    }

    @Override
    public VisibleMapArea getVisibleMapArea() {
        final GoogleMap map = getMap();
        if (map == null)
            return null;

        final VisibleRegion mapRegion = map.getProjection().getVisibleRegion();
        return new VisibleMapArea(LatLngToTempCoord(mapRegion.farLeft),
                LatLngToTempCoord(mapRegion.nearLeft),
                LatLngToTempCoord(mapRegion.nearRight),
                LatLngToTempCoord(mapRegion.farRight));
    }

    @Subscribe
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null)
            return;

        this.aMapLocation = aMapLocation;
        LatLong latLong = DroneMapHelper.AMapLocationToCoord(aMapLocation);
        LatLng latLng = DroneMapHelper.CoordToLatLang(latLong);

        // Update the user location icon.
        if (userMarker == null) {
            final MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .draggable(false)
                    .visible(true)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location));
            userMarker = googleMap.addMarker(options);
        } else {
            userMarker.setPosition(latLng);
        }

        if (mPanMode.get() == AutoPanMode.USER) {
            updateCamera(latLong, (int) googleMap.getCameraPosition().zoom);
        }

        if (mLocationListener != null) {
            mLocationListener.onLocationChanged(aMapLocation);
        }
    }
}
