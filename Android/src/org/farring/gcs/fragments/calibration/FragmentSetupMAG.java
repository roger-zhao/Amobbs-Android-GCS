package org.farring.gcs.fragments.calibration;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.ardupilotmega.msg_mag_cal_progress;
import com.MAVLink.ardupilotmega.msg_mag_cal_report;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.dronekit.core.drone.DroneInterfaces;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;
import com.dronekit.core.drone.variables.calibration.MagnetometerCalibrationImpl;
import com.dronekit.core.drone.variables.calibration.MagnetometerCalibrationImpl.OnMagnetometerCalibrationListener;
import com.evenbus.AttributeEvent;
import com.rey.material.widget.CheckBox;
import com.rey.material.widget.Spinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseDialogFragment;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.grantland.widget.AutofitTextView;

/**
 * Created by Linjieqiang on 2015-10-17.
 */
public class FragmentSetupMAG extends BaseDialogFragment implements OnMagnetometerCalibrationListener, DroneInterfaces.OnParameterManagerListener {

    @BindView(R.id.compass1NumberProgressBar)
    NumberProgressBar compass1ProgressBar;
    @BindView(R.id.compass2NumberProgressBar)
    NumberProgressBar compass2ProgressBar;
    @BindView(R.id.stateText)
    AutofitTextView stateText;
    @BindView(R.id.checkBox_compassAll)
    CheckBox checkBoxCompassAll;
    @BindView(R.id.checkBox_AutoDeclination)
    CheckBox checkBoxAutoDeclination;
    @BindView(R.id.checkBox_AutoOffset)
    CheckBox checkBoxAutoOffset;
    @BindView(R.id.Checkbox_Compass1)
    CheckBox CheckboxCompass1;
    @BindView(R.id.Checkbox_Compass1_External)
    CheckBox CheckboxCompass1External;
    @BindView(R.id.Spinner_Compass1External)
    Spinner SpinnerCompass1External;
    @BindView(R.id.TextView_Compass1_Offset)
    TextView TextViewCompass1Offset;
    @BindView(R.id.Checkbox_Compass2)
    CheckBox CheckboxCompass2;
    @BindView(R.id.Checkbox_Compass2_External)
    CheckBox CheckboxCompass2External;
    @BindView(R.id.Spinner_Compass2External)
    Spinner SpinnerCompass2External;
    @BindView(R.id.TextView_Compass2_Offset)
    TextView TextViewCompass2Offset;
    @BindView(R.id.RadioBtn_CompassMain)
    RadioGroup RadioBtnCompassMain;


    private MagnetometerCalibrationImpl magCalibration;
    private int magCaliDoneCnt = 0;
    /**
     * 参数管理器
     */
    private ParameterManager parameterManager;
    private Parameter
            enableCompassAll,// 启用全部磁罗盘
            enableAutoOffset,// 自动获取罗盘偏移量
            enableAutoDeclination,// 自动磁偏角
            enableCompass1,// 启用罗盘1号
            enableCompass2,// 启用罗盘2号
            enableCompass1External,// 启用罗盘1号外部安装
            enableCompass2External,// 启用罗盘2号外部安装
            compass1ExternalOrinentation,// 罗盘1号外部安装方向
            compass2ExternalOrinentation,// 罗盘2号外部安装方向
            compass1OffsetX, compass1OffsetY, compass1OffsetZ,// 罗盘1号偏差值
            compass2OffsetX, compass2OffsetY, compass2OffsetZ,// 罗盘2号偏差值
            compassPrimary;// 选择主罗盘

    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_mag_main_new, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parameterManager = getDrone().getParameterManager();

