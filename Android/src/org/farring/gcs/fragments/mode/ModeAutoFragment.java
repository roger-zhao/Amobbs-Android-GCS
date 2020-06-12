package org.farring.gcs.fragments.mode;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.dronekit.api.CommonApiUtils;
import com.dronekit.core.MAVLink.command.doCmd.MavLinkDoCmds;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.SimpleCommandListener;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.dronekit.core.mission.Mission;
import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.waypoints.SpatialCoordItem;
import com.dronekit.utils.MathUtils;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.R;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;

import java.util.ArrayList;
import java.util.List;


public class ModeAutoFragment extends Fragment implements View.OnClickListener, CardWheelHorizontalView.OnCardWheelScrollListener<Integer> {
    private Drone drone;
    private Mission mission;
    private int nextWaypoint;
    private ProgressBar missionProgress;
    private double remainingMissionLength;
    private boolean missionFinished;
    private CardWheelHorizontalView<Integer> waypointSelector;
    private NumericWheelAdapter waypointSelectorAdapter;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case PARAMETERS_REFRESH_COMPLETED:
            case MISSION_RECEIVED:
            case MISSION_UPDATED:
                final MissionProxy missionProxy = getMissionProxy();
                if (missionProxy != null) {
                    mission = drone.getMission();
                    waypointSelectorAdapter = new NumericWheelAdapter(getActivity().getApplicationContext(), R.layout.wheel_text_centered,
                            missionProxy.getFirstWaypoint(), missionProxy.getLastWaypoint(), "%3d");
                    waypointSelector.setViewAdapter(waypointSelectorAdapter);
                }
                break;

            case MISSION_ITEM_UPDATED:
                nextWaypoint = drone.getMissionStats().getCurrentWP();
                waypointSelector.setCurrentValue(nextWaypoint);
                break;

            case GPS_POSITION:
                updateMission();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mode_auto, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.mc_pause).setOnClickListener(this);
        view.findViewById(R.id.mc_restart).setOnClickListener(this);
        missionProgress = (ProgressBar) view.findViewById(R.id.mission_progress);
        waypointSelector = (CardWheelHorizontalView<Integer>) view.findViewById(R.id.waypoint_selector);
        waypointSelector.addScrollListener(this);
        mission = drone.getMission();

        final FishDroneGCSApp dpApp = (FishDroneGCSApp) getActivity().getApplication();

        final MissionProxy missionProxy = getMissionProxy();
        waypointSelectorAdapter = new NumericWheelAdapter(getActivity().getApplicationContext(), R.layout.wheel_text_centered, missionProxy.getFirstWaypoint(), missionProxy.getLastWaypoint(), "%3d");
        waypointSelector.setViewAdapter(waypointSelectorAdapter);
    }

    private MissionProxy getMissionProxy() {
        final Activity activity = getActivity();
        if (activity == null)
            return null;

        return ((FishDroneGCSApp) activity.getApplication()).getMissionProxy();
    }

    @Override
    public void onClick(View v) {
        if (mission == null) {
            mission = drone.getMission();
        }
        switch (v.getId()) {
            case R.id.mc_pause: {
                drone.getGuidedPoint().pauseAtCurrentLocation(null);
                break;
            }
            case R.id.mc_restart: {
                gotoMissionItem(0);
                break;
            }
        }
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

    private void gotoMissionItem(final int waypoint) {
        if (missionFinished || waypoint == 0) {
            drone.getState().changeFlightMode(ApmModes.ROTOR_GUIDED, new SimpleCommandListener() {
                @Override
                public void onSuccess() {
                    CommonApiUtils.startMission(drone, true, true, new SimpleCommandListener() {
                        @Override
                        public void onSuccess() {
                            CommonApiUtils.gotoWaypoint(drone, waypoint, null);
                        }
                    });
                    missionFinished = false;
                }
            });
        } else {
            MavLinkDoCmds.gotoWaypoint(drone, waypoint, null);
        }
    }

    private double getRemainingMissionLength() {
        Gps gps = drone.getVehicleGps();
        if (mission == null || mission.getItems().size() == 0 || gps == null || !gps.isValid())
            return -1;
        LatLong dronePos = gps.getPosition();
        List<MissionItemImpl> missionItems = mission.getItems();
        List<LatLong> path = new ArrayList<LatLong>();
        path.add(dronePos);
        for (int i = Math.max(nextWaypoint - 1, 0); i < missionItems.size(); i++) {
            MissionItemImpl item = missionItems.get(i);
            if (item instanceof SpatialCoordItem) {
                SpatialCoordItem spatialItem = (SpatialCoordItem) item;
                LatLongAlt coordinate = spatialItem.getCoordinate();
                path.add(new LatLong(coordinate.getLatitude(), coordinate.getLongitude()));
            }

        }
        return MathUtils.getPolylineLength(path);
    }

    private double getTotalMissionLength() {
        List<MissionItemImpl> missionItems = mission.getItems();
        List<LatLong> path = new ArrayList<LatLong>();
        for (int i = 0; i < missionItems.size(); i++) {
            MissionItemImpl item = missionItems.get(i);
            if (item instanceof SpatialCoordItem) {
                SpatialCoordItem spatialItem = (SpatialCoordItem) item;
                LatLongAlt coordinate = spatialItem.getCoordinate();
                path.add(new LatLong(coordinate.getLatitude(), coordinate.getLongitude()));
            }

        }
        return MathUtils.getPolylineLength(path);
    }


    private void updateMission() {
        if (mission == null)
            return;
        double totalLength = getTotalMissionLength();
        missionProgress.setMax((int) totalLength);
        remainingMissionLength = getRemainingMissionLength();
        missionProgress.setProgress((int) ((totalLength - remainingMissionLength)));
        missionFinished = remainingMissionLength < 5;
    }

    @Override
    public void onAttach(Activity activity) {
        drone = ((FishDroneGCSApp) activity.getApplication()).getDrone();
        super.onAttach(activity);
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Integer startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Integer oldValue, Integer newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView cardWheel, Integer startValue, Integer endValue) {
        if (cardWheel.getId() == R.id.waypoint_selector) {
            gotoMissionItem(endValue);
        }
    }
}
