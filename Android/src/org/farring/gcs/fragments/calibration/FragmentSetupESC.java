package org.farring.gcs.fragments.calibration;
import org.greenrobot.eventbus.EventBus;
import com.evenbus.AttributeEvent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.dronekit.core.drone.DroneInterfaces;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;

import org.farring.gcs.R;
import org.farring.gcs.dialogs.SlideToUnlockDialog;
import org.farring.gcs.fragments.helpers.BaseFragment;

public class FragmentSetupESC extends BaseFragment implements DroneInterfaces.OnParameterManagerListener{

    private ParameterManager parameterManager;
    private Parameter ESCalib;
    private Button ESCaliBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_escalibration, container, false);
        super.onViewCreated(view, savedInstanceState);
//
        parameterManager = getDrone().getParameterManager();

        ESCaliBtn = (Button) view.findViewById(R.id.btnESCali);
        ESCaliBtn.setEnabled(true);
        ESCaliBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doESCali(v);
            }
        });
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
    public void doESCali(View v) {

        int motor_num = 0;
        switch (v.getId()) {
            case R.id.btnESCali:
                if (getDrone().isConnected()) {
                    getESCaliConfirmation();
                    EventBus.getDefault().post(AttributeEvent.ESC_CALIBRATION);
                    Toast.makeText(getActivity(), "开始电调校准，请重启飞行器", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "飞控未连接，无法执行操作", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    private void getESCaliConfirmation() {
        final SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("执行电调校准", new Runnable() {
            @Override
            public void run() {
                ESCalib = parameterManager.getParameter("ESC_CALIBRATION");
                ESCalib.setValue(65);
                parameterManager.sendParameter(ESCalib);
            }
        });
        unlockDialog.show(getChildFragmentManager(), "滑动执行电调校准");
    }
}
