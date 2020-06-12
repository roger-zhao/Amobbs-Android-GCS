package org.farring.gcs.proxy.mission.item.markers;

import org.farring.gcs.R;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

/**
 * This implements the marker source for the land mission item.
 */
class LandMarkerInfo extends MissionItemMarkerInfo {
    protected LandMarkerInfo(MissionItemProxy origin) {
        super(origin);
    }

    @Override
    protected int getSelectedIconResource() {
        return R.drawable.ic_wp_land_selected;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_wp_land;
    }
}
