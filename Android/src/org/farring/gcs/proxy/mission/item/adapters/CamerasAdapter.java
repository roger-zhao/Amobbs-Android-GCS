package org.farring.gcs.proxy.mission.item.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dronekit.core.survey.CameraInfo;

import java.util.List;

public class CamerasAdapter extends ArrayAdapter<CameraInfo> {

    public CamerasAdapter(Context context, int resource, CameraInfo[] cameraDetails) {
        super(context, resource, cameraDetails);
    }

    public CamerasAdapter(Context context, int resource, List<CameraInfo> cameraDetails) {
        super(context, resource, cameraDetails);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        view.setText(getItem(position).getName());
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        view.setText(getItem(position).getName());
        return view;
    }
}

