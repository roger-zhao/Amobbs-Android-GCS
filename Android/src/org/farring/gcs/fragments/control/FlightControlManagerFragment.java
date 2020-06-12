package org.farring.gcs.fragments.control;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.Type;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.FlightDataFragment;
import org.farring.gcs.fragments.helpers.BaseFragment;

public class FlightControlManagerFragment extends BaseFragment {

    private static final String EXTRA_LAST_VEHICLE_TYPE = "extra_last_vehicle_type";
    private static final int DEFAULT_LAST_VEHICLE_TYPE = Type.TYPE_UNKNOWN;
    private int droneType;
    private SlidingUpHeader header;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTED:
            case TYPE_UPDATED:
                selectActionsBar(getDrone().getType().getDroneType());
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        final Fragment parent = getParentFragment();
        if (!(parent instanceof FlightDataFragment)) {
            throw new IllegalStateException("Parent must be an instance of " + FlightDataFragment.class.getName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flight_actions_bar, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.droneType = savedInstanceState == null
                ? DEFAULT_LAST_VEHICLE_TYPE
                : savedInstanceState.getInt(EXTRA_LAST_VEHICLE_TYPE, DEFAULT_LAST_VEHICLE_TYPE);
        selectActionsBar(this.droneType, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_LAST_VEHICLE_TYPE, droneType);
    }

    @Override
    public void onStart() {
        super.onStart();
        Drone drone = getDrone();
        if (drone.isConnected()) {
            selectActionsBar(getDrone().getType().getDroneType());
        } else {
            selectActionsBar(Type.TYPE_UNKNOWN);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isResumed())
            selectActionsBar(Type.TYPE_UNKNOWN);
    }

    private void selectActionsBar(int droneType) {
        selectActionsBar(droneType, false);
    }

    private void selectActionsBar(int droneType, boolean force) {
        if (this.droneType == droneType && !force)
            return;

        this.droneType = droneType;

        final FragmentManager fm = getChildFragmentManager();
        Fragment actionsBarFragment;
        switch (droneType) {
            case Type.TYPE_COPTER:
                actionsBarFragment = new CopterFlightControlFragment();
                break;

            case Type.TYPE_PLANE:
                actionsBarFragment = new PlaneFlightControlFragment();
                break;

            case Type.TYPE_ROVER:
                actionsBarFragment = new RoverFlightControlFragment();
                break;

            case Type.TYPE_UNKNOWN:
            default:
                actionsBarFragment = new GenericActionsFragment();
                break;
        }

        fm.beginTransaction().replace(R.id.flight_actions_bar, actionsBarFragment).commitAllowingStateLoss();
        header = (SlidingUpHeader) actionsBarFragment;
    }

    public boolean isSlidingUpPanelEnabled(Drone drone) {
        Type type = drone.getType();
        selectActionsBar(type.getDroneType());
        return header != null && header.isSlidingUpPanelEnabled(drone);
    }

    public void updateMapBearing(float bearing) {
        final FlightDataFragment parent = (FlightDataFragment) getParentFragment();
        if (parent != null) {
            parent.updateMapBearing(bearing);
        }
    }

    public interface SlidingUpHeader {
        boolean isSlidingUpPanelEnabled(Drone drone);
    }
}
