package org.farring.gcs.fragments.control;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dronekit.api.CommonApiUtils;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.gcs.follow.Follow;
import com.evenbus.AttributeEvent;
import com.github.zafarkhaja.semver.Version;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.dialogs.SlideToUnlockDialog;
import org.farring.gcs.proxy.mission.MissionProxy;

/**
 * Provide functionality for flight action button specific to copters.
 */
public class CopterFlightControlFragment extends BaseFlightControlFragment {

    private static final Version BRAKE_FEATURE_FIRMWARE_VERSION = Version.forIntegers(3, 3, 0);
    private MissionProxy missionProxy;
    private View mDisconnectedButtons;
    private View mDisarmedButtons;
    private View mArmedButtons;
    private View mInFlightButtons;
    private Button followBtn;
    private Button homeBtn;
    private Button landBtn;
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

            case MISSION_DRONIE_CREATED:
                //Get the bearing of the dronie mission.
                float bearing = (float) getDrone().getMission().makeAndUploadDronie();
                if (bearing >= 0) {
                    final FlightControlManagerFragment parent = (FlightControlManagerFragment) getParentFragment();
                    if (parent != null) {
                        parent.updateMapBearing(bearing);
                    }
                }
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_copter_mission_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDisconnectedButtons = view.findViewById(R.id.mc_disconnected_buttons);
        mDisarmedButtons = view.findViewById(R.id.mc_disarmed_buttons);
        mArmedButtons = view.findViewById(R.id.mc_armed_buttons);
        mInFlightButtons = view.findViewById(R.id.mc_in_flight_buttons);

