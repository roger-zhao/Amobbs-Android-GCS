package org.farring.gcs.activities.interfaces;

import com.dronekit.core.helpers.coordinates.LatLong;

import org.farring.gcs.proxy.mission.item.MissionItemProxy;

// 接口定义
public interface OnEditorInteraction {
    void onItemClick(MissionItemProxy item, boolean zoomToFit);

    void onMapClick(LatLong coord);

    void onListVisibilityChanged();
}
