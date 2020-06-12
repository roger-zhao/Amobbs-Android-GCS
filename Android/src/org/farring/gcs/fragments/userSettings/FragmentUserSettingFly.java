package org.farring.gcs.fragments.userSettings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dronekit.core.drone.DroneInterfaces;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;

import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.view.button.SpringSwitchButton;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;


public class FragmentUserSettingFly extends BaseFragment implements CardWheelHorizontalView.OnCardWheelScrollListener, DroneInterfaces.OnParameterManagerListener{

    private ParameterManager parameterManager;
    private Parameter wpSpeed, loitSpeed, yawBehavior;

    SpringSwitchButton yawBehaviorBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_flysetting, container, false);
        super.onViewCreated(view, savedInstanceState);
//
        final Context context = getContext();
//
        final NumericWheelAdapter flySpdAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 5, 15, "%d");
        CardWheelHorizontalView<Integer> flySpdPicker = (CardWheelHorizontalView) view.findViewById(R.id.flyVelMaxPicker);
        flySpdPicker.setViewAdapter(flySpdAdapter);
        flySpdPicker.addScrollListener(this);

        yawBehaviorBtn = (SpringSwitchButton) view.findViewById(R.id.flyYawBehavior);
        yawBehaviorBtn.setEnabled(true);
        yawBehaviorBtn.setOnToggleListener(new SpringSwitchButton.OnToggleListener() {
            @Override
            public void onToggle(boolean left) {
                setYawBehavior(left?1:0);
                // EventBus.getDefault().post(ActionEvent.ACTION_PREF_UNIT_SYSTEM_UPDATE);
            }
        });

        parameterManager = getDrone().getParameterManager();
        if (getDrone().isConnected()) {
            wpSpeed = parameterManager.getParameter("WPNAV_SPEED");
            flySpdPicker.setCurrentValue((int)wpSpeed.getValue()/100);
            yawBehavior = parameterManager.getParameter("WP_YAW_BEHAVIOR");
            yawBehaviorBtn.setLeft((((int)yawBehavior.getValue()) == 0)?0:1);
        }
        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        // 添加参数对象监听器
        parameterManager.setParameterListener(this);
        // 打开遥控数据流
    }
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onBeginReceivingParameters() {

    }

    @Override
    public void onParameterReceived(Parameter parameter, int index, int count) {

    }

    @Override
    public void onEndReceivingParameters() {

    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Object startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Object oldValue, Object newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, Object startValue, Object endValue) {
        switch (wheel.getId()) {
            case R.id.flyVelMaxPicker:
                if (getDrone().isConnected()) {
                    yawBehavior = parameterManager.getParameter("WP_YAW_BEHAVIOR");
                    wpSpeed.setValue((int) endValue*100);
                    parameterManager.sendParameter(wpSpeed);
                    loitSpeed = parameterManager.getParameter("WPNAV_LOIT_SPEED");
                    loitSpeed.setValue((int) endValue*100);
                    parameterManager.sendParameter(loitSpeed);
                }
                else
                {
                    Toast.makeText(getActivity(), "飞控未连接，无法保存当前修改", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public void setYawBehavior(int yawBehav) {
        if (getDrone().isConnected()) {
            yawBehavior = parameterManager.getParameter("WP_YAW_BEHAVIOR");
            yawBehavior.setValue(yawBehav);
            parameterManager.sendParameter(yawBehavior);
        }
        else
        {
            Toast.makeText(getActivity(), "飞控未连接，无法保存当前修改", Toast.LENGTH_SHORT).show();
        }
    }
}
