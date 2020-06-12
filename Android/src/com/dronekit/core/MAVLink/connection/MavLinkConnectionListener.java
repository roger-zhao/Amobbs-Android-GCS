package com.dronekit.core.MAVLink.connection;

import com.MAVLink.MAVLinkPacket;

/**
 * Provides updates about the mavlink connection.
 */
public interface MavLinkConnectionListener {

    /**
     * Called when a connection is being established.
     */
    void onStartingConnection();

    /**
     * Called when the mavlink connection is established.
     */
    void onConnect(long connectionTime);

    /**
     * Called when data is received via the mavlink connection.
     *
     * @param packet received data
     */
    void onReceivePacket(MAVLinkPacket packet);

    /**
     * Called when the mavlink connection is disconnected.
     */
    void onDisconnect(long disconnectionTime);

    /**
     * Provides information about communication error.
     *
     * @param errMsg error information
     */
    void onComError(String errMsg);

}
