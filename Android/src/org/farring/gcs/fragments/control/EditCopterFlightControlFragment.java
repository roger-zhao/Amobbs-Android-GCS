package org.farring.gcs.fragments.control;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dronekit.api.CommonApiUtils;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.gcs.follow.Follow;
import com.evenbus.AttributeEvent;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.proxy.mission.item.fragments.MissionTakeoffFragment;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.dialogs.SlideToUnlockDialog;
import org.farring.gcs.proxy.mission.MissionProxy;

import java.util.Locale;

/**
 * Provide functionality for flight action button specific to copters.
 */
public class EditCopterFlightControlFragment extends BaseFlightControlFragment {

    private MissionProxy missionProxy;
    private View mDisconnectedButtons;
    private View mArmedButtons;
    private Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
    }
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
        return inflater.inflate(R.layout.fragment_copter_mission_control_edit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mArmedButtons = view.findViewById(R.id.mc_armed_buttons);
        mDisconnectedButtons = view.findViewById(R.id.mc_disconnected_buttons);
        final View connectBtn = view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);

        final Button armBtn = (Button) view.findViewById(R.id.mc_armBtn);
        armBtn.setOnClickListener(this);

        final Button disarmBtn = (Button) view.findViewById(R.id.mc_disarmBtn);
        disarmBtn.setOnClickListener(this);


        final Button takeoffBtn = (Button) view.findViewById(R.id.mc_takeoff);
        takeoffBtn.setOnClickListener(this);

        final Button takeoffInAuto = (Button) view.findViewById(R.id.mc_TakeoffInAutoBtn);
        takeoffInAuto.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        missionProxy = getMissionProxy();
        setupButtonsByFlightState();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View v) {
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


            case R.id.mc_takeoff:
                // 共有方法
                final LengthUnitProvider lup = getLengthUnitProvider();
                final View contentView = getActivity().getLayoutInflater().inflate(R.layout.dialog_edit_input_number_content, null);

                final EditText mEditText = (EditText) contentView.findViewById(R.id.dialog_edit_text_content);

                // 获取系统保留值
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity()).setView(contentView).setNegativeButton(android.R.string.cancel, null);

                mEditText.setHint(lup.boxBaseValueToTarget(5).getValue() + "米");
                alertDialog.setTitle("设置起飞离地高度")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 获得所需的Edittext控件
                                CharSequence input = mEditText.getText();
                                // 如果输入为空，则重新设置隐藏提示语
                                if (TextUtils.isEmpty(input)) {
                                    input = mEditText.getHint();
                                }
                                try {
                                    // 实际输入值，不含单位系统
                                    final double altValue = Double.parseDouble(input.toString().trim());
                                    // 转换为带有单位系统
                                    final LengthUnit newAltValue = lup.boxTargetValue(altValue);
                                    // 单位系统转出来的数值
                                    final double altPrefValue = lup.fromTargetToBase(newAltValue).getValue();

                                    // 记录标志位
                                    boolean isValueInvalid = false;
                                    String valueUpdateMsg = "起飞高度值已更新!";
                                    // Compare the new altitude value with the max altitude value
                                    if (altPrefValue < 0) {
                                        isValueInvalid = true;
                                        valueUpdateMsg = "起飞高度值不能小于0米!";
                                    } else if (altPrefValue > 100) {
                                        isValueInvalid = true;
                                        valueUpdateMsg = "最大高度值不能大于100米!";
                                    }

                                    if (!isValueInvalid) {
                                        Toast.makeText(getActivity(), "即将以 " + altPrefValue + "米 高度起飞", Toast.LENGTH_LONG).show();
                                        getTakeOffConfirmation(altPrefValue);
                                    }

                                } catch (NumberFormatException e) {
                                    if (getActivity() != null) {
                                        Toast.makeText(getActivity(), "输入有误，请重新输入: " + mEditText.getText(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                alertDialog.create().show();
                break;


            case R.id.mc_TakeoffInAutoBtn:
                getTakeOffInAutoConfirmation();
                break;

            default:
                break;
        }
    }


    private void getTakeOffConfirmation(final double takeOffAlt) {
        final SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("起飞", new Runnable() {
            @Override
            public void run() {
                final double takeOffAltitude = takeOffAlt; // getAppPrefs().getDefaultAltitude();
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



    private void resetButtonsContainerVisibility() {
        mDisconnectedButtons.setVisibility(View.GONE);
        mArmedButtons.setVisibility(View.GONE);
    }

    private void setupButtonsByFlightState() {
        final State droneState = getDrone().getState();
        if (droneState != null && getDrone().isConnected()) {
            setupButtonsForArmed();
        } else {
            setupButtonsForArmed();
        }
    }

    private void setupButtonsForDisconnected() {
        resetButtonsContainerVisibility();
        mDisconnectedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForArmed() {
        resetButtonsContainerVisibility();
        mArmedButtons.setVisibility(View.VISIBLE);
    }


    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        if (!drone.isConnected())
            return false;

        final State droneState = drone.getState();
        return droneState.isArmed() && droneState.isFlying();
    }
}
