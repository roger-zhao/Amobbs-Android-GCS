package com.dronekit.communication.service;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.dronekit.communication.connection.AndroidMavLinkConnection;
import com.dronekit.communication.connection.AndroidTcpConnection;
import com.dronekit.communication.connection.AndroidUdpConnection;
import com.dronekit.communication.connection.BluetoothConnection;
import com.dronekit.communication.connection.usb.UsbConnection;
import com.dronekit.communication.model.DataLink.DataLinkListener;
import com.dronekit.communication.model.DataLink.DataLinkProvider;
import com.dronekit.core.MAVLink.connection.MavLinkConnection;
import com.dronekit.core.MAVLink.connection.MavLinkConnectionListener;
import com.dronekit.core.MAVLink.connection.MavLinkConnectionTypes;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.drone.manager.DroneCommandTracker;
import com.orhanobut.logger.Logger;

import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Provide a common class for some ease of use functionality
 */
public class MAVLinkClient implements DataLinkProvider {
    public final static String TAG = MAVLinkClient.class.getSimpleName();
    private static final int DEFAULT_SYS_ID = 255;
    private static final int DEFAULT_COMP_ID = 190;
    /**
     * Maximum possible sequence number for a packet.
     */
    private static final int MAX_PACKET_SEQUENCE = 255;
    /**
     * Used to post updates to the main thread.
     */
    private final Handler mHandler = new Handler();
    private final DataLinkListener listener;
    private final Context context;
    private AndroidMavLinkConnection mavlinkConn;
    /**
     * Defines callbacks for service binding, passed to bindService()
     * 实例化Mavlink连接通用接口，并实现其中五个回调的方法
     */
    private final MavLinkConnectionListener mConnectionListener = new MavLinkConnectionListener() {
        @Override
        public void onStartingConnection() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.notifyStartingConnection();
                }
            });
        }

        @Override
        public void onConnect(final long connectionTime) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.notifyConnected();
                }
            });
        }

        @Override
        public void onReceivePacket(final MAVLinkPacket packet) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.notifyReceivedData(packet);
                }
            });
        }

        @Override
        public void onDisconnect(long disconnectTime) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.notifyDisconnected();
                    closeConnection();
                }
            });
        }

        @Override
        public void onComError(final String errMsg) {
            if (errMsg != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onStreamError(errMsg);
                    }
                });
            }
        }
    };

    private int packetSeqNumber = 0;

    private DroneCommandTracker commandTracker;
    private DroidPlannerPrefs mAppPrefs;

    /**
     * 构造方法
     *
     * @param context
     * @param listener
     * @param commandTracker
     */
    public MAVLinkClient(Context context, DataLinkListener listener, DroneCommandTracker commandTracker) {
        this.context = context;
        this.listener = listener;
        this.commandTracker = commandTracker;
        // Initialise the app preferences handle.
        mAppPrefs = DroidPlannerPrefs.getInstance(context);
    }

    private int getConnectionStatus() {
        return mavlinkConn == null
                ? MavLinkConnection.MAVLINK_DISCONNECTED
                : mavlinkConn.getConnectionStatus();
    }

    /**
     * Setup a MAVLink connection based on the connection parameters.
     */
    @Override
    public synchronized void openConnection() {
        if (isConnected() || isConnecting())
            return;

        // Create the mavlink connection
        final int connectionType = mAppPrefs.getConnectionParameterType();

        if (mavlinkConn == null || mavlinkConn.getConnectionType() != connectionType) {
            switch (connectionType) {
                case MavLinkConnectionTypes.MAVLINK_CONNECTION_USB:
                    final int baudRate = mAppPrefs.getUsbBaudRate();
                    mavlinkConn = new UsbConnection(context, baudRate);
                    Logger.i("Connecting over usb.");
                    break;

                case MavLinkConnectionTypes.MAVLINK_CONNECTION_BLUETOOTH:
                    //Retrieve the bluetooth address to connect to
                    final String bluetoothAddress = mAppPrefs.getBluetoothDeviceAddress();
                    mavlinkConn = new BluetoothConnection(context, bluetoothAddress);
                    Logger.i("Connecting over bluetooth.");
                    break;

                case MavLinkConnectionTypes.MAVLINK_CONNECTION_TCP:
                    // Retrieve the server ip and port
                    final String tcpServerIp = mAppPrefs.getTcpServerIp();
                    final int tcpServerPort = mAppPrefs.getTcpServerPort();
                    mavlinkConn = new AndroidTcpConnection(context, tcpServerIp, tcpServerPort);
                    Logger.i("Connecting over tcp.");
                    break;

                case MavLinkConnectionTypes.MAVLINK_CONNECTION_UDP:
                    final int udpServerPort = mAppPrefs.getUdpServerPort();
                    mavlinkConn = new AndroidUdpConnection(context, udpServerPort);
                    Logger.i("Connecting over udp.");
                    break;

                default:
                    Logger.e("Unrecognized connection type: %s", connectionType);
                    break;
            }
        }

        mavlinkConn.addMavLinkConnectionListener(TAG, mConnectionListener);

        //Check if we need to ping a server to receive UDP data stream.
        if (connectionType == MavLinkConnectionTypes.MAVLINK_CONNECTION_UDP && mAppPrefs.isUdpPingEnabled()) {
            final String pingIpAddress = mAppPrefs.getUdpPingReceiverIp();
            if (!TextUtils.isEmpty(pingIpAddress)) {
                try {
                    final InetAddress resolvedAddress = InetAddress.getByName(pingIpAddress);

                    final int pingPort = mAppPrefs.getUdpPingReceiverPort();
                    final long pingPeriod = 10000l;// 10 seconds
                    final byte[] pingPayload = "Hello".getBytes();

                    ((AndroidUdpConnection) mavlinkConn).addPingTarget(resolvedAddress, pingPort, pingPeriod, pingPayload);

                } catch (UnknownHostException e) {
                    Logger.e("Unable to resolve UDP ping server ip address." + e.toString());
                }
            }
        }

        if (mavlinkConn.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED) {
            mavlinkConn.connect();
        }
    }

    /**
     * Disconnect the MAVLink connection for the given listener.
     */
    @Override
    public synchronized void closeConnection() {
        if (isDisconnected())
            return;

        mavlinkConn.removeMavLinkConnectionListener(TAG);
        if (mavlinkConn.getMavLinkConnectionListenersCount() == 0) {
            mavlinkConn.disconnect();
        }

        listener.notifyDisconnected();
    }

    protected void sendMavMessage(MAVLinkMessage message, int sysId, int compId, ICommandListener listener) {
        if (isDisconnected() || message == null) {
            return;
        }

        final MAVLinkPacket packet = message.pack();
        packet.sysid = sysId;
        packet.compid = compId;
        packet.seq = packetSeqNumber;

        mavlinkConn.sendMavPacket(packet);

        packetSeqNumber = (packetSeqNumber + 1) % (MAX_PACKET_SEQUENCE + 1);

        if (commandTracker != null && listener != null) {
            commandTracker.onCommandSubmitted(message, listener);
        }
    }

    public synchronized boolean isDisconnected() {
        return getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
    }

    @Override
    public synchronized void sendMavMessage(MAVLinkMessage message, ICommandListener listener) {
        sendMavMessage(message, DEFAULT_SYS_ID, DEFAULT_COMP_ID, listener);
    }

    @Override
    public synchronized boolean isConnected() {
        return getConnectionStatus() == MavLinkConnection.MAVLINK_CONNECTED;
    }

    private boolean isConnecting() {
        return getConnectionStatus() == MavLinkConnection.MAVLINK_CONNECTING;
    }
}
