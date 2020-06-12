package org.farring.gcs.fragments.calibration;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.dronekit.core.MAVLink.MavLinkStreamRates;
import com.dronekit.core.drone.DroneInterfaces;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseDialogFragment;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Linjieqiang on 2015-10-17.
 */
public class FragmentSetupRCMode extends BaseDialogFragment implements DroneInterfaces.OnParameterManagerListener {

    /**
     * 初始化变量
     */
    private CheckBox[] chkbxSimple = new CheckBox[6];
    private CheckBox[] chkbxSuperSimple = new CheckBox[6];
    private Spinner[] pwmSpinners = new Spinner[6];
    private TableRow[] layoutPWM = new TableRow[6];
    private TextView textPWMRange, textPWMCurrent;

    private int[] pwm = {1230, 1360, 1490, 1620, 1750};
    private int[] flightModeIndex = {1, 2, 4, 8, 16, 32};
    private String[] listPWM;

    private int[] valueFM;
    private String[] stringFM;


    /**
     * 参数管理器
     */
    private ParameterManager parameterManager;
    private Parameter ParamFlyMode1, ParamFlyMode2, ParamFlyMode3, ParamFlyMode4, ParamFlyMode5, ParamFlyMode6, ParamSimple, ParamSuperSimple;

    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_fm_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        parameterManager = getDrone().getParameterManager();

        listPWM = getResources().getStringArray(R.array.FligthMode_PWM_Range);

        textPWMRange = (TextView) v.findViewById(R.id.textViewPWMRange);
        textPWMCurrent = (TextView) v.findViewById(R.id.textViewPWM);

        pwmSpinners[0] = (Spinner) v.findViewById(R.id.spinnerFM1);
        pwmSpinners[1] = (Spinner) v.findViewById(R.id.spinnerFM2);
        pwmSpinners[2] = (Spinner) v.findViewById(R.id.spinnerFM3);
        pwmSpinners[3] = (Spinner) v.findViewById(R.id.spinnerFM4);
        pwmSpinners[4] = (Spinner) v.findViewById(R.id.spinnerFM5);
        pwmSpinners[5] = (Spinner) v.findViewById(R.id.spinnerFM6);

        chkbxSimple[0] = (CheckBox) v.findViewById(R.id.checkBoxFM1);
        chkbxSimple[1] = (CheckBox) v.findViewById(R.id.checkBoxFM2);
        chkbxSimple[2] = (CheckBox) v.findViewById(R.id.checkBoxFM3);
        chkbxSimple[3] = (CheckBox) v.findViewById(R.id.checkBoxFM4);
        chkbxSimple[4] = (CheckBox) v.findViewById(R.id.checkBoxFM5);
        chkbxSimple[5] = (CheckBox) v.findViewById(R.id.checkBoxFM6);

        chkbxSuperSimple[0] = (CheckBox) v.findViewById(R.id.checkBoxFMS1);
        chkbxSuperSimple[1] = (CheckBox) v.findViewById(R.id.checkBoxFMS2);
        chkbxSuperSimple[2] = (CheckBox) v.findViewById(R.id.checkBoxFMS3);
        chkbxSuperSimple[3] = (CheckBox) v.findViewById(R.id.checkBoxFMS4);
        chkbxSuperSimple[4] = (CheckBox) v.findViewById(R.id.checkBoxFMS5);
        chkbxSuperSimple[5] = (CheckBox) v.findViewById(R.id.checkBoxFMS6);

        layoutPWM[0] = (TableRow) v.findViewById(R.id.layoutFM1);
        layoutPWM[1] = (TableRow) v.findViewById(R.id.layoutFM2);
        layoutPWM[2] = (TableRow) v.findViewById(R.id.layoutFM3);
        layoutPWM[3] = (TableRow) v.findViewById(R.id.layoutFM4);
        layoutPWM[4] = (TableRow) v.findViewById(R.id.layoutFM5);
        layoutPWM[5] = (TableRow) v.findViewById(R.id.layoutFM6);

