package org.farring.gcs.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.error.ErrorType;
import com.evenbus.AttributeEvent;
import com.evenbus.LogMessageEvent;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.DrawerNavigationUI;
import org.farring.gcs.fragments.control.FlightControlManagerFragment;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.fragments.mode.FlightModePanel;
import org.farring.gcs.utils.prefs.AutoPanMode;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Fredia Huya-Kouadio on 8/27/15.
 */
public class FlightDataFragment extends BaseFragment {

    public static final String EXTRA_SHOW_ACTION_DRAWER_TOGGLE = "extra_show_action_drawer_toggle";
    private static final boolean DEFAULT_SHOW_ACTION_DRAWER_TOGGLE = false;

    /**
     * Determines how long the failsafe view is visible for.
     */
    private static final long WARNING_VIEW_DISPLAY_TIMEOUT = 10000l; //ms
    private final AtomicBoolean mSlidingPanelCollapsing = new AtomicBoolean(false);
    private final String disablePanelSlidingLabel = "disablingListener";
    private final Handler handler = new Handler();
    private final String parentActivityPanelListenerLabel = "parentListener";
    private final SlidingPanelListenerManager slidingPanelListenerMgr = new SlidingPanelListenerManager();
    private View actionbarShadow;

    private View warningContainer;
    private final Runnable hideWarningViewCb = new Runnable() {
        @Override
        public void run() {
            hideWarningView();
        }
    };
    private TextView warningText;
    private FlightMapFragment mapFragment;
    private FlightControlManagerFragment flightActions;
    private SlidingUpPanelLayout mSlidingPanel;
    private View mFlightActionsView;

