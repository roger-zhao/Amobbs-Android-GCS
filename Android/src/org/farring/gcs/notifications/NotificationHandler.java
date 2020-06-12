package org.farring.gcs.notifications;

import android.content.Context;

import com.dronekit.core.drone.autopilot.Drone;

/**
 * This class handles DroidPlanner's status bar, and audible notifications. It also provides support for the Android Wear functionality.
 */
public class NotificationHandler {

    /**
     * Handles Droidplanner's audible notifications.
     */
    private final TTSNotificationProvider mTtsNotification;
    /**
     * Handles Droidplanner's status bar notification.
     */
    private final StatusBarNotificationProvider mStatusBarNotification;
    /**
     * Handles emergency beep notification.
     */
    private final EmergencyBeepNotificationProvider mBeepNotification;

    public NotificationHandler(Context context, Drone drone) {
        mTtsNotification = new TTSNotificationProvider(context, drone);
        mStatusBarNotification = new StatusBarNotificationProvider(context, drone);
        mBeepNotification = new EmergencyBeepNotificationProvider(context);
    }

    public void init() {
        mTtsNotification.init();
        mStatusBarNotification.init();
        mBeepNotification.init();
    }

    public void terminate() {
        mTtsNotification.onTerminate();
        mStatusBarNotification.onTerminate();
        mBeepNotification.onTerminate();
    }

    /**
     * Defines the methods that need to be supported by Droidplanner's notification provider types (i.e: audible (text to speech), status bar).
     */
    public interface NotificationProvider {
        void init();

        /**
         * Release resources used by the provider.
         */
        void onTerminate();
    }
}