        final View connectBtn = view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);

        homeBtn = (Button) view.findViewById(R.id.mc_homeBtn);
        homeBtn.setOnClickListener(this);

        final Button armBtn = (Button) view.findViewById(R.id.mc_armBtn);
        armBtn.setOnClickListener(this);

        final Button disarmBtn = (Button) view.findViewById(R.id.mc_disarmBtn);
        disarmBtn.setOnClickListener(this);

        landBtn = (Button) view.findViewById(R.id.mc_land);
        landBtn.setOnClickListener(this);

        final Button takeoffBtn = (Button) view.findViewById(R.id.mc_takeoff);
        takeoffBtn.setOnClickListener(this);

        pauseBtn = (Button) view.findViewById(R.id.mc_pause);
        pauseBtn.setOnClickListener(this);

        autoBtn = (Button) view.findViewById(R.id.mc_autoBtn);
        autoBtn.setOnClickListener(this);

        final Button takeoffInAuto = (Button) view.findViewById(R.id.mc_TakeoffInAutoBtn);
        takeoffInAuto.setOnClickListener(this);

        followBtn = (Button) view.findViewById(R.id.mc_follow);
        followBtn.setOnClickListener(this);

        final Button dronieBtn = (Button) view.findViewById(R.id.mc_dronieBtn);
        dronieBtn.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        missionProxy = getMissionProxy();

        setupButtonsByFlightState();
        updateFlightModeButtons();
        updateFollowButton();
    }

    @Override
    public void onStop() {
        super.onStop();
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
                CommonApiUtils.arm(getDrone(), false, null);
                break;

            case R.id.mc_land:
                getDrone().getState().changeFlightMode(ApmModes.ROTOR_LAND, null);
                break;

            case R.id.mc_takeoff:
                getTakeOffConfirmation();
                break;

            case R.id.mc_homeBtn:
                getDrone().getState().changeFlightMode(ApmModes.ROTOR_RTL, null);
                break;

            case R.id.mc_pause: {
                // 暂停跟随
                final Follow followState = dpApp.getDroneManager().getFollowMe();
                if (followState.isEnabled()) {
                    followState.disableFollowMe();
                }

                if (drone.getType().getFirmwareVersionNumber().greaterThanOrEqualTo(BRAKE_FEATURE_FIRMWARE_VERSION)) {
                    drone.getState().changeFlightMode(ApmModes.ROTOR_BRAKE, null);
                } else {
                    drone.getGuidedPoint().pauseAtCurrentLocation(null);
                }
                break;
            }

            case R.id.mc_autoBtn:
                getDrone().getState().changeFlightMode(ApmModes.ROTOR_AUTO, null);
                break;

            case R.id.mc_TakeoffInAutoBtn:
                getTakeOffInAutoConfirmation();
                break;

            case R.id.mc_follow:
                dpApp.getDroneManager().getFollowMe().toggleFollowMeState();
                break;

            case R.id.mc_dronieBtn:
                getDronieConfirmation();
                break;

            default:
                break;
        }
    }

    private void getDronieConfirmation() {
        new MaterialDialog.Builder(getActivity())
                .iconRes(R.drawable.ic_launcher)
                .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                .title(getString(R.string.pref_dronie_creation_title))
                .content(getString(R.string.pref_dronie_creation_message))
                .positiveText(getString(android.R.string.yes))
                .negativeText(getString(android.R.string.no))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        missionProxy.makeAndUploadDronie();
                    }
                })
                .show();
    }

    private void getTakeOffConfirmation() {
        final SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("起飞", new Runnable() {
            @Override
            public void run() {
                final double takeOffAltitude = getAppPrefs().getDefaultAltitude();
                getDrone().getGuidedPoint().doGuidedTakeoff(takeOffAltitude);
            }
        });
        unlockDialog.show(getChildFragmentManager(), "Slide to take off");
    }

    private void getTakeOffInAutoConfirmation() {
        final SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("自动起飞", new Runnable() {
            @Override
            public void run() {
                final double takeOffAltitude = getAppPrefs().getDefaultAltitude();
                final Drone drone = getDrone();
                drone.getGuidedPoint().doGuidedTakeoff(takeOffAltitude);
                getDrone().getState().changeFlightMode(ApmModes.ROTOR_AUTO, null);
            }
        });
        unlockDialog.show(getChildFragmentManager(), "Slide to take off in auto");
    }

    private void getArmingConfirmation() {
        SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("解锁", new Runnable() {
            @Override
            public void run() {
                CommonApiUtils.arm(getDrone(), true, null);
            }
        });
        unlockDialog.show(getChildFragmentManager(), "Slide To Arm");
    }

    // 更新飞行控制按键
    private void updateFlightModeButtons() {
        resetFlightModeButtons();

        // 获取状态
        State droneState = getDrone().getState();
        // 获取模式
        final ApmModes flightMode = droneState.getMode();
        if (flightMode == null)
            return;

        switch (flightMode) {
            case ROTOR_AUTO:
                autoBtn.setActivated(true);
                break;

            case ROTOR_BRAKE:
                pauseBtn.setActivated(true);
                break;

            case ROTOR_RTL:
                homeBtn.setActivated(true);
                break;

            case ROTOR_LAND:
                landBtn.setActivated(true);
                break;
            default:
                break;
        }
    }

    private void resetFlightModeButtons() {
        homeBtn.setActivated(false);
        landBtn.setActivated(false);
        pauseBtn.setActivated(false);
        autoBtn.setActivated(false);
    }

    private void updateFollowButton() {
        switch (dpApp.getDroneManager().getFollowMe().getState()) {
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

    private void resetButtonsContainerVisibility() {
        mDisconnectedButtons.setVisibility(View.GONE);
        mDisarmedButtons.setVisibility(View.GONE);
        mArmedButtons.setVisibility(View.GONE);
        mInFlightButtons.setVisibility(View.GONE);
    }

    private void setupButtonsByFlightState() {
        final State droneState = getDrone().getState();
        if (droneState != null && getDrone().isConnected()) {
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

    private void setupButtonsForDisconnected() {
        resetButtonsContainerVisibility();
        mDisconnectedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForDisarmed() {
        resetButtonsContainerVisibility();
        mDisarmedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForArmed() {
        resetButtonsContainerVisibility();
        mArmedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForFlying() {
        resetButtonsContainerVisibility();
        mInFlightButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        if (!drone.isConnected())
            return false;

        final State droneState = drone.getState();
        return droneState.isArmed() && droneState.isFlying();
    }
}
