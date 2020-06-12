package org.farring.gcs.fragments.helpers;

import android.app.Activity;
import android.support.v4.app.ListFragment;

import com.dronekit.core.drone.autopilot.Drone;

import org.greenrobot.eventbus.EventBus;
import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.proxy.mission.MissionProxy;

/**
 * Provides access to the DroidPlannerApi to its derived class.
 */
public abstract class BaseListFragment extends ListFragment {

    private FishDroneGCSApp dpApp;

    protected MissionProxy getMissionProxy() {
        return dpApp.getMissionProxy();
    }

    protected Drone getDrone() {
        return dpApp.getDrone();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dpApp = (FishDroneGCSApp) activity.getApplication();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