        // 数据适配器
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_setup_item, getResources().getStringArray(R.array.CompassOrientation));
        SpinnerCompass1External.setAdapter(adapter);
        SpinnerCompass2External.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        // 添加参数对象监听器
        parameterManager.setParameterListener(this);
        magCalibration = getDrone().getMagnetometerCalibration();
        magCalibration.setListener(this);

        getAllParameter();
        updatePanelInfo();
        setupParamsListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        // 移除磁罗盘监听事件
        magCalibration.setListener(null);
        cancelCalibration();
    }

    /**
     * 获取全部的磁罗盘值
     */
    private void getAllParameter() {
        // 设置这个值为启用(1)来使用磁罗盘. 设置这个值为禁用(0)将不使用磁罗盘.
        // enableCompassAll = parameterManager.getParameter("MAG_ENABLE");

        // 启用或禁用罗盘偏移的自动获取
        // enableAutoOffset = parameterManager.getParameter("COMPASS_LEARN");

        // 自动磁偏角
        // enableAutoDeclination = parameterManager.getParameter("COMPASS_AUTODEC");

        // 选择主罗盘
        // compassPrimary = parameterManager.getParameter("COMPASS_PRIMARY");

        // 使能罗盘
        // enableCompass1 = parameterManager.getParameter("COMPASS_USE");
        // enableCompass2 = parameterManager.getParameter("COMPASS_USE2");

        // 罗盘1号的偏差值
        compass1OffsetX = parameterManager.getParameter("COMPASS_OFS_X");
        compass1OffsetY = parameterManager.getParameter("COMPASS_OFS_Y");
        compass1OffsetZ = parameterManager.getParameter("COMPASS_OFS_Z");

        // 罗盘2号的偏差值
        // compass2OffsetX = parameterManager.getParameter("COMPASS_OFS2_X");
        // compass2OffsetY = parameterManager.getParameter("COMPASS_OFS2_Y");
        // compass2OffsetZ = parameterManager.getParameter("COMPASS_OFS2_Z");
//
        // // 罗盘使能外安装
        // enableCompass1External = parameterManager.getParameter("COMPASS_EXTERNAL");
        // enableCompass2External = parameterManager.getParameter("COMPASS_EXTERN2");
//
        // // 罗盘安装方向
        // compass1ExternalOrinentation = parameterManager.getParameter("COMPASS_ORIENT");
        // compass2ExternalOrinentation = parameterManager.getParameter("COMPASS_ORIENT2");
    }

    /**
     * 更新界面
     */
    private void updatePanelInfo() {
        if (enableCompassAll != null)
            checkBoxCompassAll.setChecked(enableCompassAll.getValue() == 1);

        if (enableAutoDeclination != null)
            checkBoxAutoDeclination.setChecked(enableAutoDeclination.getValue() == 1);
        if (enableAutoOffset != null)
            checkBoxAutoOffset.setChecked(enableAutoOffset.getValue() == 1);

        if (enableCompass1External != null)
            CheckboxCompass1External.setChecked(enableCompass1External.getValue() == 1);
        if (enableCompass2External != null)
            CheckboxCompass2External.setChecked(enableCompass2External.getValue() == 1);

        if (enableCompass1 != null)
            CheckboxCompass1.setChecked(enableCompass1.getValue() == 1);
        if (enableCompass2 != null)
            CheckboxCompass2.setChecked(enableCompass2.getValue() == 1);

        if (compass1OffsetX != null && compass1OffsetY != null && compass1OffsetZ != null)
            TextViewCompass1Offset.setText("" +
                    "X:" + (float) compass1OffsetX.getValue() +
                    ";Y:" + (float) compass1OffsetY.getValue() +
                    ";Z:" + (float) compass1OffsetZ.getValue());

        // if (compass2OffsetX != null && compass2OffsetY != null && compass2OffsetZ != null)
        //     TextViewCompass2Offset.setText("" +
        //             "X:" + (float) compass2OffsetX.getValue() +
        //             ";Y:" + (float) compass2OffsetY.getValue() +
        //             ";Z:" + (float) compass2OffsetZ.getValue());
//
        // if (compass1ExternalOrinentation != null)
        //     SpinnerCompass1External.setSelection((int) compass1ExternalOrinentation.getValue());
        // if (compass2ExternalOrinentation != null)
        //     SpinnerCompass2External.setSelection((int) compass2ExternalOrinentation.getValue());

        if (compassPrimary != null)
            if (compassPrimary.getValue() == 0) {
                RadioBtnCompassMain.check(R.id.RadioBtn_Compass1);
            } else {
                RadioBtnCompassMain.check(R.id.RadioBtn_Compass2);
            }
    }

    private void setupParamsListener() {
        checkBoxCompassAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enableCompassAll != null) {
                    enableCompassAll.setValue(isChecked ? 1 : 0);
                    parameterManager.sendParameter(enableCompassAll);
                }
            }
        });

        checkBoxAutoDeclination.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enableAutoDeclination != null) {
                    enableAutoDeclination.setValue(isChecked ? 1 : 0);
                    parameterManager.sendParameter(enableAutoDeclination);
                }
            }
        });

        checkBoxAutoOffset.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enableAutoOffset != null) {
                    enableAutoOffset.setValue(isChecked ? 1 : 0);
                    parameterManager.sendParameter(enableAutoOffset);
                }
            }
        });

        CheckboxCompass1External.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enableCompass1External != null) {
                    // enableCompass1External.setValue(isChecked ? 1 : 0);
                    enableCompass1External.setValue(0);
                    parameterManager.sendParameter(enableCompass1External);
                }
            }
        });

        CheckboxCompass2External.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enableCompass2External != null) {
                    // enableCompass2External.setValue(isChecked ? 1 : 0);
                    enableCompass2External.setValue( 0);
                    parameterManager.sendParameter(enableCompass2External);
                }
            }
        });

        CheckboxCompass1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enableCompass1 != null) {
                    enableCompass1.setValue(isChecked ? 1 : 0);
                    parameterManager.sendParameter(enableCompass1);
                }
            }
        });

        CheckboxCompass2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (enableCompass2 != null) {
                    // enableCompass2.setValue(isChecked ? 1 : 0);
                    enableCompass2.setValue(0);
                    parameterManager.sendParameter(enableCompass2);
                }
            }
        });


        SpinnerCompass1External.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(Spinner parent, View view, int position, long id) {
                if (compass1ExternalOrinentation != null) {
                    compass1ExternalOrinentation.setValue(position);
                    parameterManager.sendParameter(compass1ExternalOrinentation);
                }
            }
        });

        SpinnerCompass2External.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(Spinner parent, View view, int position, long id) {
                if (compass2ExternalOrinentation != null) {
                    compass2ExternalOrinentation.setValue(position);
                    parameterManager.sendParameter(compass2ExternalOrinentation);
                }
            }
        });

        RadioBtnCompassMain.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (compassPrimary != null) {
                    compassPrimary.setValue(0);
                    parameterManager.sendParameter(compassPrimary);
                }
            }
        });
    }


    // 取消校准（此刻界面做出调整）
    @Override
    public void onCalibrationCancelled() {
        stateText.setText("校准取消");
        magCaliDoneCnt = 0;
    }

    // 处理中
    @Override
    public void onCalibrationProgress(msg_mag_cal_progress msgProgress) {
        stateText.setText("正在校准指南针，请稍候……");
        switch (msgProgress.compass_id) {
            case 0:
                compass1ProgressBar.setProgress(msgProgress.completion_pct);
                break;

            case 1:
                // compass2ProgressBar.setProgress(msgProgress.completion_pct);
                break;
        }
    }

    // 校准完成【移植完成】
    @Override
    public void onCalibrationCompleted(msg_mag_cal_report msgReport) {
        // 获取偏差值
        double[] offsets = new double[]{msgReport.ofs_x, msgReport.ofs_y, msgReport.ofs_z};
        stateText.setText("校准成功:" + Arrays.toString(offsets));
        if(magCaliDoneCnt++ < 2)
        {
            EventBus.getDefault().post(AttributeEvent.MAG_CALIBRATION_DONE);
            Toast.makeText(getActivity(), "指南针校准完成", Toast.LENGTH_SHORT).show();
        }
        // 清零进度条
        compass1ProgressBar.setProgress(0);
        // compass2ProgressBar.setProgress(0);
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTED:
                break;

            case STATE_DISCONNECTED:
                cancelCalibration();
                break;
        }
    }

    // 取消校准
    private void cancelCalibration() {
        if (getDrone().isConnected() && !getDrone().getState().isFlying()) {
            magCalibration.cancelCalibration();
        }
        // 清零进度条
        compass1ProgressBar.setProgress(0);
        // compass2ProgressBar.setProgress(0);
    }

    @OnClick({R.id.Btn_Start, R.id.Btn_Cancel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.Btn_Start:
                // 【开始校准磁罗盘】
                if (getDrone().isConnected() && !getDrone().getState().isFlying()) {
                    /**
                     * Start the magnetometer calibration process
                     * @param retryOnFailure if true, automatically retry the magnetometer calibration if it fails
                     * @param saveAutomatically if true, save the calibration automatically without user input.
                     * @param startDelay positive delay in seconds before starting the calibration
                     */
                    magCalibration.startCalibration(true, true, 10);
                    magCaliDoneCnt = 0;
                } else {
                    Toast.makeText(getActivity(), "飞行器未连接，无法校准", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.Btn_Cancel:
                cancelCalibration();
                break;
        }
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
        getAllParameter();
        updatePanelInfo();
        setupParamsListener();
    }
}
