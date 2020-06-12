package com.dronekit.communication.connection;

import android.content.Context;

import com.dronekit.core.MAVLink.connection.MavLinkConnection;
import com.dronekit.core.model.Logger;
import com.dronekit.utils.AndroidLogger;

public abstract class AndroidMavLinkConnection extends MavLinkConnection {

    private static final String TAG = AndroidMavLinkConnection.class.getSimpleName();

    protected final Context mContext;

    public AndroidMavLinkConnection(Context applicationContext) {
        this.mContext = applicationContext;
    }

    @Override
    protected final Logger initLogger() {
        return AndroidLogger.getLogger();
    }
}
