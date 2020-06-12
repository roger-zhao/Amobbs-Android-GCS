package org.farring.gcs.fragments.actionbar;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dronekit.core.drone.autopilot.Drone;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;

public class VehicleStatusFragment extends BaseFragment {

    private CharSequence title = "";
    private TextView titleView;
    private ImageView connectedIcon, batteryIcon;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTED:
                updateAllStatus();
                break;

            case STATE_DISCONNECTED:
                updateAllStatus();
                break;

            case HEARTBEAT_RESTORED:
                updateConnectionStatus();
                break;

            case HEARTBEAT_TIMEOUT:
                updateConnectionStatus();
                break;

            case BATTERY_UPDATED:
                updateBatteryStatus();
                break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicle_status, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        connectedIcon = (ImageView) view.findViewById(R.id.status_vehicle_connection);
        batteryIcon = (ImageView) view.findViewById(R.id.status_vehicle_battery);

        titleView = (TextView) view.findViewById(R.id.status_actionbar_title);
        titleView.setText(title);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateAllStatus();
    }

    @Override
    public void onStop() {
        super.onStop();
        updateAllStatus();
    }

    private void updateAllStatus() {
        updateBatteryStatus();
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        final Drone drone = getDrone();
        if (drone == null || !drone.isConnected())
            connectedIcon.setImageLevel(0);
        else {
            if (drone.isConnectionAlive())
                connectedIcon.setImageLevel(2);
            else
                connectedIcon.setImageLevel(1);
        }
    }


    public void setTitle(CharSequence title) {
        this.title = title;
        titleView.setText(title);
    }

    private void updateBatteryStatus() {
        Drone drone = getDrone();
        int level;

        if (drone == null || !drone.isConnected()) {
            level = 0;
        } else {

            double battRemain = drone.getBattery().getBatteryRemain();
            if (battRemain >= 100) {
                level = 8;
            } else if (battRemain >= 87.5) {
                level = 7;
            } else if (battRemain >= 75) {
                level = 6;
            } else if (battRemain >= 62.5) {
                level = 5;
            } else if (battRemain >= 50) {
                level = 4;
            } else if (battRemain >= 37.5) {
                level = 3;
            } else if (battRemain >= 25) {
                level = 2;
            } else if (battRemain >= 12.5) {
                level = 1;
            } else {
                level = 0;
            }
        }
        batteryIcon.setImageLevel(level);
    }
}