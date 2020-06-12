package org.farring.gcs.fragments.calibration;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.dronekit.api.CommonApiUtils;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;

import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;
import com.dronekit.core.drone.DroneInterfaces;


public class FragmentSetupMotorTest extends BaseFragment implements CardWheelHorizontalView.OnCardWheelScrollListener, DroneInterfaces.OnParameterManagerListener{

    private ParameterManager parameterManager;
    private Parameter frameType;
    private Button M1testBtn;
    private Button M2testBtn;
    private Button M3testBtn;
    private Button M4testBtn;
    private Button M5testBtn;
    private Button M6testBtn;
    private Button M7testBtn;
    private Button M8testBtn;
    private int thrVal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_motortest, container, false);
        super.onViewCreated(view, savedInstanceState);
//
        final Context context = getContext();
//
        final NumericWheelAdapter throttleAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 5, 20, "%d");
        CardWheelHorizontalView<Integer> throttlePicker = (CardWheelHorizontalView) view.findViewById(R.id.mototestThrottlePicker);
        throttlePicker.setViewAdapter(throttleAdapter);
        throttlePicker.addScrollListener(this);
        parameterManager = getDrone().getParameterManager();

        M1testBtn = (Button) view.findViewById(R.id.mc_M1testBtn);
        M1testBtn.setEnabled(true);
        M1testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });
        M2testBtn = (Button) view.findViewById(R.id.mc_M2testBtn);
        M2testBtn.setEnabled(true);
        M2testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });
        M3testBtn = (Button) view.findViewById(R.id.mc_M3testBtn);
        M3testBtn.setEnabled(true);
        M3testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });
        M4testBtn = (Button) view.findViewById(R.id.mc_M4testBtn);
        M4testBtn.setEnabled(true);
        M4testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });
        M5testBtn = (Button) view.findViewById(R.id.mc_M5testBtn);
        M5testBtn.setEnabled(true);
        M5testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });
        M6testBtn = (Button) view.findViewById(R.id.mc_M6testBtn);
        M6testBtn.setEnabled(true);
        M6testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });
        M7testBtn = (Button) view.findViewById(R.id.mc_M7testBtn);
        M7testBtn.setEnabled(true);
        M7testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });
        M8testBtn = (Button) view.findViewById(R.id.mc_M8testBtn);
        M8testBtn.setEnabled(true);
        M8testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMotorTest(v);
            }
        });

        if (getDrone().isConnected()) {
            frameType = parameterManager.getParameter("FRAME_TYPE");
            int motorMaxNum = (int)frameType.getValue();
            if(motorMaxNum == 4)
            {
                M5testBtn.setVisibility(View.GONE);
                M6testBtn.setVisibility(View.GONE);
                M7testBtn.setVisibility(View.GONE);
                M8testBtn.setVisibility(View.GONE);
            }
            else if(motorMaxNum == 6)
            {
                M7testBtn.setVisibility(View.GONE);
                M8testBtn.setVisibility(View.GONE);
            }
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
            case R.id.mototestThrottlePicker:
                thrVal = (int) endValue;
                Toast.makeText(getActivity(), "油门比例: "+ thrVal + "%", Toast.LENGTH_SHORT).show();
                break;

        }
    }
    public void doMotorTest(View v) {
        int motor_num = 0;
        switch (v.getId()) {
            case R.id.mc_M1testBtn:
                motor_num = 1;
                break;
            case R.id.mc_M2testBtn:
                motor_num = 2;
                break;
            case R.id.mc_M3testBtn:
                motor_num = 3;
                break;
            case R.id.mc_M4testBtn:
                motor_num = 4;
                break;
            case R.id.mc_M5testBtn:
                motor_num = 5;
                break;
            case R.id.mc_M6testBtn:
                motor_num = 6;
                break;
            case R.id.mc_M7testBtn:
                motor_num = 7;
                break;
            case R.id.mc_M8testBtn:
                motor_num = 8;
                break;
        }
        if((motor_num != 0) && getDrone().isConnected())
        {
            CommonApiUtils.doMotorTest(getDrone(), motor_num, thrVal, 2, null);
            Toast.makeText(getActivity(), "测试电机 "+ motor_num, Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getActivity(), "飞控未连接，无法执行操作", Toast.LENGTH_SHORT).show();
        }
    }


}
