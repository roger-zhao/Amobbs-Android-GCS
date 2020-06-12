package org.farring.gcs.fragments.mode;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.gcs.follow.Follow;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;

/**
 * Implements the flight/apm mode panel description.
 */
public class FlightModePanel extends BaseFragment {
    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        if (getActivity() == null)
            return;

        switch (attributeEvent) {
            case STATE_CONNECTED:
            case STATE_DISCONNECTED:
            case STATE_VEHICLE_MODE:
            case TYPE_UPDATED:
            case FOLLOW_START:
            case FOLLOW_STOP:
                onModeUpdate(getDrone());
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flight_mode_panel, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Update the mode info panel based on the current mode.
        onModeUpdate(getDrone());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void onModeUpdate(Drone drone) {
        // Update the info panel fragment
        final State droneState = drone.getState();
        Fragment infoPanel;
        if (droneState == null || !drone.isConnected()) {
            infoPanel = new ModeDisconnectedFragment();
        } else {
            ApmModes mode = droneState.getMode();
            if (mode == null) {
                infoPanel = new ModeDisconnectedFragment();
            } else {
                switch (mode) {
                    case ROTOR_RTL:
                    case FIXED_WING_RTL:
                    case ROVER_RTL:
                        infoPanel = new ModeRTLFragment();
                        break;

                    case FIXED_WING_AUTO:
                    case ROTOR_AUTO:
                    case ROVER_AUTO:
                        infoPanel = new ModeAutoFragment();
                        break;

                    case ROTOR_LAND:
                        infoPanel = new ModeLandFragment();
                        break;

                    case FIXED_WING_LOITER:
                    case ROTOR_LOITER:
                        infoPanel = new ModeLoiterFragment();
                        break;

                    case ROTOR_STABILIZE:
                    case FIXED_WING_STABILIZE:
                        infoPanel = new ModeStabilizeFragment();
                        break;

                    case ROTOR_ACRO:
                        infoPanel = new ModeAcroFragment();
                        break;

                    case ROTOR_ALT_HOLD:
                        infoPanel = new ModeAltholdFragment();
                        break;

                    case FIXED_WING_CIRCLE:
                    case ROTOR_CIRCLE:
                        infoPanel = new ModeCircleFragment();
                        break;

                    case ROTOR_GUIDED:
                    case FIXED_WING_GUIDED:
                    case ROVER_GUIDED:
                    case ROVER_HOLD:
                        final Follow followState = dpApp.getDroneManager().getFollowMe();
                        if (followState.isEnabled()) {
                            infoPanel = new ModeFollowFragment();
                        } else {
                            infoPanel = new ModeGuidedFragment();
                        }
                        break;

                    case ROTOR_TOY:
                        infoPanel = new ModeDriftFragment();
                        break;

                    case ROTOR_SPORT:
                        infoPanel = new ModeSportFragment();
                        break;

                    case ROTOR_POSHOLD:
                        infoPanel = new ModePosHoldFragment();
                        break;

                    default:
                        infoPanel = new ModeDisconnectedFragment();
                        break;
                }
            }
        }

        getChildFragmentManager().beginTransaction().replace(R.id.modeInfoPanel, infoPanel).commitAllowingStateLoss();
    }
}