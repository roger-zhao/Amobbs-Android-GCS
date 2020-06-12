package org.farring.gcs.fragments.helpers;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.DialogFragment;

import com.dronekit.core.drone.autopilot.Drone;
import com.evenbus.ActionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.utils.unit.UnitManager;
import org.farring.gcs.utils.unit.providers.area.AreaUnitProvider;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.utils.unit.providers.speed.SpeedUnitProvider;
import org.farring.gcs.utils.unit.systems.UnitSystem;

/**
 * Provides access to the DroidPlannerApi to its derived class.
 */
public abstract class BaseDialogFragment extends DialogFragment {

    private FishDroneGCSApp dpApp;
    private LengthUnitProvider lengthUnitProvider;
    private AreaUnitProvider areaUnitProvider;
    private SpeedUnitProvider speedUnitProvider;

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_PREF_UNIT_SYSTEM_UPDATE:
                setupUnitProviders(dpApp.getApplicationContext());
                break;
        }
    }

    protected MissionProxy getMissionProxy() {
        return dpApp.getMissionProxy();
    }

    protected Drone getDrone() {
        return dpApp.getDrone();
    }

    protected LengthUnitProvider getLengthUnitProvider() {
        return lengthUnitProvider;
    }

    protected AreaUnitProvider getAreaUnitProvider() {
        return areaUnitProvider;
    }

    protected SpeedUnitProvider getSpeedUnitProvider() {
        return speedUnitProvider;
    }

    protected DroidPlannerPrefs getAppPrefs() {
        return DroidPlannerPrefs.getInstance(getContext());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dpApp = (FishDroneGCSApp) activity.getApplication();

        final Context context = activity.getApplicationContext();
        setupUnitProviders(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        setupUnitProviders(getContext());
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void setupUnitProviders(Context context) {
        if (context == null)
            return;

        final UnitSystem unitSystem = UnitManager.getUnitSystem(context);
        lengthUnitProvider = unitSystem.getLengthUnitProvider();
        areaUnitProvider = unitSystem.getAreaUnitProvider();
        speedUnitProvider = unitSystem.getSpeedUnitProvider();
    }
}
