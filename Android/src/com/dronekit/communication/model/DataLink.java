package com.dronekit.communication.model;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.dronekit.core.drone.commandListener.ICommandListener;

public class DataLink {

    public interface DataLinkProvider {

        void sendMavMessage(MAVLinkMessage message, ICommandListener listener);

        boolean isConnected();

        void openConnection();

        void closeConnection();
    }

    public interface DataLinkListener {

        void notifyStartingConnection();

        void notifyConnected();

        void notifyDisconnected();

        void notifyReceivedData(MAVLinkPacket packet);

        void onStreamError(String errorMsg);
    }
}
