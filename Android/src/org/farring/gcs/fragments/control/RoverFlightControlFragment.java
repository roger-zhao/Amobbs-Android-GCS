package org.farring.gcs.fragments.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.State;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;

/**
 * Created by Fredia Huya-Kouadio on 3/4/15.
 */
public class RoverFlightControlFragment extends BaseFlightControlFragment {

    private View mDisconnectedButtons;
    private View mActiveButtons;
    private Button homeBtn;
    private Button pauseBtn;
    private Button autoBtn;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_ARMING:
            case STATE_CONNECTED:
            case STATE_DISCONNECTED:
            case STATE_UPDATED:
                setupButtonsByFlightState();
                break;

            case STATE_VEHICLE_MODE:
                updateFlightModeButtons();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rover_mission_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDisconnectedButtons = view.findViewById(R.id.mc_disconnected_buttons);
        mActiveButtons = view.findViewById(R.id.mc_connected_buttons);

        final View connectBtn = view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);

        homeBtn = (Button) view.findViewById(R.id.mc_homeBtn);
        homeBtn.setOnClickListener(this);

        pauseBtn = (Button) view.findViewById(R.id.mc_pause);
        pauseBtn.setOnClickListener(this);

        autoBtn = (Button) view.findViewById(R.id.mc_autoBtn);
        autoBtn.setOnClickListener(this);
    }

    private void updateFlightModeButtons() {
        resetFlightModeButtons();

        final Drone drone = getDrone();
        final State droneState = drone.getState();
        final ApmModes flightMode = droneState.getMode();
        if (flightMode != null) {
            switch (flightMode) {
                case ROVER_AUTO:
                    autoBtn.setActivated(true);
                    break;

                case ROVER_HOLD:
                case ROVER_GUIDED:
                    pauseBtn.setActivated(true);
                    break;

                case ROVER_RTL:
                    homeBtn.setActivated(true);
                    break;
            }
        }
    }

    private void resetFlightModeButtons() {
        homeBtn.setActivated(false);
        pauseBtn.setActivated(false);
        autoBtn.setActivated(false);
    }

    private void setupButtonsByFlightState() {
        final State droneState = getDrone().getState();
        if (droneState != null && getDrone().isConnected()) {
            setupButtonsForConnected();
        } else {
            setupButtonsForDisconnected();
        }
    }

    private void resetButtonsContainerVisibility() {
        mDisconnectedButtons.setVisibility(View.GONE);
        mActiveButtons.setVisibility(View.GONE);
    }

    private void setupButtonsForDisconnected() {
        resetButtonsContainerVisibility();
        mDisconnectedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForConnected() {
        resetButtonsContainerVisibility();
        mActiveButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        setupButtonsByFlightState();
        updateFlightModeButtons();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        return drone.isConnected();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mc_connectBtn:
                ((SuperUI) getActivity()).toggleDroneConnection();
                break;

            case R.id.mc_homeBtn:
                getDrone().getState().changeFlightMode(ApmModes.ROVER_RTL, null);
                break;

            case R.id.mc_pause: {
                getDrone().getState().changeFlightMode(ApmModes.ROVER_HOLD, null);
                break;
            }

            case R.id.mc_autoBtn:
                getDrone().getState().changeFlightMode(ApmModes.ROVER_AUTO, null);
                break;

            default:
                break;
        }
    }
}
