package com.dronekit.core.MAVLink.connection;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.dronekit.core.model.Logger;
import com.tlog.database.LogsRecordDatabase;

import org.farring.gcs.utils.file.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base for mavlink connection implementations.
 */
public abstract class MavLinkConnection {

    /*
     * MavLink connection states
     * Mavlink连接状态定义（未连接，连接中，已连接）
     */
    public static final int MAVLINK_DISCONNECTED = 0;
    public static final int MAVLINK_CONNECTING = 1;
    public static final int MAVLINK_CONNECTED = 2;
    private static final String TAG = MavLinkConnection.class.getSimpleName();

    /**
     * Size of the buffer used to read messages from the mavlink connection.
     * 读取缓冲区：4096字节
     */
    private static final int READ_BUFFER_SIZE = 4096;

    protected final Logger mLogger = initLogger();

    /**
     * Set of listeners subscribed to this mavlink connection.
     * We're using a ConcurrentSkipListSet because the object will be accessed from multiple threads concurrently.
     */
    private final ConcurrentHashMap<String, MavLinkConnectionListener> mListeners = new ConcurrentHashMap<>();

    /**
     * Queue the set of packets to send via the mavlink connection. A thread will be blocking on it until there's element(s) available to send.
     */
    private final LinkedBlockingQueue<byte[]> mPacketsToSend = new LinkedBlockingQueue<>();

    /**
     * Queue the set of packets to log. A thread will be blocking on it until there's element(s) available for logging.
     * 【日志队列】
     */
    private final LinkedBlockingQueue<byte[]> mPacketsToLog = new LinkedBlockingQueue<>();
    // 原子级别标志位，用于记录Mavlink连接状态，当前为“未连接”
    private final AtomicInteger mConnectionStatus = new AtomicInteger(MAVLINK_DISCONNECTED);
    // 原子级别标志位，用于记录Mavlink连接时间
    private final AtomicLong mConnectionTime = new AtomicLong(-1);

