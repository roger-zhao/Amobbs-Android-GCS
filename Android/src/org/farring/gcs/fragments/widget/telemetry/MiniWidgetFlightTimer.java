package org.farring.gcs.fragments.widget.telemetry;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback;
import com.dronekit.core.drone.autopilot.Drone;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.widget.TowerWidget;
import org.farring.gcs.fragments.widget.TowerWidgets;

public class MiniWidgetFlightTimer extends TowerWidget {

    private final static long FLIGHT_TIMER_PERIOD = 1000L; // 1 second
    /**
     * This handler is used to update the flight time value.
     */
    protected final Handler mHandler = new Handler();
    private TextView flightTimer;
    /**
     * Runnable used to update the drone flight time.
     */
    protected Runnable mFlightTimeUpdater = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(this);
            final Drone drone = getDrone();
            if (drone == null || !drone.isConnected())
                return;

            if (flightTimer != null) {
                long timeInSeconds = drone.getState().getFlightTime();
                long minutes = timeInSeconds / 60;
                long seconds = timeInSeconds % 60;

                flightTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            mHandler.postDelayed(this, FLIGHT_TIMER_PERIOD);
        }
    };

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_UPDATED:
                updateFlightTimer();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mini_widget_flight_timer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Context context = getActivity().getApplicationContext();

        flightTimer = (TextView) view.findViewById(R.id.flight_timer);
        flightTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(getActivity())
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(context.getString(R.string.label_widget_flight_timer))
                        .content(context.getString(R.string.description_reset_flight_timer))
                        .positiveText(context.getString(android.R.string.yes))
                        .negativeText(context.getString(android.R.string.no))
                        .onPositive(new SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                getDrone().getState().resetFlightTimer();
                                updateFlightTimer();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        updateFlightTimer();
    }

    private void updateFlightTimer() {
        final Drone drone = getDrone();
        mHandler.removeCallbacks(mFlightTimeUpdater);
        if (drone != null && drone.isConnected()) {
            mFlightTimeUpdater.run();
        } else {
            flightTimer.setText("00:00");
        }
    }

    @Override
    public TowerWidgets getWidgetType() {
        return TowerWidgets.FLIGHT_TIMER;
    }
}