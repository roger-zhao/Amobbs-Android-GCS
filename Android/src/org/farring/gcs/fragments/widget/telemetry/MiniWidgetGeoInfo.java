package org.farring.gcs.fragments.widget.telemetry;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Gps;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.widget.TowerWidget;
import org.farring.gcs.fragments.widget.TowerWidgets;

public class MiniWidgetGeoInfo extends TowerWidget {

    private TextView latitude, longitude;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case GPS_POSITION:
            case HOME_UPDATED:
                onPositionUpdate();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mini_widget_geo_info, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getContext();
        final Drone drone = getDrone();
        latitude = (TextView) view.findViewById(R.id.latitude_telem);
        longitude = (TextView) view.findViewById(R.id.longitude_telem);

        final ClipboardManager clipboardMgr = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        View container = view.findViewById(R.id.mini_widget_geo_info_layout);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drone.isConnected()) {
                    Gps droneGps = drone.getVehicleGps();
                    if (droneGps.isValid()) {
                        //Copy the lat long to the clipboard.
                        String latLongText = droneGps.getPosition().getLongitude() + "/" + droneGps.getPosition().getLatitude();
                        clipboardMgr.setPrimaryClip(ClipData.newPlainText("飞行器经纬度 Longitude/latitude", latLongText));
                        Toast.makeText(context, "经纬度数据已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        onPositionUpdate();
    }

    private void onPositionUpdate() {
        if (!isAdded())
            return;

        Gps droneGps = getDrone().getVehicleGps();
        if (droneGps.isValid()) {
            Double latitudeValue = droneGps.getPosition().getLatitude();
            Double longitudeValue = droneGps.getPosition().getLongitude();
            latitude.setText(getString(R.string.latitude_telem, Location.convert(latitudeValue, Location.FORMAT_DEGREES).toString()));
            longitude.setText(getString(R.string.longitude_telem, Location.convert(longitudeValue, Location.FORMAT_DEGREES).toString()));
        }
    }

    @Override
    public TowerWidgets getWidgetType() {
        return TowerWidgets.GEO_INFO;
    }
}