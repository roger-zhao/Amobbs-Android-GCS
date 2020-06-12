package org.farring.gcs.fragments.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.dronekit.api.CommonApiUtils;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.GuidedPoint;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.gcs.follow.Follow;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.dialogs.SlideToUnlockDialog;

/**
 * Provides functionality for flight action buttons specific to planes.
 */
public class PlaneFlightControlFragment extends BaseFlightControlFragment {
    private View mDisconnectedButtons;
    private View disarmedButtons;
    private View armedButtons;
    private View mInFlightButtons;
    private Button followBtn;
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

            case FOLLOW_START:
            case FOLLOW_STOP:
                final Follow followState = dpApp.getDroneManager().getFollowMe();
                if (followState != null) {
                    String eventLabel = null;
                    switch (followState.getState()) {
                        case FOLLOW_START:
                            eventLabel = "使能跟随";
                            break;

                        case FOLLOW_RUNNING:
                            eventLabel = "跟随运行中";
                            break;

                        case FOLLOW_END:
                            eventLabel = "跟随失能";
                            break;

                        case FOLLOW_INVALID_STATE:
                            eventLabel = "跟随失败: 状态不可用";
                            break;

                        case FOLLOW_DRONE_DISCONNECTED:
                            eventLabel = "跟随失败: 飞行器未连接";
                            break;

                        case FOLLOW_DRONE_NOT_ARMED:
                            eventLabel = "跟随失败: 飞行器未解锁";
                            break;
                    }
                    Toast.makeText(getActivity(), eventLabel, Toast.LENGTH_SHORT).show();
                }

        /* FALL - THROUGH */
            case FOLLOW_UPDATE:
                updateFlightModeButtons();
                updateFollowButton();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plane_mission_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDisconnectedButtons = view.findViewById(R.id.mc_disconnected_buttons);
        disarmedButtons = view.findViewById(R.id.mc_disarmed_buttons);
        armedButtons = view.findViewById(R.id.mc_armed_buttons);
        mInFlightButtons = view.findViewById(R.id.mc_connected_buttons);

        final View connectBtn = view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);

        homeBtn = (Button) view.findViewById(R.id.mc_homeBtn);
        homeBtn.setOnClickListener(this);

        final Button armBtn = (Button) view.findViewById(R.id.mc_armBtn);
        armBtn.setOnClickListener(this);

        final Button disarmBtn = (Button) view.findViewById(R.id.mc_disarmBtn);
        disarmBtn.setOnClickListener(this);

        final Button takeoffInAuto = (Button) view.findViewById(R.id.mc_TakeoffInAutoBtn);
        takeoffInAuto.setOnClickListener(this);

        pauseBtn = (Button) view.findViewById(R.id.mc_pause);
        pauseBtn.setOnClickListener(this);

        autoBtn = (Button) view.findViewById(R.id.mc_autoBtn);
        autoBtn.setOnClickListener(this);

        followBtn = (Button) view.findViewById(R.id.mc_follow);
        followBtn.setOnClickListener(this);
    }

    private void updateFollowButton() {
        final Follow followState = dpApp.getDroneManager().getFollowMe();
        if (followState == null)
            return;

        switch (followState.getState()) {
            case FOLLOW_START:
                followBtn.setBackgroundColor(getResources().getColor(R.color.orange));
                break;

            case FOLLOW_RUNNING:
                followBtn.setActivated(true);
                followBtn.setBackgroundResource(R.drawable.flight_action_row_bg_selector);
                break;

            default:
                followBtn.setActivated(false);
                followBtn.setBackgroundResource(R.drawable.flight_action_row_bg_selector);
                break;
        }
    }

    private void updateFlightModeButtons() {
        resetFlightModeButtons();

        final Drone drone = getDrone();
        final State droneState = drone.getState();
        final ApmModes flightMode = droneState.getMode();
        if (flightMode != null) {
            switch (flightMode) {
                case FIXED_WING_AUTO:
                    autoBtn.setActivated(true);
                    break;

                case FIXED_WING_GUIDED:
                    final GuidedPoint guidedState = drone.getGuidedPoint();
                    final Follow followState = dpApp.getDroneManager().getFollowMe();
                    if (guidedState.isInitialized() && !followState.isEnabled()) {
                        pauseBtn.setActivated(true);
                    }
                    break;

                case FIXED_WING_RTL:
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
        if (getDrone().isConnected()) {
            if (droneState.isArmed()) {
                if (droneState.isFlying()) {
                    setupButtonsForFlying();
                } else {
                    setupButtonsForArmed();
                }
            } else {
                setupButtonsForDisarmed();
            }
        } else {
            setupButtonsForDisconnected();
        }
    }

    private void resetButtonsContainerVisibility() {
        mDisconnectedButtons.setVisibility(View.GONE);
        disarmedButtons.setVisibility(View.GONE);
        armedButtons.setVisibility(View.GONE);
        mInFlightButtons.setVisibility(View.GONE);
    }

    private void setupButtonsForDisconnected() {
        resetButtonsContainerVisibility();
        mDisconnectedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForDisarmed() {
        resetButtonsContainerVisibility();
        disarmedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForArmed() {
        resetButtonsContainerVisibility();
        armedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForFlying() {
        resetButtonsContainerVisibility();
        mInFlightButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();

        setupButtonsByFlightState();
        updateFlightModeButtons();
        updateFollowButton();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void getArmingConfirmation() {
        SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("解锁", new Runnable() {
            @Override
            public void run() {
                CommonApiUtils.arm(getDrone(), true, null);
            }
        });
        unlockDialog.show(getChildFragmentManager(), "滑动解锁");
    }

    @Override
    public void onClick(View v) {
        final Drone drone = getDrone();
        switch (v.getId()) {
            case R.id.mc_connectBtn:
                ((SuperUI) getActivity()).toggleDroneConnection();
                break;

            case R.id.mc_armBtn:
                getArmingConfirmation();
                break;

            case R.id.mc_disarmBtn:
                CommonApiUtils.arm(drone, false, null);
                break;

            case R.id.mc_homeBtn:
                getDrone().getState().changeFlightMode(ApmModes.FIXED_WING_RTL, null);
                break;

            case R.id.mc_pause: {
                final Follow followState = dpApp.getDroneManager().getFollowMe();
                if (followState.isEnabled()) {
                    followState.disableFollowMe();
                }
                drone.getGuidedPoint().pauseAtCurrentLocation(null);
                break;
            }

            case R.id.mc_TakeoffInAutoBtn:
            case R.id.mc_autoBtn:
                getDrone().getState().changeFlightMode(ApmModes.FIXED_WING_AUTO, null);
                break;

            case R.id.mc_follow: {
                dpApp.getDroneManager().getFollowMe().toggleFollowMeState();
                break;
            }

            default:
                break;
        }
    }

    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        final State droneState = drone.getState();
        return drone.isConnected() && droneState.isArmed() && droneState.isFlying();
    }
}
