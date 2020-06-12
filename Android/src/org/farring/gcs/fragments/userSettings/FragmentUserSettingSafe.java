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


public class FragmentUserSettingSafe extends BaseFragment implements CardWheelHorizontalView.OnCardWheelScrollListener, DroneInterfaces.OnParameterManagerListener{

    private ParameterManager parameterManager;
    private Parameter rcfsBehavior, minBattVolt;

    SpringSwitchButton rcfsBehaviorBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_safesetting, container, false);
        super.onViewCreated(view, savedInstanceState);
//
        final Context context = getContext();
//
        final NumericWheelAdapter BattVoltMinAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 3, 100, "%d");
        CardWheelHorizontalView<Integer> BattVoltMinPicker = (CardWheelHorizontalView) view.findViewById(R.id.BattVoltMinPicker);
        BattVoltMinPicker.setViewAdapter(BattVoltMinAdapter);
        BattVoltMinPicker.addScrollListener(this);
        parameterManager = getDrone().getParameterManager();

        rcfsBehaviorBtn = (SpringSwitchButton) view.findViewById(R.id.RCFSBehavior);
        rcfsBehaviorBtn.setEnabled(true);
        rcfsBehaviorBtn.setOnToggleListener(new SpringSwitchButton.OnToggleListener() {
            @Override
            public void onToggle(boolean left) {
                setrcfsBehavior(left?1:0);
                // EventBus.getDefault().post(ActionEvent.ACTION_PREF_UNIT_SYSTEM_UPDATE);
            }
        });

        if (getDrone().isConnected()) {
            minBattVolt = parameterManager.getParameter("FS_VOLT");
            BattVoltMinPicker.setCurrentValue((int)minBattVolt.getValue());
            rcfsBehavior = parameterManager.getParameter("RC_FS_NON_AUTO");
            rcfsBehaviorBtn.setLeft((((int)rcfsBehavior.getValue()) == 0)?0:1);
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
            case R.id.BattVoltMinPicker:
                if (getDrone().isConnected()) {
                    minBattVolt = parameterManager.getParameter("FS_VOLT");
                    minBattVolt.setValue((int) endValue);
                    parameterManager.sendParameter(minBattVolt);
                }
                else
                {
                    Toast.makeText(getActivity(), "飞控未连接，无法保存当前修改", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public void setrcfsBehavior(int rcfsBehav) {
        if (getDrone().isConnected()) {
            rcfsBehavior = parameterManager.getParameter("RC_FS_NON_AUTO");
            rcfsBehavior.setValue((int) rcfsBehav);
            parameterManager.sendParameter(rcfsBehavior);
        }
        else
        {
            Toast.makeText(getActivity(), "飞控未连接，无法保存当前修改", Toast.LENGTH_SHORT).show();
        }
    }
}
