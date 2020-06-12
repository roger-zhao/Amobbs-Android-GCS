package org.farring.gcs.fragments.calibration;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.SimpleCommandListener;
import com.dronekit.core.drone.variables.State;
import com.evenbus.AttributeEvent;
import com.evenbus.TTSEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;

public class FragmentSetupIMU extends BaseFragment {

    private final static long TIMEOUT_MAX = 60000l; //ms
    private final static long UPDATE_TIMEOUT_PERIOD = 100l; //ms
    private static final String EXTRA_UPDATE_TIMESTAMP = "extra_update_timestamp";
    private final Handler handler = new Handler();
    private long updateTimestamp;

    private int calibration_step = 0;
    private TextView textViewStep;
    private TextView textViewOffset;
    private TextView textViewScaling;
    private TextView textViewTimeOut;
    private ProgressBar pbTimeOut;
    private String timeLeftStr;
    private Drawable drawableGood, drawableWarning, drawablePoor;
    private Button btnStep, btnCancel;
    private TextView textDesc;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(this);
            updateTimeOutProgress();
            handler.postDelayed(this, UPDATE_TIMEOUT_PERIOD);
        }
    };

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            // 校准IMU（处理MAV消息）
            case CALIBRATION_IMU: {
                processMAVMessage(getDrone().getCalibrationSetup().getMessage(), true);
                break;
            }
            case CALIBRATION_IMU_POS: {
                // Toast.makeText(getActivity(), "消息飞控校准步骤：", Toast.LENGTH_LONG).show();
                short fcStep = getDrone().getCalibrationSetup().getFCStep();
                processMAVMessage(fcStep, true);
                break;
            }
            // 连接成功
            case STATE_CONNECTED:
                if (calibration_step == 0) {
                    //Reset the screen, and enable the calibration button
                    resetCalibration();
                    btnStep.setEnabled(true);
                    btnCancel.setEnabled(true);
                }
                break;

            // 断开连接
            case STATE_DISCONNECTED:
                //Reset the screen, and disable the calibration button
                btnStep.setEnabled(false);
                btnCancel.setEnabled(false);
                resetCalibration();
                break;

            // 校准超时
            case CALIBRATION_IMU_TIMEOUT:
                if (getDrone().isConnected()) {
                    String calIMUMessage = getDrone().getCalibrationSetup().getMessage();
                    if (calIMUMessage != null)
                        relayInstructions(calIMUMessage);
                }
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_imu_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewStep = (TextView) view.findViewById(R.id.textViewIMUStep);
        textViewOffset = (TextView) view.findViewById(R.id.TextViewIMUOffset);
        textViewScaling = (TextView) view.findViewById(R.id.TextViewIMUScaling);
        textViewTimeOut = (TextView) view.findViewById(R.id.textViewIMUTimeOut);
        pbTimeOut = (ProgressBar) view.findViewById(R.id.progressBarTimeOut);

        textDesc = (TextView) view.findViewById(R.id.textViewDesc);

        btnStep = (Button) view.findViewById(R.id.buttonStep);
        btnStep.setEnabled(false);
        btnStep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCalibrationStep(calibration_step);
            }
        });

        btnCancel =  (Button) view.findViewById(R.id.buttonCancel);
        btnCancel.setEnabled(false);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelCalibration(v);
            }
        });
        pbTimeOut.setVisibility(View.INVISIBLE);
        textViewTimeOut.setVisibility(View.INVISIBLE);
        textViewOffset.setVisibility(View.INVISIBLE);
        textViewScaling.setVisibility(View.INVISIBLE);
        timeLeftStr = getString(R.string.setup_imu_timeleft);

        drawableGood = getResources().getDrawable(R.drawable.pstate_good);
        drawableWarning = getResources().getDrawable(R.drawable.pstate_warning);
        drawablePoor = getResources().getDrawable(R.drawable.pstate_poor);

        if (savedInstanceState != null) {
            updateTimestamp = savedInstanceState.getLong(EXTRA_UPDATE_TIMESTAMP);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_UPDATE_TIMESTAMP, updateTimestamp);
    }

    private void resetCalibration() {
        calibration_step = 0;
        updateDescription(calibration_step);
    }

    @Override
    public void onStart() {
        super.onStart();
        Drone drone = getDrone();
        State droneState = drone.getState();
        if (drone.isConnected() && !droneState.isFlying()) {
            btnStep.setEnabled(true);
            btnCancel.setEnabled(true);
            if (drone.getCalibrationSetup().isCalibrating()) {
                processMAVMessage(drone.getCalibrationSetup().getMessage(), false);
            } else {
                resetCalibration();
            }
        } else {
            btnStep.setEnabled(false);
            btnCancel.setEnabled(false);
            resetCalibration();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void processCalibrationStep(int step) {
        if (step == 0) {
            startCalibration();
            updateTimestamp = System.currentTimeMillis();
        } else if (step > 0 && step < 7) {
            sendAck(step);
        } else {
            calibration_step = 0;

            textViewStep.setText(R.string.setup_imu_step);

            textViewOffset.setVisibility(View.INVISIBLE);
            textViewScaling.setVisibility(View.INVISIBLE);

            updateDescription(calibration_step);
        }
    }

    public void updateDescription(int calibration_step) {
        int id;
        switch (calibration_step) {
            case 0:
                id = R.string.setup_imu_start;
                break;
            case 1:
                id = R.string.setup_imu_normal;
                break;
            case 2:
                id = R.string.setup_imu_left;
                break;
            case 3:
                id = R.string.setup_imu_right;
                break;
            case 4:
                id = R.string.setup_imu_nosedown;
                break;
            case 5:
                id = R.string.setup_imu_noseup;
                break;
            case 6:
                id = R.string.setup_imu_back;
                break;
            case 7:
                id = R.string.setup_imu_completed;
                break;
            default:
                return;
        }

        if (textDesc != null) {
            textDesc.setText(id);
        }

        if (btnStep != null) {
            if (calibration_step == 0)
                btnStep.setText(R.string.button_setup_calibrate);
            else if (calibration_step == 7)
                btnStep.setText(R.string.button_setup_done);
            else
                btnStep.setText(R.string.button_setup_next);
        }

        if (calibration_step == 7 || calibration_step == 0) {
            handler.removeCallbacks(runnable);

            pbTimeOut.setVisibility(View.INVISIBLE);
            textViewTimeOut.setVisibility(View.INVISIBLE);
        } else {
            handler.removeCallbacks(runnable);

            textViewTimeOut.setVisibility(View.VISIBLE);
            pbTimeOut.setIndeterminate(true);
            pbTimeOut.setVisibility(View.VISIBLE);
            handler.postDelayed(runnable, UPDATE_TIMEOUT_PERIOD);
        }
    }

    private void sendAck(int step) {
        Drone dpApi = getDrone();
        if (dpApi.isConnected()) {
            dpApi.getCalibrationSetup().sendAck(step);
        }
    }

    private void startCalibration() {
        Drone dpApi = getDrone();
        if (dpApi.isConnected()) {
            getDrone().getCalibrationSetup().startCalibration(new SimpleCommandListener() {
                @Override
                public void onError(int error) {
                    Toast.makeText(getActivity(), R.string.imu_calibration_start_error, Toast.LENGTH_LONG).show();
                }
            });
            getDrone().getCalibrationSetup().startCalibration(null);
            // Toast.makeText(getActivity(), R.string.imu_calibration_start_now, Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelCalibration(View v) {
        Drone dpApi = getDrone();
        if (dpApi.isConnected()) {
            getDrone().getCalibrationSetup().cancelCalibration();
        }
        Toast.makeText(getActivity(), R.string.ACC_cali_canceled, Toast.LENGTH_SHORT).show();
        resetCalibration();
    }


    private void processMAVMessage(String message, boolean updateTime) {
        if (message.contains("Place") || message.contains("Calibration")) {
            if (updateTime) {
                updateTimestamp = System.currentTimeMillis();
            }
            processOrientation(message);
        } else if (message.contains("Offsets")) {
            textViewOffset.setVisibility(View.VISIBLE);
            textViewOffset.setText(message);
        } else if (message.contains("Scaling")) {
            textViewScaling.setVisibility(View.VISIBLE);
            textViewScaling.setText(message);
        }
    }
    private void processMAVMessage(short fcstep, boolean updateTime) {
        processOrientation(fcstep);
    }

    private void processOrientation(String message) {
        String msg = "";
        if (message.contains("level"))
        {
            calibration_step = 1;
            msg = "请水平放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (message.contains("LEFT"))
        {
            calibration_step = 2;
            msg = "请将机头左面朝下放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (message.contains("RIGHT"))
        {
            calibration_step = 3;
            msg = "请将机头右面朝下放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (message.contains("DOWN"))
        {
            calibration_step = 4;
            msg = "请将机头朝下放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (message.contains("UP"))
        {
            calibration_step = 5;
            msg = "请将机头朝上放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (message.contains("BACK"))
        {
            calibration_step = 6;
            msg = "请倒置飞行器（正面朝下），静待1到2秒后，按下一步继续校准";
        }
        else if (message.contains("Calibration successful"))
        {
            calibration_step = 7;
            msg = "校准成功";
        }
        else if (message.contains("Calibration FAILED")) {
            calibration_step = 0;
            msg = "校准失败";
        }

        relayInstructions(msg);

        textViewStep.setText(msg);

        updateDescription(calibration_step);
    }
    private void processOrientation(short fcstep) {

        if((fcstep == calibration_step)
                || !(fcstep > 0 && fcstep < 7)
                )
        {
            return;
        }
        // Toast.makeText(getActivity(), "FC：" + fcstep + ", GCS: " + calibration_step , Toast.LENGTH_SHORT).show();
        String msg = "";
        calibration_step = fcstep;
        if (fcstep == 1)
        {
            msg = "请水平放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (fcstep == 2)
        {
            msg = "请将机头左面朝下放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (fcstep == 3)
        {
            msg = "请将机头右面朝下放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (fcstep == 4)
        {
            msg = "请将机头朝下放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (fcstep == 5)
        {
            msg = "请将机头朝上放置飞行器，静待1到2秒后，按下一步继续校准";
        }
        else if (fcstep == 6)
        {
            msg = "请倒置飞行器（正面朝下），静待1到2秒后，按下一步继续校准";
        }

        relayInstructions(msg);

        textViewStep.setText(msg);

        updateDescription(calibration_step);
    }

    private void relayInstructions(String instructions) {
        final Activity activity = getActivity();
        if (activity == null) return;

        final Context context = activity.getApplicationContext();

        EventBus.getDefault().post(new TTSEvent(TTSEvent.TTSEvents.TTS_CALIBRATION_IMU, instructions));

        // Toast.makeText(context, instructions, Toast.LENGTH_LONG).show();
    }

    protected void updateTimeOutProgress() {
        final long timeElapsed = System.currentTimeMillis() - updateTimestamp;
        long timeLeft = (int) (TIMEOUT_MAX - timeElapsed);

        if (timeLeft >= 0) {
            int secLeft = (int) (timeLeft / 1000) + 1;

            pbTimeOut.setIndeterminate(false);
            pbTimeOut.setMax((int) TIMEOUT_MAX);
            pbTimeOut.setProgress((int) timeLeft);

            textViewTimeOut.setText(timeLeftStr + String.valueOf(secLeft) + "s");
            if (secLeft > 15)
                pbTimeOut.setProgressDrawable(drawableGood);
            else if (secLeft <= 15 && secLeft > 5)
                pbTimeOut.setProgressDrawable(drawableWarning);
            else if (secLeft == 5)
                pbTimeOut.setProgressDrawable(drawablePoor);

        } else {
            textViewTimeOut.setText(timeLeftStr + "0s");
        }
    }
}
