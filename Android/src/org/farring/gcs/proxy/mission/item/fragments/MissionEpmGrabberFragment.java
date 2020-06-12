package org.farring.gcs.proxy.mission.item.fragments;

import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemType;

import org.farring.gcs.R;

public class MissionEpmGrabberFragment extends MissionDetailFragment {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_epm_grabber;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.EPM_GRIPPER));
    }
}
