package org.farring.gcs.fragments.calibration;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dronekit.core.MAVLink.MavLinkStreamRates;
import com.dronekit.core.drone.DroneInterfaces;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;
import com.evenbus.AttributeEvent;
import com.rey.material.widget.Button;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseDialogFragment;
import org.farring.gcs.view.FillBar.FillBar;
import org.farring.gcs.view.RcStick.RcStick;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.farring.gcs.R.id.ch_5_text;
import static org.farring.gcs.R.id.ch_6_text;
import static org.farring.gcs.R.id.ch_7_text;
import static org.farring.gcs.R.id.ch_8_text;
// import static org.farring.gcs.R.id.dialog;
import static org.farring.gcs.R.id.roll_pitch_text;
import static org.farring.gcs.R.id.thr_yaw_text;

/**
 * Created by Linjieqiang on 2015-10-17.
 * 校准遥控器分为2个步骤
 * 1.全面拨动开关
 * 2.居中
 */
public class FragmentSetupRCCalibration extends BaseDialogFragment implements DroneInterfaces.OnParameterManagerListener {

    /**
     * Minimum threshold for the RC value.
     */
    private static final int RC_MIN = 1000;
    /**
     * Maximum threshold for the RC value.
     */
    private static final int RC_MAX = 2200;
    private static final String[] RCStr = {"CH 1 ", "CH 2 ", "CH 3 ", "CH 4 ", "CH 5", "CH 6", "CH 7", "CH 8"};
    private static int stepForCalibration = 0;
    @BindView(R.id.fillBar_throttle)
    FillBar fillBarCh3;
    @BindView(R.id.fillBar_yaw)
    FillBar fillBarCh4;
    @BindView(R.id.fillBar_pitch)
    FillBar fillBarCh2;
    @BindView(R.id.fillBar_roll)
    FillBar fillBarCh1;
    @BindView(R.id.fillBar_ch_5)
    FillBar fillBarCh5;
    @BindView(R.id.fillBar_ch_6)
    FillBar fillBarCh6;
    @BindView(R.id.fillBar_ch_7)
    FillBar fillBarCh7;
    @BindView(R.id.fillBar_ch_8)
    FillBar fillBarCh8;
    @BindView(thr_yaw_text)
    TextView thrYawText;
    @BindView(roll_pitch_text)
    TextView rollPitchText;
    @BindView(R.id.stickLeft)
    RcStick stickLeft;
    @BindView(R.id.stickRight)
    RcStick stickRight;
    @BindView(ch_5_text)
    TextView ch5Text;
    @BindView(ch_6_text)
    TextView ch6Text;
    @BindView(ch_7_text)
    TextView ch7Text;
    @BindView(ch_8_text)
    TextView ch8Text;
    @BindView(R.id.step4Calibration)
    TextView step4Calibration;
    @BindView(R.id.Btn_Start)
    Button BtnStart;

    private ParameterManager parameterManager;

    private int data[] = new int[8];
    private int cMin[] = new int[8];
    private int cMid[] = new int[8];
    private int cMax[] = new int[8];
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_rc_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        parameterManager = getDrone().getParameterManager();

