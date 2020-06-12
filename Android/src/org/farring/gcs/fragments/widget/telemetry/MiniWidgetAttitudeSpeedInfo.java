package org.farring.gcs.fragments.widget.telemetry;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.widget.TowerWidget;
import org.farring.gcs.fragments.widget.TowerWidgets;
import org.farring.gcs.utils.unit.providers.speed.SpeedUnitProvider;
import org.farring.gcs.view.AttitudeIndicator;

public class MiniWidgetAttitudeSpeedInfo extends TowerWidget {

    private AttitudeIndicator attitudeIndicator;
    private TextView roll, yaw, pitch, horizontalSpeed, verticalSpeed;
    private Boolean headingModeFPV = false;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case ATTITUDE_UPDATED:
                onOrientationUpdate();
                break;

            case SPEED_UPDATED:
                onSpeedUpdate();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mini_widget_attitude_speed_info, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        attitudeIndicator = (AttitudeIndicator) view.findViewById(R.id.aiView);

        roll = (TextView) view.findViewById(R.id.rollValueText);
        yaw = (TextView) view.findViewById(R.id.yawValueText);
        pitch = (TextView) view.findViewById(R.id.pitchValueText);

        horizontalSpeed = (TextView) view.findViewById(R.id.horizontal_speed_telem);
        verticalSpeed = (TextView) view.findViewById(R.id.vertical_speed_telem);
    }

    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        headingModeFPV = prefs.getBoolean("pref_heading_mode", false);
        updateAllTelem();
    }

    private void updateAllTelem() {
        onOrientationUpdate();
        onSpeedUpdate();
    }

    private void onOrientationUpdate() {
        if (!isAdded())
            return;

        float r = (float) getDrone().getAttitude().getRoll();
        float p = (float) getDrone().getAttitude().getPitch();
        float y = (float) getDrone().getAttitude().getYaw();

        if (!headingModeFPV & y < 0) {
            y = 360 + y;
        }

        attitudeIndicator.setAttitude(r, p, y);

        roll.setText(String.format("%3.0f\u00B0", r));
        pitch.setText(String.format("%3.0f\u00B0", p));
        yaw.setText(String.format("%3.0f\u00B0", y));
    }

    private void onSpeedUpdate() {
        if (!isAdded())
            return;

        final SpeedUnitProvider speedUnitProvider = getSpeedUnitProvider();
        horizontalSpeed.setText(getString(R.string.horizontal_speed_telem, speedUnitProvider.boxBaseValueToTarget(getDrone().getSpeed().getGroundSpeed()).toString()));
        verticalSpeed.setText(getString(R.string.vertical_speed_telem, speedUnitProvider.boxBaseValueToTarget(getDrone().getSpeed().getVerticalSpeed()).toString()));
    }

    @Override
    public TowerWidgets getWidgetType() {
        return TowerWidgets.ATTITUDE_SPEED_INFO;
    }
}