        setupSpinners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // 打开遥控数据流
        MavLinkStreamRates.setupStreamRates(getDrone().getMavClient(), getDrone().getSysid(), getDrone().getCompid(), 1, 0, 1, 1, 1, 50, 0, 0);
        // 添加参数对象监听器
        parameterManager.setParameterListener(this);
        updatePanelInfo();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void setupSpinners() {
        String pairs[] = getResources().getStringArray(R.array.FligthMode_CopterV3_1);
        valueFM = new int[pairs.length];
        stringFM = new String[pairs.length];

        int i = 0;
        for (String item : pairs) {
            String pair[] = item.split(";");
            valueFM[i] = Integer.parseInt(pair[0]);
            stringFM[i] = pair[1];
            i++;
        }

        // 数据适配器
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_setup_item, stringFM);
        // adapter.setDropDownViewResource(R.layout.spinner_setup_item_dropdown);

        for (Spinner spinner : pwmSpinners)
            spinner.setAdapter(adapter);
    }


    private int getPWMRangeIndex(int pwmValue) {
        if (pwmValue <= pwm[0])
            return 0;
        else if (pwmValue > pwm[0] && pwmValue <= pwm[1])
            return 1;
        else if (pwmValue > pwm[1] && pwmValue <= pwm[2])
            return 2;
        else if (pwmValue > pwm[2] && pwmValue <= pwm[3])
            return 3;
        else if (pwmValue > pwm[3] && pwmValue <= pwm[4])
            return 4;
        else if (pwmValue >= pwm[4])
            return 5;

        return -1;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case RC_IN:
                int pwmIn = getDrone().getVehicleRC().in[4];
                int pwmId = getPWMRangeIndex(pwmIn);
                textPWMCurrent.setText(String.format("PWM In : %d", pwmIn));
                textPWMRange.setText("Flight Mode #" + String.valueOf(pwmId + 1) + " (" + listPWM[pwmId] + ")");

                // 更新布局文件
                for (LinearLayout layout : layoutPWM)
                    layout.setBackgroundColor(0);
                if (pwmId > -1)
                    layoutPWM[pwmId].setBackgroundColor(getResources().getColor(R.color.red));
                break;
        }
    }

    protected int getSpinnerIndexFromValue(int value, int[] valueList) {
        for (int i = 0; i < valueList.length; i++) {
            if (valueList[i] == value)
                return i;
        }

        return -1;
    }

    protected void updatePanelInfo() {
        int fmData;
        if (getDrone().isConnected()) {
            ParamFlyMode1 = parameterManager.getParameter("FLTMODE1");
            if (ParamFlyMode1 != null) {
                fmData = (int) ParamFlyMode1.getValue();
                pwmSpinners[0].setSelection(getSpinnerIndexFromValue(fmData, valueFM), true);
            }

            ParamFlyMode2 = parameterManager.getParameter("FLTMODE2");
            if (ParamFlyMode2 != null) {
                fmData = (int) ParamFlyMode2.getValue();
                pwmSpinners[1].setSelection(getSpinnerIndexFromValue(fmData, valueFM), true);
            }

            ParamFlyMode3 = parameterManager.getParameter("FLTMODE3");
            if (ParamFlyMode3 != null) {
                fmData = (int) ParamFlyMode3.getValue();
                pwmSpinners[2].setSelection(getSpinnerIndexFromValue(fmData, valueFM), true);
            }

            ParamFlyMode4 = parameterManager.getParameter("FLTMODE4");
            if (ParamFlyMode4 != null) {
                fmData = (int) ParamFlyMode4.getValue();
                pwmSpinners[3].setSelection(getSpinnerIndexFromValue(fmData, valueFM), true);
            }

            ParamFlyMode5 = parameterManager.getParameter("FLTMODE5");
            if (ParamFlyMode5 != null) {
                fmData = (int) ParamFlyMode5.getValue();
                pwmSpinners[4].setSelection(getSpinnerIndexFromValue(fmData, valueFM), true);
            }

            ParamFlyMode6 = parameterManager.getParameter("FLTMODE6");
            if (ParamFlyMode6 != null) {
                fmData = (int) ParamFlyMode6.getValue();
                pwmSpinners[5].setSelection(getSpinnerIndexFromValue(fmData, valueFM), true);
            }

            ParamSimple = parameterManager.getParameter("SIMPLE");
            if (ParamSimple != null) {
                for (int i = 0; i < 6; i++) {
                    fmData = (int) ParamSimple.getValue();
                    chkbxSimple[i].setChecked((fmData & flightModeIndex[i]) == flightModeIndex[i]);
                }
            }

            ParamSuperSimple = parameterManager.getParameter("SUPER_SIMPLE");
            if (ParamSuperSimple != null) {
                for (int i = 0; i < 6; i++) {
                    fmData = (int) ParamSimple.getValue();
                    chkbxSuperSimple[i].setChecked((fmData & flightModeIndex[i]) == flightModeIndex[i]);
                }
            }
        } else {
            Toast.makeText(getActivity(), "飞行器未连接，无法执行操作", Toast.LENGTH_SHORT).show();
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

    private void changeParametersValues(int dataFM[]) {
        if (getDrone().isConnected()) {
            ParamFlyMode1 = parameterManager.getParameter("FLTMODE1");
            if (ParamFlyMode1 != null) {
                ParamFlyMode1.setValue(dataFM[0]);
                parameterManager.sendParameter(ParamFlyMode1);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamFlyMode2 = parameterManager.getParameter("FLTMODE2");
            if (ParamFlyMode2 != null) {
                ParamFlyMode2.setValue(dataFM[1]);
                parameterManager.sendParameter(ParamFlyMode2);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamFlyMode3 = parameterManager.getParameter("FLTMODE3");
            if (ParamFlyMode3 != null) {
                ParamFlyMode3.setValue(dataFM[2]);
                parameterManager.sendParameter(ParamFlyMode3);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamFlyMode4 = parameterManager.getParameter("FLTMODE4");
            if (ParamFlyMode4 != null) {
                ParamFlyMode4.setValue(dataFM[3]);
                parameterManager.sendParameter(ParamFlyMode4);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamFlyMode5 = parameterManager.getParameter("FLTMODE5");
            if (ParamFlyMode5 != null) {
                ParamFlyMode5.setValue(dataFM[4]);
                parameterManager.sendParameter(ParamFlyMode5);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamFlyMode6 = parameterManager.getParameter("FLTMODE6");
            if (ParamFlyMode6 != null) {
                ParamFlyMode6.setValue(dataFM[5]);
                parameterManager.sendParameter(ParamFlyMode6);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamSimple = parameterManager.getParameter("SIMPLE");
            if (ParamSimple != null) {
                ParamSimple.setValue(dataFM[6]);
                parameterManager.sendParameter(ParamSimple);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamSuperSimple = parameterManager.getParameter("SUPER_SIMPLE");
            if (ParamSuperSimple != null) {
                ParamSuperSimple.setValue(dataFM[7]);
                parameterManager.sendParameter(ParamSuperSimple);
            } else {
                Toast.makeText(getActivity(), "部分参数写入失败~!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getActivity(), "模式设置成功!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "飞行器未连接，无法执行操作", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 发送数据
     */
    @OnClick(R.id.Btn_Start)
    public void onClick() {
        // 临时变量
        int cnt = 0;
        int dataFM[] = new int[8];

        // Read all spinners value
        for (Spinner spinner : pwmSpinners) {
            dataFM[cnt] = valueFM[spinner.getSelectedItemPosition()];
            cnt++;
        }

        // read SIMPLE MODE check boxes and create bit value
        cnt = 0;
        for (CheckBox chkbx : chkbxSimple) {
            if (chkbx.isChecked()) {
                dataFM[6] += flightModeIndex[cnt];
            }
            cnt++;
        }

        // read SUPER SIMPLE MODE check boxes and create bit value
        cnt = 0;
        for (CheckBox chkbx : chkbxSuperSimple) {
            if (chkbx.isChecked()) {
                dataFM[7] += flightModeIndex[cnt];
            }
            cnt++;
        }

        // 应用方法
        changeParametersValues(dataFM);
    }
}
