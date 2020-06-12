package com.dronekit.core.drone.variables.calibration;

import android.os.Handler;
import android.widget.Toast;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_statustext;
import com.MAVLink.enums.MAV_CMD;
import com.dronekit.core.MAVLink.MavLinkCalibration;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.drone.commandListener.SimpleCommandListener;
import com.evenbus.AttributeEvent;

import org.farring.gcs.R;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.atomic.AtomicReference;

public class AccelCalibration extends DroneVariable {

    private final Handler handler;
    private final AtomicReference<ICommandListener> listenerRef = new AtomicReference<>(null);
    private final Runnable onCalibrationStart = new Runnable() {
        @Override
        public void run() {
            final ICommandListener listener = listenerRef.getAndSet(null);
            if (listener != null) {
                listener.onSuccess();
            }
        }
    };
    private String mavMsg;
    private boolean calibrating;
    private short fcStep;
    public AccelCalibration(Drone drone, Handler handler) {
        super(drone);
        this.handler = handler;
        EventBus.getDefault().register(this);
    }

    public void startCalibration(ICommandListener listener) {
        if (calibrating) {
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }

        if (myDrone.getState().isFlying()) {
            calibrating = false;
        } else {
            calibrating = true;
            mavMsg = "";

            listenerRef.set(listener);
            MavLinkCalibration.startAccelerometerCalibration(myDrone, new SimpleCommandListener() {
                @Override
                public void onSuccess() {
                    final ICommandListener listener = listenerRef.getAndSet(null);
                    if (listener != null) {
                        listener.onSuccess();
                    }
                }

                @Override
                public void onError(int executionError) {
                    final ICommandListener listener = listenerRef.getAndSet(null);
                    if (listener != null) {
                        listener.onError(executionError);
                    }
                }

                @Override
                public void onTimeout() {
                    final ICommandListener listener = listenerRef.getAndSet(null);
                    if (listener != null) {
                        listener.onTimeout();
                    }
                }
            });
        }
    }

    public void sendAck(int step) {
        if (calibrating)
            MavLinkCalibration.sendCalibrationAckMessage(myDrone, step);
    }

    public void processMessage(MAVLinkMessage msg) {

        if (calibrating && msg.msgid == msg_statustext.MAVLINK_MSG_ID_STATUSTEXT) {
            msg_statustext statusMsg = (msg_statustext) msg;
            final String message = statusMsg.getText();

            if (message != null && (message.startsWith("Place vehicle") || message.startsWith("Calibration"))) {
                handler.post(onCalibrationStart);

                mavMsg = message;
                if (message.startsWith("Calibration"))
                    calibrating = false;
                EventBus.getDefault().post(AttributeEvent.CALIBRATION_IMU);
            }
        }
        else if(msg.msgid == msg_command_long.MAVLINK_MSG_ID_COMMAND_LONG)
        {
            msg_command_long cmdMsg = (msg_command_long)msg;
            if(cmdMsg.command == MAV_CMD.MAV_CMD_ACCELCAL_VEHICLE_POS)
            {
                fcStep = (short)cmdMsg.param1;
                EventBus.getDefault().post(AttributeEvent.CALIBRATION_IMU_POS);
            }
        }

    }

    public String getMessage() {
        return mavMsg;
    }
    public short getFCStep() {
        return fcStep;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case HEARTBEAT_TIMEOUT:
            case STATE_DISCONNECTED:
                if (calibrating)
                    cancelCalibration();
                break;
        }
    }


    public void cancelCalibration() {
        mavMsg = "";
        calibrating = false;
        MavLinkCalibration.cancelAccelerometerCalibration(myDrone, new SimpleCommandListener());
    }
}
