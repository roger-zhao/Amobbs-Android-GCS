package org.farring.gcs.fragments.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dronekit.core.drone.autopilot.Drone;

import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;

/**
 * Provides action buttons functionality for generic drone type.
 */
public class GenericActionsFragment extends BaseFlightControlFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_generic_mission_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View connectBtn = view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mc_connectBtn:
                ((SuperUI) getActivity()).toggleDroneConnection();
                break;
        }
    }

    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        return false;
    }
}
