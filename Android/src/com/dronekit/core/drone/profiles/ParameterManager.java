package com.dronekit.core.drone.profiles;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.SparseBooleanArray;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_param_value;
import com.dronekit.core.MAVLink.MavLinkParameters;
import com.dronekit.core.drone.DroneInterfaces.OnParameterManagerListener;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Parameter;
import com.dronekit.utils.file.IO.ParameterMetadataLoader;
import com.evenbus.AttributeEvent;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to manage the communication of parameters to the MAV.
 * <p/>
 * Should be initialized with a MAVLink Object, so the manager can send messages via the MAV link.
 * The function processMessage must be called with every new MAV Message.
 */
public class ParameterManager extends DroneVariable {

    private static final long TIMEOUT = 1000L; // milliseconds
    private final List<Parameter> parametersList = new ArrayList<Parameter>();
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private final SparseBooleanArray paramsRollCall = new SparseBooleanArray();
    private final ConcurrentHashMap<String, Parameter> parameters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ParameterMetadata> parametersMetadata = new ConcurrentHashMap<>();
    private final Handler watchdog;
    private final Context context;
    private int expectedParams;

    public final Runnable watchdogCallback = new Runnable() {
        @Override
        public void run() {
            onParameterStreamStopped();
        }
    };

    private OnParameterManagerListener parameterListener;
    private final Runnable parametersReceiptStartNotification = new Runnable() {
        @Override
        public void run() {
            if (parameterListener != null)
                parameterListener.onBeginReceivingParameters();
        }
    };

    private final Runnable parametersReceiptEndNotification = new Runnable() {
        @Override
        public void run() {
            if (parameterListener != null)
                parameterListener.onEndReceivingParameters();
            EventBus.getDefault().post(AttributeEvent.PARAMETERS_REFRESH_COMPLETED);
        }
    };

    public ParameterManager(Drone myDrone, Context context, Handler handler) {
        super(myDrone);
        this.context = context;
        this.watchdog = handler;
        EventBus.getDefault().register(this);
        refreshParametersMetadata();
    }

    public void refreshParameters() {
        if (isRefreshing.compareAndSet(false, true)) {
            expectedParams = 0;
            parameters.clear();
            paramsRollCall.clear();

            notifyParametersReceiptStart();

            MavLinkParameters.requestParametersList(myDrone);
            resetWatchdog();
        }
    }

    public List<Parameter> getParametersList() {
        if (parametersList.isEmpty()) {
            refreshParameters();
        }

        return parametersList;
    }

    public Map<String, Parameter> getParameters() {
        // Update the cache if it's stale. Parameters download is expensive, but we assume the caller knows what it's doing.
        if (parameters.isEmpty())
            refreshParameters();

        return parameters;
    }

    /**
     * Try to process a Mavlink message if it is a parameter related message
     *
     * @param msg Mavlink message to process
     * @return Returns true if the message has been processed
     */
    public boolean processMessage(MAVLinkMessage msg) {
        if (msg.msgid == msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE) {
            processReceivedParam((msg_param_value) msg);
            return true;
        }
        return false;
    }

    protected void processReceivedParam(msg_param_value m_value) {
        // collect params in parameter list
        Parameter param = new Parameter(m_value.getParam_Id(), m_value.param_value, m_value.param_type);
        loadParameterMetadata(param);

        parameters.put(param.getName().toLowerCase(Locale.US), param);
        int paramIndex = m_value.param_index;
        if (paramIndex == -1) {
            // update listener
            notifyParameterReceipt(param, 0, 1);

            notifyParametersReceiptEnd();
            return;
        }

        paramsRollCall.append(paramIndex, true);
        expectedParams = m_value.param_count;

        // update listener
        notifyParameterReceipt(param, paramIndex, m_value.param_count);

        // Are all parameters here? Notify the listener with the parameters
        if (parameters.size() >= m_value.param_count) {
            killWatchdog();
            isRefreshing.set(false);

            // 添加到集合中
            for (String key : parameters.keySet()) {
                parametersList.add(parameters.get(key));
            }

            // 接收完成！
            notifyParametersReceiptEnd();
        } else {
            resetWatchdog();
        }
    }

    private void reRequestMissingParams(int howManyParams) {
        for (int i = 0; i < howManyParams; i++) {
            if (!paramsRollCall.get(i)) {
                MavLinkParameters.readParameter(myDrone, i);
            }
        }
    }

    public void sendParameter(Parameter parameter) {
        MavLinkParameters.sendParameter(myDrone, parameter);
    }

    public void readParameter(String name) {
        MavLinkParameters.readParameter(myDrone, name);
    }

    public Parameter getParameter(String name) {
        if (TextUtils.isEmpty(name))
            return null;

        return parameters.get(name.toLowerCase(Locale.US));
    }

    private void onParameterStreamStopped() {
        if (expectedParams > 0) {
            reRequestMissingParams(expectedParams);
            resetWatchdog();
        } else {
            isRefreshing.set(false);
        }
    }

    private void resetWatchdog() {
        watchdog.removeCallbacks(watchdogCallback);
        watchdog.postDelayed(watchdogCallback, TIMEOUT);
    }

    private void killWatchdog() {
        watchdog.removeCallbacks(watchdogCallback);
        isRefreshing.set(false);
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case HEARTBEAT_FIRST:
                refreshParameters();
                break;

            case STATE_DISCONNECTED:
            case HEARTBEAT_TIMEOUT:
                killWatchdog();
                break;

            case TYPE_UPDATED:
                refreshParametersMetadata();
                break;
        }
    }

    private void refreshParametersMetadata() {
        // Reload the vehicle parameters metadata
        String metadataType = myDrone.getType().getFirmwareType().getParameterMetadataGroup();
        if (!TextUtils.isEmpty(metadataType)) {
            try {
                ParameterMetadataLoader.load(context, metadataType, this.parametersMetadata);
            } catch (Exception e) {
                Logger.e(e, e.getMessage());
            }
        }

        // parametersMetadata:装载参数对象的集合体
        if (parametersMetadata.isEmpty() || parameters.isEmpty())
            return;

        // 遍历集合
        for (Parameter parameter : parameters.values()) {
            loadParameterMetadata(parameter);
        }
    }

    private void loadParameterMetadata(Parameter parameter) {
        ParameterMetadata metadata = parametersMetadata.get(parameter.getName());
        if (metadata != null) {
            parameter.setDisplayName(metadata.getDisplayName());
            parameter.setDescription(metadata.getDescription());
            parameter.setUnits(metadata.getUnits());
            parameter.setRange(metadata.getRange());
            parameter.setValues(metadata.getValues());
        }
    }

    public void setParameterListener(OnParameterManagerListener parameterListener) {
        this.parameterListener = parameterListener;
    }

    private void notifyParametersReceiptStart() {
        if (parameterListener != null)
            watchdog.post(parametersReceiptStartNotification);
    }

    private void notifyParametersReceiptEnd() {
        if (parameterListener != null)
            watchdog.post(parametersReceiptEndNotification);
    }

    private void notifyParameterReceipt(final Parameter parameter, final int index, final int count) {
        if (parameterListener != null) {
            watchdog.post(new Runnable() {
                @Override
                public void run() {
                    if (parameterListener != null)
                        parameterListener.onParameterReceived(parameter, index, count);
                }
            });
        }
    }
}
