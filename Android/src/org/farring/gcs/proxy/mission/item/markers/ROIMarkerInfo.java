package org.farring.gcs.proxy.mission.item.markers;

import org.farring.gcs.R;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

/**
 * This implements the marker source for the ROI mission item.
 */
class ROIMarkerInfo extends MissionItemMarkerInfo {
    protected ROIMarkerInfo(MissionItemProxy origin) {
        super(origin);
    }

    @Override
    protected int getSelectedIconResource() {
        return R.drawable.ic_wp_map_selected;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_roi;
    }
}
