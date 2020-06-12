package org.farring.gcs.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Altitude;
import com.dronekit.core.drone.property.Battery;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.property.Home;
import com.dronekit.core.drone.property.Signal;
import com.dronekit.core.drone.property.Speed;
import com.dronekit.core.drone.property.Vibration;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.State;
import com.dronekit.utils.MathUtils;
import com.evenbus.AttributeEvent;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.EditorActivity;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.notifications.NotificationHandler.NotificationProvider;
import org.farring.gcs.utils.SpannableUtils;
import org.farring.gcs.utils.unit.UnitManager;

/**
 * Implements DroidPlanner's status bar notifications.
 */
public class StatusBarNotificationProvider implements NotificationProvider {


    StatusBarNotificationProvider(Context context, Drone api) {
        mContext = context;
        this.drone = api;

        mNotificationIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, EditorActivity.class), 0);
        mToggleConnectionIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, EditorActivity.class).setAction(SuperUI.ACTION_TOGGLE_DRONE_CONNECTION), 0);
    }
    /**
     * This is the period for the flight time update.
     */
    protected final static long FLIGHT_TIMER_PERIOD = 1000l; // 1 second
    private static final String TAG = StatusBarNotificationProvider.class.getSimpleName();
    /**
     * Android status bar's notification id.
     */
    private static final int NOTIFICATION_ID = 1;

    private final Handler mHandler = new Handler();
    /**
     * Application context.
     */
    private final Context mContext;
    private final Runnable removeNotification = new Runnable() {
        @Override
        public void run() {
            NotificationManagerCompat.from(mContext).cancelAll();
        }
    };
    /**
     * Pending intent for the notification on click behavior. Opens the
     * FlightActivity screen.
     */
    private final PendingIntent mNotificationIntent;
    /**
     * Pending intent for the notification connect/disconnect action.
     */
    private final PendingIntent mToggleConnectionIntent;

    private final Drone drone;
    /**
     * Builder for the app notification.
     */
    private NotificationCompat.Builder mNotificationBuilder;
    /**
     * Uses to generate the inbox style use to populate the notification.
     */
    private InboxStyleBuilder mInboxBuilder;
    /**
     * Runnable used to update the drone flight time.
     */
    protected final Runnable mFlightTimeUpdater = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(this);
            if (drone == null || !drone.isConnected())
                return;

            if (mInboxBuilder != null) {
                long timeInSeconds = drone.getState().getFlightTime();

                final Vibration userData = drone.getVibration();
                if (userData != null) {
                    timeInSeconds = (long) userData.getVibrationY();
                }
                long minutes = timeInSeconds / 60;
                long seconds = timeInSeconds % 60;

                mInboxBuilder.setLine(1, SpannableUtils.normal("飞行时间:   ", SpannableUtils.bold(String.format("%02d:%02d", minutes, seconds))));
            }

            mHandler.postDelayed(this, FLIGHT_TIMER_PERIOD);
        }
    };


    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        boolean showNotification = true;
        switch (attributeEvent) {
            case GPS_POSITION:
                updateHome(drone);
                break;
            case GPS_FIX:
            case GPS_COUNT:
                updateGps(drone);
                break;
            case BATTERY_UPDATED:
                updateBattery(drone);
                break;
            case HOME_UPDATED:
                updateHome(drone);
                break;
            case SIGNAL_UPDATED:
                updateRadio(drone);
                break;
            case STATE_UPDATED:
                updateDroneState(drone);
                break;
            case STATE_VEHICLE_MODE:
            case TYPE_UPDATED:
                updateFlightMode(drone);
                break;
            case SPEED_UPDATED:
                updateVel(drone);
                break;
            case ALTITUDE_UPDATED:
                updateAltitude(drone);
                break;
            default:
                showNotification = false;
                break;
        }

        if (showNotification) {
            showNotification();
        }
    }

    @Override
    public void init() {
        mHandler.removeCallbacks(removeNotification);

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        final String summaryText = mContext.getString(R.string.connected);

        mInboxBuilder = new InboxStyleBuilder().setSummary(summaryText);
        mNotificationBuilder = new NotificationCompat.Builder(mContext)
                .addAction(R.drawable.ic_action_io, mContext.getText(R.string.menu_disconnect),
                        mToggleConnectionIntent)
                .setContentIntent(mNotificationIntent)
                .setContentText(summaryText)
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(mContext.getResources().getColor(R.color.stat_notify_connected)
                );

        updateFlightMode(drone);
        updateDroneState(drone);
        updateBattery(drone);
        updateGps(drone);
        updateHome(drone);
        updateRadio(drone);
        updateAltitude(drone);
        updateVel(drone);
        showNotification();
    }

    /**
     * Dismiss the app status bar notification.
     */
    @Override
    public void onTerminate() {
        EventBus.getDefault().unregister(this);

        mInboxBuilder = null;

        if (mNotificationBuilder != null) {
            mNotificationBuilder = new NotificationCompat.Builder(mContext)
                    .addAction(R.drawable.ic_action_io,
                            mContext.getText(R.string.menu_connect), mToggleConnectionIntent)
                    .setContentIntent(mNotificationIntent)
                    .setContentTitle(mContext.getString(R.string.disconnected))
                    .setOngoing(false).setContentText("")
                    .setSmallIcon(R.drawable.ic_stat_notify);
        }

        showNotification();

        mHandler.postDelayed(removeNotification, 2000L);
    }


    private void updateBattery(Drone drone) {
        if (mInboxBuilder == null)
            return;

        Battery droneBattery = drone.getBattery();
        String update = droneBattery == null ? "--" : String.format("%.1fV", droneBattery.getBatteryVoltage());
        // String update = droneBattery == null ? "--" : String.format("%2.1fV (%2.0f%%)", droneBattery.getBatteryVoltage(), droneBattery.getBatteryRemain());

        mInboxBuilder.setLine(0, SpannableUtils.normal("电池电压:   ", SpannableUtils.bold(update)));
    }


    private void updateAltitude(Drone drone) {
        final Altitude altitude = drone.getAltitude();
        if (altitude != null) {
            double alt = altitude.getAltitude();
            mInboxBuilder.setLine(2, SpannableUtils.normal("高度:   ", SpannableUtils.bold(String.format("%.1f 米", alt))));

        }
    }

    private void updateGps(Drone drone) {
        if (mInboxBuilder == null)
            return;

        Gps droneGps = drone.getVehicleGps();
        String update = droneGps == null ? "--" : String.format("%d/%.1f", droneGps.getSatellitesCount(), droneGps.getGpsEph());
        mInboxBuilder.setLine(3, SpannableUtils.normal("卫星:   ", SpannableUtils.bold(update)));
    }

    private void updateVel(Drone drone) {
        final Speed velocity = drone.getSpeed();
        if (velocity != null) {
            double velVal = velocity.getGroundSpeed();
            mInboxBuilder.setLine(4, SpannableUtils.normal("速度:   ", SpannableUtils.bold(String.format("%.1f 米/秒", velVal))));
        }
    }


    private void updateHome(Drone drone) {
        if (mInboxBuilder == null)
            return;

        String update = "--";
        final Gps droneGps = this.drone.getVehicleGps();
        final Home droneHome = this.drone.getVehicleHome();
        if (droneGps != null && droneGps.isValid() && droneHome != null && droneHome.isValid()) {
            LengthUnit distanceToHome = UnitManager.getUnitSystem(mContext).getLengthUnitProvider()
                    .boxBaseValueToTarget(MathUtils.getDistance2D(droneHome.getCoordinate(), droneGps.getPosition()));
            update = String.format("%s", distanceToHome);
        }
        mInboxBuilder.setLine(5, SpannableUtils.normal("距家:   ", update.replace("m","米")));
    }

    private void updateRadio(Drone drone) {
        if (mInboxBuilder == null)
            return;

        Signal droneSignal = drone.getSignal();
        String update = droneSignal == null ? "--" : String.format("%d%%", MathUtils.getSignalStrength(droneSignal.getFadeMargin(), droneSignal.getRemFadeMargin()));
        mInboxBuilder.setLine(6, SpannableUtils.normal("信号:   ", SpannableUtils.bold(update)));
    }


    private void updateDroneState(Drone drone) {
        if (mInboxBuilder == null)
            return;

        mHandler.removeCallbacks(mFlightTimeUpdater);
        if (drone != null && drone.isConnected()) {
            mFlightTimeUpdater.run();
        }
    }

    private void updateFlightMode(Drone drone) {
        if (mNotificationBuilder == null)
            return;

        State droneState = drone.getState();
        ApmModes mode = droneState == null ? null : droneState.getMode();
        String update = mode == null ? "--" : mode.getLabel();

        final CharSequence modeSummary = SpannableUtils.normal("飞行模式:  ", SpannableUtils.bold(update));
        mNotificationBuilder.setContentTitle(modeSummary);
    }

    /**
     * Build a notification from the notification builder, and display it.
     */
    private void showNotification() {
        if (mNotificationBuilder == null) {
            return;
        }

        if (mInboxBuilder != null) {
            mNotificationBuilder.setStyle(mInboxBuilder.generateInboxStyle());
        }

        NotificationManagerCompat.from(mContext).notify(NOTIFICATION_ID,
                mNotificationBuilder.build());
    }

    private static class InboxStyleBuilder {
        private static final int MAX_LINES_COUNT = 7;

        private final CharSequence[] mLines = new CharSequence[MAX_LINES_COUNT];

        private CharSequence mSummary;

        private boolean mHasContent = false;

        public void setLine(int index, CharSequence content) {
            if (index >= mLines.length || index < 0) {
                Log.w(TAG, "Invalid index (" + index + ") for inbox content.");
                return;
            }

            mLines[index] = content;
            mHasContent = true;
        }

        public InboxStyleBuilder setSummary(CharSequence summary) {
            mSummary = summary;
            mHasContent = true;
            return this;
        }

        public NotificationCompat.InboxStyle generateInboxStyle() {
            if (!mHasContent) {
                return null;
            }

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            if (mSummary != null) {
                inboxStyle.setSummaryText(mSummary);
            }

            for (CharSequence line : mLines) {
                if (line != null) {
                    inboxStyle.addLine(line);
                }
            }
            return inboxStyle;
        }
    }
}
