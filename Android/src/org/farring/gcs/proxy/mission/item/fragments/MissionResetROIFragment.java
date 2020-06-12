package org.farring.gcs.proxy.mission.item.fragments;

import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemType;

import org.farring.gcs.R;

/**
 * Created by Administrator on 2016/1/25.
 */
public class MissionResetROIFragment extends MissionDetailFragment {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_reset_roi;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.RESET_ROI));
    }
}