    /**
     * Blocks until there's packets to log, then dispatch them.
     * 【日志线程，用于记录Mavlink通信日志】直到有log日志产生，否则该线程堵塞
     */
    private final Runnable mLoggingTask = new Runnable() {

        @Override
        public void run() {
            // 获取文件对象
            final File tempLogFile = FileUtils.getTLogFile();
            final ByteBuffer logBuffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
            logBuffer.order(ByteOrder.BIG_ENDIAN);

            try {
                final BufferedOutputStream logWriter = new BufferedOutputStream(new FileOutputStream(tempLogFile));
                try {
                    while (mConnectionStatus.get() == MAVLINK_CONNECTED) {
                        // 获取队列中的日志数据
                        final byte[] packetData = mPacketsToLog.take();
                        // 清空缓冲区
                        logBuffer.clear();
                        // 记录时间(时间戳)
                        logBuffer.putLong(System.currentTimeMillis() * 1000);

                        // 记录Mavlink数据包
                        logWriter.write(logBuffer.array());
                        logWriter.write(packetData);
                    }
                } catch (InterruptedException e) {
                    mLogger.logVerbose(TAG, e.getMessage());

                } catch (IOException e) {
                    e.printStackTrace();
                    mLogger.logErr(TAG, "IO Exception while writing to " + tempLogFile.getAbsolutePath(), e);
                } finally {
                    try {
                        // 关流
                        logWriter.close();
                    } catch (IOException e) {
                        mLogger.logErr(TAG, "IO Exception while closing " + tempLogFile.getAbsolutePath(), e);
                    }
                    // 异步并发保存到数据库
                    LogsRecordDatabase.saveTlogToDBAsync(tempLogFile);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                mLogger.logErr(TAG, "IO Exception while writing to " + tempLogFile.getAbsolutePath(), e);
            }
        }
    };

    private Thread mConnectThread;
    private Thread mTaskThread;
    /**
     * Start the connection process.
     */
    private final Runnable mConnectingTask = new Runnable() {
        @Override
        public void run() {
            // Load the connection specific preferences
            loadPreferences();
            // Open the connection
            try {
                openConnection();
            } catch (IOException e) {
                // Ignore errors while shutting down
                if (mConnectionStatus.get() != MAVLINK_DISCONNECTED) {
                    reportComError(e.getMessage());
                    mLogger.logErr(TAG, e);
                }

                disconnect();
            }
            mLogger.logInfo(TAG, "Exiting connecting thread.");
        }
    };
    /**
     * Blocks until there's packet(s) to send, then dispatch them.
     */
    private final Runnable mSendingTask = new Runnable() {
        @Override
        public void run() {
            try {
                while (mConnectionStatus.get() == MAVLINK_CONNECTED) {
                    byte[] buffer = mPacketsToSend.take();

                    try {
                        sendBuffer(buffer);
                        queueToLog(buffer);
                    } catch (IOException e) {
                        reportComError(e.getMessage());
                        mLogger.logErr(TAG, e);
                    }
                }
            } catch (InterruptedException e) {
                mLogger.logVerbose(TAG, e.getMessage());
            } finally {
                disconnect();
            }
        }
    };

    /**
     * Manages the receiving and sending of messages.
     * 线程管理类（接收和发送消息）
     */
    private final Runnable mManagerTask = new Runnable() {
        @Override
        public void run() {
            Thread sendingThread = null;
            Thread loggingThread = null;

            try {
                // 获取当前时间作为连接时间
                final long connectionTime = System.currentTimeMillis();
                // 设置连接事件
                mConnectionTime.set(connectionTime);
                // 报告连接时间
                reportConnect(connectionTime);

                // Launch the 'Sending' thread
                mLogger.logInfo(TAG, "Starting sender thread.");
                sendingThread = new Thread(mSendingTask, "MavLinkConnection-Sending Thread");
                sendingThread.start();

                //Launch the 'Logging' thread
                mLogger.logInfo(TAG, "Starting logging thread.");
                loggingThread = new Thread(mLoggingTask, "MavLinkConnection-Logging Thread");
                loggingThread.start();

                final Parser parser = new Parser();
                parser.stats.mavlinkResetStats();

                final byte[] readBuffer = new byte[READ_BUFFER_SIZE];

                while (mConnectionStatus.get() == MAVLINK_CONNECTED) {
                    int bufferSize = readDataBlock(readBuffer);
                    handleData(parser, bufferSize, readBuffer);
                }
            } catch (IOException e) {
                // Ignore errors while shutting down
                if (mConnectionStatus.get() != MAVLINK_DISCONNECTED) {
                    reportComError(e.getMessage());
                    mLogger.logErr(TAG, e);
                }
            } finally {
                if (sendingThread != null && sendingThread.isAlive()) {
                    sendingThread.interrupt();
                }

                if (loggingThread != null && loggingThread.isAlive()) {
                    loggingThread.interrupt();
                }

                disconnect();
                mLogger.logInfo(TAG, "Exiting manager thread.");
            }
        }

        // 解析接收到的二进制数据，使其变为可读可用的Mavlink消息对象
        private void handleData(Parser parser, int bufferSize, byte[] buffer) {
            if (bufferSize < 1) {
                return;
            }

            for (int i = 0; i < bufferSize; i++) {
                MAVLinkPacket receivedPacket = parser.mavlink_parse_char(buffer[i] & 0x00ff);
                if (receivedPacket != null) {
                    queueToLog(receivedPacket);
                    reportReceivedPacket(receivedPacket);
                }
            }
        }
    };

    /**
     * Establish a mavlink connection.
     * If the connection is successful, it will be reported through the MavLinkConnectionListener interface.
     * 【建立Mavlink连接】如果连接成功，将会通过MavLinkConnectionListener接口方法进行回调操作
     */
    public void connect() {
        // 判断状态
        if (mConnectionStatus.compareAndSet(MAVLINK_DISCONNECTED, MAVLINK_CONNECTING)) {
            // 日志记录
            mLogger.logInfo(TAG, "Starting connection thread.");
            // 新建连接线程
            mConnectThread = new Thread(mConnectingTask, "MavLinkConnection-Connecting Thread");
            // 连接线程启动
            mConnectThread.start();
            // 报告连接中
            reportConnecting();
        }
    }

    // 【连接打开】
    protected void onConnectionOpened() {
        if (mConnectionStatus.compareAndSet(MAVLINK_CONNECTING, MAVLINK_CONNECTED)) {
            // 打开连接
            mLogger.logInfo(TAG, "Starting manager thread.");
            // 新建任务线程
            mTaskThread = new Thread(mManagerTask, "MavLinkConnection-Manager Thread");
            // 任务线程启动
            mTaskThread.start();
        }
    }

    // 【连接失败】
    protected void onConnectionFailed(String errMsg) {
        // 日志记录
        mLogger.logInfo(TAG, "Unable to establish connection: " + errMsg);
        // 报告错误
        reportComError(errMsg);
        // 断开连接
        disconnect();
    }

    /**
     * Disconnect a mavlink connection.
     * If the operation is successful, it will be reported through the MavLinkConnectionListener interface.
     * 【断开与Mavlink链路】如果操作成功，将会通过MavLinkConnectionListener接口方法进行回调，回调相关方法
     */
    public void disconnect() {
        if (mConnectionStatus.get() == MAVLINK_DISCONNECTED || (mConnectThread == null && mTaskThread == null)) {
            return;
        }

        try {
            final long disconnectTime = System.currentTimeMillis();

            mConnectionStatus.set(MAVLINK_DISCONNECTED);
            mConnectionTime.set(-1);

            if (mConnectThread != null && mConnectThread.isAlive() && !mConnectThread.isInterrupted()) {
                mConnectThread.interrupt();
            }

            if (mTaskThread != null && mTaskThread.isAlive() && !mTaskThread.isInterrupted()) {
                mTaskThread.interrupt();
            }

            closeConnection();
            reportDisconnect(disconnectTime);
        } catch (IOException e) {
            mLogger.logErr(TAG, e);
            reportComError(e.getMessage());
        }
    }

    // 获取连接状态【未连接 or 已连接】
    public int getConnectionStatus() {
        return mConnectionStatus.get();
    }

    // 将MavlinkPacket添加到日志文件（排队形式）
    public void sendMavPacket(MAVLinkPacket packet) {
        final byte[] packetData = packet.encodePacket();
        if (!mPacketsToSend.offer(packetData)) {
            mLogger.logErr(TAG, "Unable to send mavlink packet. Packet queue is full!");
        }
    }

    // 将MavlinkPacket添加到日志文件（排队形式）
    private void queueToLog(MAVLinkPacket packet) {
        if (packet != null)
            queueToLog(packet.encodePacket());
    }

    /**
     * Queue a mavlink packet for logging.
     * 将二进制数据写入到log日志中
     *
     * @param packetData MAVLinkPacket packet
     * @return true if the packet was queued successfully.
     */
    private void queueToLog(byte[] packetData) {
        if (packetData != null) {
            if (!mPacketsToLog.offer(packetData)) {
                mLogger.logErr(TAG, "Unable to log mavlink packet. Queue is full!");
            }
        }
    }

    /**
     * Adds a listener to the mavlink connection.
     *
     * @param listener
     * @param tag      Listener tag
     */
    public void addMavLinkConnectionListener(String tag, MavLinkConnectionListener listener) {
        mListeners.put(tag, listener);

        if (getConnectionStatus() == MAVLINK_CONNECTED) {
            listener.onConnect(mConnectionTime.get());
        }
    }

    /**
     * @return the count of connection listeners.
     * 返回监听器集合中的监听器个数
     */
    public int getMavLinkConnectionListenersCount() {
        return mListeners.size();
    }

    /**
     * Used to query the presence of a connection listener.
     * 查询带有tag标志的监听器是否在监听器集合MavLinkConnectionListener中
     *
     * @param tag connection listener tag
     * @return true if the tag is present in the listeners list.
     */
    public boolean hasMavLinkConnectionListener(String tag) {
        return mListeners.containsKey(tag);
    }

    /**
     * Removes the specified listener.
     * 返回监听器集合中的监听器个数
     *
     * @param tag Listener tag
     */
    public void removeMavLinkConnectionListener(String tag) {
        mListeners.remove(tag);
    }

    /**
     * Removes all the connection listeners.
     * 从监听器集合中移除指定的监听器
     */
    public void removeAllMavLinkConnectionListeners() {
        mListeners.clear();
    }

    // **************************** 抽象方法，供子类实现 【打开 关闭 读取 写入】 **************************
    // 初始化日志记录仪
    protected abstract Logger initLogger();

    // 1.打开连接
    protected abstract void openConnection() throws IOException;

    // 2.读取数据
    protected abstract int readDataBlock(byte[] buffer) throws IOException;

    // 3.发送数据
    protected abstract void sendBuffer(byte[] buffer) throws IOException;

    // 4.关闭连接
    protected abstract void closeConnection() throws IOException;

    protected abstract void loadPreferences();

    /**
     * @return The type of this mavlink connection.
     */
    public abstract int getConnectionType();

    protected Logger getLogger() {
        return mLogger;
    }

    /**
     * Utility method to notify the mavlink listeners about communication errors.
     * 【5.报告错误】当Mavlink连接出现错误时，通知各监听器（回调对应事件）
     *
     * @param errMsg
     */
    protected void reportComError(String errMsg) {
        if (mListeners.isEmpty())
            return;

        for (MavLinkConnectionListener listener : mListeners.values()) {
            listener.onComError(errMsg);
        }
    }

    // 【1.报告正在连接中】当Mavlink连接中时，通知各监听器（回调对应事件）
    protected void reportConnecting() {
        for (MavLinkConnectionListener listener : mListeners.values()) {
            listener.onStartingConnection();
        }
    }

    /**
     * Utility method to notify the mavlink listeners about a successful connection.
     * 【2.报告已连接】当Mavlink连接已经建立时被回调
     */
    protected void reportConnect(long connectionTime) {
        for (MavLinkConnectionListener listener : mListeners.values()) {
            listener.onConnect(connectionTime);
        }
    }

    /**
     * Utility method to notify the mavlink listeners about a connection disconnect.
     * 【4.报告断开连接】当Mavlink断开连接时被回调
     */
    protected void reportDisconnect(long disconnectTime) {
        if (mListeners.isEmpty())
            return;

        for (MavLinkConnectionListener listener : mListeners.values()) {
            listener.onDisconnect(disconnectTime);
        }
    }

    /**
     * Utility method to notify the mavlink listeners about received messages.
     * 【3.报告收到数据】当Mavlink收到数据时被回调
     *
     * @param packet received mavlink packet
     */
    private void reportReceivedPacket(MAVLinkPacket packet) {
        if (mListeners.isEmpty())
            return;

        for (MavLinkConnectionListener listener : mListeners.values()) {
            listener.onReceivePacket(packet);
        }
    }
}
