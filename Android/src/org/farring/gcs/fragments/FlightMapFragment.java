package org.farring.gcs.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.variables.GuidedPoint;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.dialogs.GuidedDialog;
import org.farring.gcs.dialogs.GuidedDialog.GuidedDialogListener;
import org.farring.gcs.graphic.map.GraphicHome;
import org.farring.gcs.maps.DPMap.OnMapLongClickListener;
import org.farring.gcs.maps.DPMap.OnMarkerClickListener;
import org.farring.gcs.maps.DPMap.OnMarkerDragListener;
import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.utils.DroneMapHelper;
import org.farring.gcs.utils.prefs.AutoPanMode;

public class FlightMapFragment extends DroneMap implements OnMapLongClickListener,
        OnMarkerClickListener, OnMarkerDragListener, GuidedDialogListener {

    private static final int MAX_TOASTS_FOR_LOCATION_PRESS = 3;
    private static final String PREF_USER_LOCATION_FIRST_PRESS = "pref_user_location_first_press";
    private static final int DEFAULT_USER_LOCATION_FIRST_PRESS = 0;
    private static final String PREF_DRONE_LOCATION_FIRST_PRESS = "pref_drone_location_first_press";
    private static final int DEFAULT_DRONE_LOCATION_FIRST_PRESS = 0;
    /**
     * The map should zoom on the user location the first time it's acquired. This flag helps enable the behavior.
     */
    private static boolean didZoomOnUserLocation = false;
    private OnGuidedClickListener guidedClickListener;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        super.onReceiveAttributeEvent(attributeEvent);
        switch (attributeEvent) {
            case STATE_ARMING:
                final State droneState = drone.getState();
                if (droneState.isArmed()) {
                    mMapFragment.clearFlightPath();
                }
                break;
        }
    }

    public void setGuidedClickListener(OnGuidedClickListener guidedClickListener) {
        this.guidedClickListener = guidedClickListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        View view = super.onCreateView(inflater, viewGroup, bundle);

        mMapFragment.setOnMapLongClickListener(this);
        mMapFragment.setOnMarkerDragListener(this);
        mMapFragment.setOnMarkerClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapFragment.selectAutoPanMode(mAppPrefs.getAutoPanMode());

        if (!didZoomOnUserLocation) {
            super.goToMyLocation();
            didZoomOnUserLocation = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapFragment.selectAutoPanMode(AutoPanMode.DISABLED);
    }

    @Override
    protected int getMaxFlightPathSize() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.valueOf(prefs.getString("pref_max_flight_path_size", "500"));
    }

    @Override
    public boolean setAutoPanMode(AutoPanMode target) {
        // Update the map panning preferences.
        if (mAppPrefs != null)
            mAppPrefs.setAutoPanMode(target);

        if (mMapFragment != null)
            mMapFragment.selectAutoPanMode(target);
        return true;
    }

    @Override
    public void onMapLongClick(LatLong coord) {
        if (drone != null && drone.isConnected()) {
            final GuidedPoint guidedState = drone.getGuidedPoint();
            if (guidedState.isInitialized()) {
                if (guidedClickListener != null)
                    guidedClickListener.onGuidedClick(coord);
            } else {
                GuidedDialog dialog = new GuidedDialog();
                dialog.setCoord(DroneMapHelper.CoordToGaodeLatLang(coord));
                dialog.setListener(this);
                dialog.show(getChildFragmentManager(), "GUIDED dialog");
            }
        }
    }

    @Override
    public void onForcedGuidedPoint(LatLng coord) {
        try {
            GuidedPoint guidedPoint = drone.getGuidedPoint();
            if (guidedPoint.isInitialized()) {
                guidedPoint.newGuidedCoord(DroneMapHelper.GaodeLatLngToCoord(coord));
            }
            guidedPoint.forcedGuidedCoordinate(DroneMapHelper.GaodeLatLngToCoord(coord), null);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMarkerDragStart(MarkerInfo markerInfo) {
    }

    @Override
    public void onMarkerDrag(MarkerInfo markerInfo) {
    }

    @Override
    public void onMarkerDragEnd(MarkerInfo markerInfo) {
        if (!(markerInfo instanceof GraphicHome)) {
            GuidedPoint guidedPoint = drone.getGuidedPoint();
            if (guidedPoint.isInitialized()) {
                guidedPoint.newGuidedCoord(markerInfo.getPosition());
            }
        }
    }

    @Override
    public boolean onMarkerClick(MarkerInfo markerInfo) {
        if (markerInfo == null)
            return false;
        GuidedPoint guidedPoint = drone.getGuidedPoint();
        if (guidedPoint.isInitialized()) {
            guidedPoint.newGuidedCoord(markerInfo.getPosition());
        }
        return true;
    }

    @Override
    protected boolean isMissionDraggable() {
        return false;
    }

    @Override
    public void goToMyLocation() {
        super.goToMyLocation();
        int pressCount = mAppPrefs.prefs.getInt(PREF_USER_LOCATION_FIRST_PRESS, DEFAULT_USER_LOCATION_FIRST_PRESS);
        if (pressCount < MAX_TOASTS_FOR_LOCATION_PRESS) {
            Toast.makeText(context, R.string.user_autopan_long_press, Toast.LENGTH_LONG).show();
            mAppPrefs.prefs.edit().putInt(PREF_USER_LOCATION_FIRST_PRESS, pressCount + 1).apply();
        }
    }

    @Override
    public void goToDroneLocation() {
        super.goToDroneLocation();

        if (this.drone == null)
            return;

        final Gps droneGps = this.drone.getVehicleGps();
        if (droneGps == null || !droneGps.isValid())
            return;

        final int pressCount = mAppPrefs.prefs.getInt(PREF_DRONE_LOCATION_FIRST_PRESS, DEFAULT_DRONE_LOCATION_FIRST_PRESS);
        if (pressCount < MAX_TOASTS_FOR_LOCATION_PRESS) {
            Toast.makeText(context, R.string.drone_autopan_long_press, Toast.LENGTH_LONG).show();
            mAppPrefs.prefs.edit().putInt(PREF_DRONE_LOCATION_FIRST_PRESS, pressCount + 1).apply();
        }
    }

    public interface OnGuidedClickListener {
        void onGuidedClick(LatLong coord);
    }
}
