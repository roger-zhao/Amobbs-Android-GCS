package com.dronekit.core.drone;

import android.os.Handler;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.ICommandListener;

public class DroneVariable {
    public Drone myDrone;

    public DroneVariable(Drone myDrone) {
        this.myDrone = myDrone;
    }

    /**
     * Convenience method to post a success event to the listener.
     *
     * @param handler  Use to dispatch the event
     * @param listener To whom the event is dispatched.
     */
    protected void postSuccessEvent(Handler handler, final ICommandListener listener) {
        if (handler != null && listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSuccess();
                }
            });
        }
    }

    /**
     * Convenience method to post an error event to the listener.
     *
     * @param handler  Use to dispatch the event
     * @param listener To whom the event is dispatched.
     * @param error    Execution error.
     */
    protected void postErrorEvent(Handler handler, final ICommandListener listener, final int error) {
        if (handler != null && listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onError(error);
                }
            });
        }
    }

    /**
     * Convenience method to post a timeout event to the listener.
     *
     * @param handler  Use to dispatch the event
     * @param listener To whom the event is dispatched.
     */
    protected void postTimeoutEvent(Handler handler, final ICommandListener listener) {
        if (handler != null && listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onTimeout();
                }
            });
        }
    }
}