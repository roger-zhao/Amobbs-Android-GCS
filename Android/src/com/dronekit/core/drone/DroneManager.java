package com.dronekit.core.drone;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_command_ack;
import com.dronekit.communication.model.DataLink.DataLinkListener;
import com.dronekit.communication.service.MAVLinkClient;
import com.dronekit.core.MAVLink.MavLinkMsgHandler;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.autopilot.DroneImplement;
import com.dronekit.core.drone.manager.DroneCommandTracker;
import com.dronekit.core.gcs.ReturnToMe;
import com.dronekit.core.gcs.follow.Follow;
import com.dronekit.core.gcs.location.FusedLocation;
import com.dronekit.utils.AndroidApWarningParser;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

/**
 * Bridge between the communication channel, the drone instance(s), and the connected client(s).
 */
public class DroneManager implements DataLinkListener {

    private final Handler handler;
    private final MavLinkMsgHandler mavLinkMsgHandler;
    private final DroneCommandTracker commandTracker;
    private final Context context;
    private Drone drone;
    private Follow followMe;
    private ReturnToMe returnToMe;

    // 构造方法
    public DroneManager(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        DroidPlannerPrefs dpPrefs = DroidPlannerPrefs.getInstance(context);
        commandTracker = new DroneCommandTracker(handler);

        this.drone = new DroneImplement(context, handler, new MAVLinkClient(context, this, commandTracker), new AndroidApWarningParser());
        drone.getStreamRates().setRates(dpPrefs.getRates());

        this.mavLinkMsgHandler = new MavLinkMsgHandler(drone);

        this.followMe = new Follow(this, new FusedLocation());
        this.returnToMe = new ReturnToMe(this, new FusedLocation());
        EventBus.getDefault().register(this);
    }

    public Handler getHandler() {
        return handler;
    }

    public ReturnToMe getReturnToMe() {
        return returnToMe;
    }

    public Follow getFollowMe() {
        return followMe;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTING:
                Toast.makeText(context, "连接中……", Toast.LENGTH_SHORT).show();
                break;

            case STATE_DISCONNECTED:
                Toast.makeText(context, "连接断开...", Toast.LENGTH_SHORT).show();
                break;

            case STATE_CONNECTION_FAILED:
//            case HEARTBEAT_TIMEOUT:
                Toast.makeText(context, "数据链路错误……连接失败...", Toast.LENGTH_LONG).show();
                disconnect();
                break;

            case HEARTBEAT_TIMEOUT:
                //Toast.makeText(context, "数据链路丢失……检查连接...", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 开始连接
     */
    @Override
    public void notifyStartingConnection() {
        EventBus.getDefault().post(AttributeEvent.STATE_CONNECTING);
    }

    @Override
    public void notifyConnected() {
        EventBus.getDefault().post(AttributeEvent.CHECKING_VEHICLE_LINK);
    }

    @Override
    public void notifyDisconnected() {
        EventBus.getDefault().post(AttributeEvent.STATE_DISCONNECTED);
    }

    @Override
    public void notifyReceivedData(MAVLinkPacket packet) {
        MAVLinkMessage receivedMsg = packet.unpack();
        if (receivedMsg == null)
            return;

        if (receivedMsg.msgid == msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK) {
            msg_command_ack commandAck = (msg_command_ack) receivedMsg;
            commandTracker.onCommandAck(msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK, commandAck);
        } else {
            this.mavLinkMsgHandler.receiveData(receivedMsg);
        }
    }

    @Override
    public void onStreamError(final String errorMsg) {
        Toast.makeText(context, "连接错误:" + errorMsg, Toast.LENGTH_SHORT).show();
    }

    public Drone getDrone() {
        return this.drone;
    }

    public boolean isConnected() {
        return drone != null && drone.isConnected();
    }

    public void connect() {
        MAVLinkClient mavClient = (MAVLinkClient) drone.getMavClient();

        if (mavClient.isDisconnected()) {
            mavClient.openConnection();
        } else {
            if (isConnected()) {
                EventBus.getDefault().post(AttributeEvent.STATE_CONNECTED);

                if (!drone.isConnectionAlive())
                    EventBus.getDefault().post(AttributeEvent.HEARTBEAT_TIMEOUT);
            }
        }
    }

    public void disconnect() {
        final MAVLinkClient mavClient = (MAVLinkClient) drone.getMavClient();
        if (mavClient.isConnected()) {
            mavClient.closeConnection();
        }
    }
}
