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
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;


public class FragmentUserSettingApp extends BaseFragment implements CardWheelHorizontalView.OnCardWheelScrollListener, DroneInterfaces.OnParameterManagerListener{

    private ParameterManager parameterManager;
    private Parameter ABpointDist, spinnerSpd, pumpSpd;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_userappsetting, container, false);
        super.onViewCreated(view, savedInstanceState);
//
        final Context context = getContext();
//
        final NumericWheelAdapter flyWideAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 5, 100, "%d");
        CardWheelHorizontalView<Integer> flyWidePicker = (CardWheelHorizontalView) view.findViewById(R.id.flyWidePicker);
        flyWidePicker.setViewAdapter(flyWideAdapter);
        flyWidePicker.addScrollListener(this);

        final NumericWheelAdapter spinnerSpdAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 100, "%d");
        CardWheelHorizontalView<Integer> spinnerSpdPicker = (CardWheelHorizontalView) view.findViewById(R.id.spinnerSpdPicker);
        spinnerSpdPicker.setViewAdapter(spinnerSpdAdapter);
        spinnerSpdPicker.addScrollListener(this);

        final NumericWheelAdapter pumpSpdAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 100, "%d");
        CardWheelHorizontalView<Integer> pumpSpdPicker = (CardWheelHorizontalView) view.findViewById(R.id.pumpSpdPicker);
        pumpSpdPicker.setViewAdapter(pumpSpdAdapter);
        pumpSpdPicker.addScrollListener(this);

        parameterManager = getDrone().getParameterManager();

        if (getDrone().isConnected()) {
            spinnerSpd = parameterManager.getParameter("CROP_SPD");
            pumpSpd = parameterManager.getParameter("PMP_SPD");
            ABpointDist = parameterManager.getParameter("AB_DIS");
            flyWidePicker.setCurrentValue((int)ABpointDist.getValue());
            spinnerSpdPicker.setCurrentValue((int)spinnerSpd.getValue());
            pumpSpdPicker.setCurrentValue((int)pumpSpd.getValue());
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
            case R.id.flyWidePicker:
                if (getDrone().isConnected()) {
                    ABpointDist = parameterManager.getParameter("AB_DIS");
                    ABpointDist.setValue((int) endValue);
                    parameterManager.sendParameter(ABpointDist);
                } else {
                    Toast.makeText(getActivity(), "飞控未连接，无法保存当前修改", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.spinnerSpdPicker:
                if (getDrone().isConnected()) {
                    spinnerSpd = parameterManager.getParameter("CROP_SPD");
                    spinnerSpd.setValue(((int) endValue)*10+1000);
                    parameterManager.sendParameter(spinnerSpd);

                } else {
                    Toast.makeText(getActivity(), "飞控未连接，无法保存当前修改", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.pumpSpdPicker:
                if (getDrone().isConnected()) {
                    pumpSpd = parameterManager.getParameter("PMP_SPD");
                    pumpSpd.setValue(((int) endValue)*10+1000);
                    parameterManager.sendParameter(pumpSpd);

                } else {
                    Toast.makeText(getActivity(), "飞控未连接，无法保存当前修改", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