        fillBarCh1.setup(RC_MAX, RC_MIN);
        fillBarCh2.setup(RC_MAX, RC_MIN);
        fillBarCh3.setup(RC_MAX, RC_MIN);
        fillBarCh4.setup(RC_MAX, RC_MIN);
        fillBarCh5.setup(RC_MAX, RC_MIN);
        fillBarCh6.setup(RC_MAX, RC_MIN);
        fillBarCh7.setup(RC_MAX, RC_MIN);
        fillBarCh8.setup(RC_MAX, RC_MIN);
    }

    @Override
    public void onStart() {
        super.onStart();
        // 添加参数对象监听器
        parameterManager.setParameterListener(this);
        // 打开遥控数据流
        MavLinkStreamRates.setupStreamRates(getDrone().getMavClient(), getDrone().getSysid(), getDrone().getCompid(), 1, 0, 1, 1, 1, 50, 0, 0);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case RC_IN:
                updatePanelInfo();
                break;
        }
    }
    private boolean rcinValid(int rcin)
    {
        if((rcin > RC_MIN) && (rcin < RC_MAX))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    private void updatePanelInfo() {
        data = getDrone().getVehicleRC().in;

        if(rcinValid(data[0]))
        {
            fillBarCh1.setValue(data[0]);
        }
        if(rcinValid(data[1]))
        {
            fillBarCh2.setValue(data[1]);
        }
        if(rcinValid(data[2]))
        {
            fillBarCh3.setValue(data[2]);
        }
        if(rcinValid(data[3]))
        {
            fillBarCh4.setValue(data[3]);
        }
        if(rcinValid(data[4]))
        {
            fillBarCh5.setValue(data[4]);
        }
        if(rcinValid(data[5]))
        {
            fillBarCh6.setValue(data[5]);
        }
        if(rcinValid(data[6]))
        {
            fillBarCh7.setValue(data[6]);
        }
        if(rcinValid(data[7]))
        {
            fillBarCh8.setValue(data[7]);
        }

        rollPitchText.setText("左右: " + Integer.toString(data[0]) + "\n前后: " + Integer.toString(data[1]));
        thrYawText.setText("油门: " + Integer.toString(data[2]) + "\n航向: " + Integer.toString(data[3]));
        ch5Text.setText("摇杆 5: " + Integer.toString(data[4]));
        ch6Text.setText("摇杆 6: " + Integer.toString(data[5]));
        ch7Text.setText("摇杆 7: " + Integer.toString(data[6]));
        ch8Text.setText("摇杆 8: " + Integer.toString(data[7]));

        float x, y;
        x = (data[3] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
        y = (data[2] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
        stickLeft.setPosition(x, y);

        x = (data[0] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
        y = (data[1] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
        stickRight.setPosition(x, -y);
    }

    private String getCalibrationStr() {
        String txt = "遥控通道\t\t最小值\t\t中值\t\t最大值";

        cMin[0] = fillBarCh1.getMinValue();
        cMin[1] = fillBarCh2.getMinValue();
        cMin[2] = fillBarCh3.getMinValue();
        cMin[3] = fillBarCh4.getMinValue();
        cMin[4] = fillBarCh5.getMinValue();
        cMin[5] = fillBarCh6.getMinValue();
        cMin[6] = fillBarCh7.getMinValue();
        cMin[7] = fillBarCh8.getMinValue();

        cMax[0] = fillBarCh1.getMaxValue();
        cMax[1] = fillBarCh2.getMaxValue();
        cMax[2] = fillBarCh3.getMaxValue();
        cMax[3] = fillBarCh4.getMaxValue();
        cMax[4] = fillBarCh5.getMaxValue();
        cMax[5] = fillBarCh6.getMaxValue();
        cMax[6] = fillBarCh7.getMaxValue();
        cMax[7] = fillBarCh8.getMaxValue();

        if (data != null)
            cMid = data;

        for (int i = 0; i < 8; i++) {
            if((i == 2) || (i > 3))
            {
                cMid[i] = (cMin[i] + cMax[i])/2;
            }
            txt += "\n" + RCStr[i] + "\t";
            txt += "\t" + String.valueOf(cMin[i]) + "\t";
            txt += "\t" + String.valueOf(cMid[i]) + "\t";
            txt += "\t" + String.valueOf(cMax[i]);
        }

        return txt;
    }

    public void sendCalibrationData() {
        if (getDrone().isConnected()) {
            Parameter RcParam, escaliParam;

            for (int i = 0; i < 8; i++) {
                // 设置最小值
                RcParam = parameterManager.getParameter("RC" + String.valueOf(i + 1) + "_MIN");
                RcParam.setValue(cMin[i]);
                parameterManager.sendParameter(RcParam);

                // 设置最大值
                RcParam = parameterManager.getParameter("RC" + String.valueOf(i + 1) + "_MAX");
                RcParam.setValue(cMax[i]);
                parameterManager.sendParameter(RcParam);

                // 设置中间值
                RcParam = parameterManager.getParameter("RC" + String.valueOf(i + 1) + "_TRIM");
                RcParam.setValue(cMid[i]);
                parameterManager.sendParameter(RcParam);
            }
            // notify RC cali done
            escaliParam = parameterManager.getParameter("ESC_CALIBRATION");
            escaliParam.setValue(0x51);
            parameterManager.sendParameter(escaliParam);
            Toast.makeText(getActivity(), "遥控器校准成功!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "请先连接飞控，然后校准", Toast.LENGTH_SHORT).show();
        }

        setFillBarShowMinMax(false);
    }

    private void setFillBarShowMinMax(boolean b) {
        fillBarCh1.setShowMinMax(b);
        fillBarCh2.setShowMinMax(b);
        fillBarCh3.setShowMinMax(b);
        fillBarCh4.setShowMinMax(b);
        fillBarCh5.setShowMinMax(b);
        fillBarCh6.setShowMinMax(b);
        fillBarCh7.setShowMinMax(b);
        fillBarCh8.setShowMinMax(b);
    }

    public void startRCalib()
    {
        if (!getDrone().isConnected())
        {
            Toast.makeText(getActivity(), "飞行器未连接，无法校准", Toast.LENGTH_SHORT).show();
            return;
        }
        Parameter escaliParam = parameterManager.getParameter("ESC_CALIBRATION");
        escaliParam.setValue(0x61);
        parameterManager.sendParameter(escaliParam);
        switch (stepForCalibration) {
            case 0:
                stepForCalibration = 1;
                step4Calibration.setText("将摇杆在整个行程范围内移动数次，并拨动开关、控制杆、旋钮至全部可能的位置");
                BtnStart.setText("下一步");
                break;

            case 1:
                stepForCalibration = 2;
                step4Calibration.setText("将油门摇杆放在最低位置，其他摇杆、开关、控制杆、旋钮在中位");
                BtnStart.setText("发送校准值");
                break;

            case 2:
                new MaterialDialog.Builder(getActivity())
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title("遥控器校准结果确认")
                        .content(getCalibrationStr())
                        .positiveText("确认发送")
                        .negativeText("重新校准")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            // 应用方法
                                            sendCalibrationData();
                                            EventBus.getDefault().post(AttributeEvent.RC_CALIBRATION_DONE);
                                            Toast.makeText(getActivity(), "遥控器校准完成", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                        )
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            // 应用方法
                                            stepForCalibration = 0;
                                            startRCalib();
                                            Toast.makeText(getActivity(), "遥控器重新校准开始", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                        ).show();

                break;
        }
    }
    @OnClick(R.id.Btn_Start)
    public void onClick() {
        startRCalib();
    }

    // 开始进度
    private void startProgress() {
        final Activity activity = getActivity();
        if (activity == null)
            return;
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(R.string.refreshing_parameters);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    // 更新进度条（进度，总进度）
    private void updateProgress(int progress, int max) {
        if (progressDialog == null) {
            startProgress();
        }
        if (progressDialog.isIndeterminate()) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(max);
        }
        progressDialog.setProgress(progress);
    }

    // 停止进度条
    private void stopProgress() {
        // dismiss progress dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onBeginReceivingParameters() {
        startProgress();
    }

    @Override
    public void onParameterReceived(Parameter parameter, int index, int count) {
        final int defaultValue = -1;
        if (index != defaultValue && count != defaultValue) {
            updateProgress(index, count);
        }
    }

    @Override
    public void onEndReceivingParameters() {
        stopProgress();
        updatePanelInfo();
    }
}