    private final SlidingUpPanelLayout.PanelSlideListener mDisablePanelSliding = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View view, float v) {
        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            switch (newState) {
                case COLLAPSED:
                    mSlidingPanel.setEnabled(false);
                    mSlidingPanel.setPanelHeight(mFlightActionsView.getHeight());
                    mSlidingPanelCollapsing.set(false);

                    //Remove the panel slide listener
                    slidingPanelListenerMgr.removePanelSlideListener(disablePanelSlidingLabel);
                    break;
            }
        }
    };

    private FloatingActionButton mGoToMyLocation;
    private FloatingActionButton mGoToDroneLocation;
    private FloatingActionButton actionDrawerToggle;

    private DrawerNavigationUI navActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DrawerNavigationUI)
            navActivity = (DrawerNavigationUI) activity;

        if (activity instanceof SlidingUpPanelLayout.PanelSlideListener)
            slidingPanelListenerMgr.addPanelSlideListener(parentActivityPanelListenerLabel, (SlidingUpPanelLayout.PanelSlideListener) activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flight_data, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle arguments = getArguments();
        final boolean showActionDrawerToggle = arguments == null
                ? DEFAULT_SHOW_ACTION_DRAWER_TOGGLE
                : arguments.getBoolean(EXTRA_SHOW_ACTION_DRAWER_TOGGLE, DEFAULT_SHOW_ACTION_DRAWER_TOGGLE);

        actionbarShadow = view.findViewById(R.id.actionbar_shadow);

        final FragmentManager fm = getChildFragmentManager();

        mSlidingPanel = (SlidingUpPanelLayout) view.findViewById(R.id.slidingPanelContainer);
        mSlidingPanel.addPanelSlideListener(slidingPanelListenerMgr);

        warningText = (TextView) view.findViewById(R.id.failsafeTextView);
        warningContainer = view.findViewById(R.id.warningContainer);
        ImageView closeWarningView = (ImageView) view.findViewById(R.id.close_warning_view);
        closeWarningView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideWarningView();
            }
        });
        setupMapFragment();

        mGoToMyLocation = (FloatingActionButton) view.findViewById(R.id.my_location_button);
        mGoToDroneLocation = (FloatingActionButton) view.findViewById(R.id.drone_location_button);
        actionDrawerToggle = (FloatingActionButton) view.findViewById(R.id.toggle_action_drawer);

        if (showActionDrawerToggle) {
            actionDrawerToggle.setVisibility(View.VISIBLE);

            actionDrawerToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (navActivity == null)
                        return;

                    if (navActivity.isActionDrawerOpened())
                        navActivity.closeActionDrawer();
                    else
                        navActivity.openActionDrawer();
                }
            });
        }

        mGoToMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToMyLocation();
                    updateMapLocationButtons(AutoPanMode.DISABLED);
                }
            }
        });
        mGoToMyLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToMyLocation();
                    updateMapLocationButtons(AutoPanMode.USER);
                    return true;
                }
                return false;
            }
        });

        mGoToDroneLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToDroneLocation();
                    updateMapLocationButtons(AutoPanMode.DISABLED);
                }
            }
        });
        mGoToDroneLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToDroneLocation();
                    updateMapLocationButtons(AutoPanMode.DRONE);
                    return true;
                }
                return false;
            }
        });

        flightActions = (FlightControlManagerFragment) fm.findFragmentById(R.id.flightActionsFragment);
        if (flightActions == null) {
            flightActions = new FlightControlManagerFragment();
            fm.beginTransaction().add(R.id.flightActionsFragment, flightActions).commit();
        }

        mFlightActionsView = view.findViewById(R.id.flightActionsFragment);
        mFlightActionsView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver
                .OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!mSlidingPanelCollapsing.get()) {
                    mSlidingPanel.setPanelHeight(mFlightActionsView.getHeight());
                }
            }
        });

        // Add the mode info panel fragment
        FlightModePanel flightModePanel = (FlightModePanel) fm.findFragmentById(R.id.sliding_drawer_content);
        if (flightModePanel == null) {
            flightModePanel = new FlightModePanel();
            fm.beginTransaction()
                    .add(R.id.sliding_drawer_content, flightModePanel)
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setupMapFragment();
        updateMapLocationButtons(getAppPrefs().getAutoPanMode());
        enableSlidingUpPanel(getDrone());
    }

    @Override
    public void onStop() {
        super.onStop();
        enableSlidingUpPanel(getDrone());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navActivity = null;
        slidingPanelListenerMgr.removePanelSlideListener(parentActivityPanelListenerLabel);
    }

    @Subscribe
    public void onReceiveLogMessageEvent(LogMessageEvent logMessageEvent) {
        onAutopilotError(logMessageEvent.getLogLevel(), logMessageEvent.getMessage());
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case AUTOPILOT_ERROR:
                String errorName = getDrone().getState().getErrorId();
                final ErrorType errorType = ErrorType.getErrorById(errorName);
                onAutopilotError(errorType);
                break;

            case STATE_ARMING:
            case STATE_CONNECTED:
            case STATE_DISCONNECTED:
            case STATE_UPDATED:
            case TYPE_UPDATED:
                enableSlidingUpPanel(getDrone());
                break;

            case FOLLOW_START:
                //Extend the sliding drawer if collapsed.
                if (!mSlidingPanelCollapsing.get()
                        && mSlidingPanel.isEnabled()
                        && mSlidingPanel.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
                break;

            case MISSION_DRONIE_CREATED:
                float dronieBearing = getMissionProxy().makeAndUploadDronie();
                if (dronieBearing != -1)
                    updateMapBearing(dronieBearing);
                break;
        }
    }

    private void hideWarningView() {
        handler.removeCallbacks(hideWarningViewCb);
        if (warningContainer != null && warningContainer.getVisibility() != View.GONE)
            warningContainer.setVisibility(View.GONE);
    }

    public void updateActionbarShadow(int shadowHeight) {
        if (actionbarShadow == null || actionbarShadow.getLayoutParams().height == shadowHeight)
            return;

        actionbarShadow.getLayoutParams().height = shadowHeight;
        actionbarShadow.requestLayout();
    }


    /**
     * Used to setup the flight screen map fragment. Before attempting to initialize the map fragment, this checks if the Google Play Services binary is installed and up to date.
     */
    private void setupMapFragment() {
        final FragmentManager fm = getChildFragmentManager();
        if (mapFragment == null) {
            mapFragment = (FlightMapFragment) fm.findFragmentById(R.id.flight_map_fragment);
            if (mapFragment == null) {
                mapFragment = new FlightMapFragment();
                fm.beginTransaction().add(R.id.flight_map_fragment, mapFragment).commit();
            }
        }
    }

    private void updateMapLocationButtons(AutoPanMode mode) {
        mGoToMyLocation.setActivated(false);
        mGoToDroneLocation.setActivated(false);

        if (mapFragment != null) {
            mapFragment.setAutoPanMode(mode);
        }

        switch (mode) {
            case DRONE:
                mGoToDroneLocation.setActivated(true);
                break;

            case USER:
                mGoToMyLocation.setActivated(true);
                break;
            default:
                break;
        }
    }

    public void updateMapBearing(float bearing) {
        if (mapFragment != null)
            mapFragment.updateMapBearing(bearing);
    }

    private void enableSlidingUpPanel(Drone api) {
        if (mSlidingPanel == null || api == null) {
            return;
        }

        final boolean isEnabled = flightActions != null && flightActions.isSlidingUpPanelEnabled
                (api);

        if (isEnabled) {
            mSlidingPanel.setEnabled(true);
            mSlidingPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    SlidingUpPanelLayout.PanelState panelState = mSlidingPanel.getPanelState();
                    switch (panelState) {
                        case EXPANDED:
                        case ANCHORED:
                        case HIDDEN:
                            slidingPanelListenerMgr.onPanelStateChanged(mSlidingPanel.getChildAt(1), panelState, panelState);
                    }

                    mSlidingPanel.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
        } else {
            if (!mSlidingPanelCollapsing.get()) {
                SlidingUpPanelLayout.PanelState panelState = mSlidingPanel.getPanelState();
                if (panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    slidingPanelListenerMgr.addPanelSlideListener(disablePanelSlidingLabel, mDisablePanelSliding);
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    mSlidingPanelCollapsing.set(true);
                } else {
                    mSlidingPanel.setEnabled(false);
                    mSlidingPanelCollapsing.set(false);
                }
            }
        }
    }

    private void onAutopilotError(ErrorType errorType) {
        if (errorType == null)
            return;

        final CharSequence errorLabel;
        switch (errorType) {
            case NO_ERROR:
                errorLabel = null;
                break;

            default:
                errorLabel = errorType.getLabel(getContext());
                break;
        }

        onAutopilotError(Log.ERROR, errorLabel);
    }

    private void onAutopilotError(int logLevel, CharSequence errorMsg) {
        if (TextUtils.isEmpty(errorMsg))
            return;

        switch (logLevel) {
            case Log.ERROR:
            case Log.WARN:
                handler.removeCallbacks(hideWarningViewCb);

                warningText.setText(errorMsg);
                warningContainer.setVisibility(View.VISIBLE);
                handler.postDelayed(hideWarningViewCb, WARNING_VIEW_DISPLAY_TIMEOUT);
                break;
        }
    }

    public void setGuidedClickListener(FlightMapFragment.OnGuidedClickListener listener) {
        mapFragment.setGuidedClickListener(listener);
    }

    public void addMapMarkerProvider(DroneMap.MapMarkerProvider provider) {
        mapFragment.addMapMarkerProvider(provider);
    }

    public void removeMapMarkerProvider(DroneMap.MapMarkerProvider provider) {
        mapFragment.removeMapMarkerProvider(provider);
    }

    private static class SlidingPanelListenerManager implements SlidingUpPanelLayout.PanelSlideListener {
        private final HashMap<String, SlidingUpPanelLayout.PanelSlideListener> panelListenerClients = new HashMap<>();

        public void addPanelSlideListener(String label, SlidingUpPanelLayout.PanelSlideListener listener) {
            panelListenerClients.put(label, listener);
        }

        public void removePanelSlideListener(String label) {
            panelListenerClients.remove(label);
        }

        @Override
        public void onPanelSlide(View view, float v) {
            for (SlidingUpPanelLayout.PanelSlideListener listener : panelListenerClients.values()) {
                listener.onPanelSlide(view, v);
            }
        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            for (SlidingUpPanelLayout.PanelSlideListener listener : panelListenerClients.values()) {
                listener.onPanelStateChanged(panel, previousState, newState);
            }
        }
    }
}
