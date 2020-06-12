package org.farring.gcs.fragments.mode;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.variables.GuidedPoint;
import com.dronekit.core.drone.variables.Type;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.fragments.FlightDataFragment;
import org.farring.gcs.fragments.FlightMapFragment;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;

public class ModeGuidedFragment extends BaseFragment implements CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit>, FlightMapFragment.OnGuidedClickListener {

    protected FlightDataFragment parent;
    private CardWheelHorizontalView<LengthUnit> mAltitudeWheel;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        final Fragment parentFragment = getParentFragment().getParentFragment();
        if (!(parentFragment instanceof FlightDataFragment)) {
            throw new IllegalStateException("Parent fragment must be an instance of " + FlightDataFragment.class.getName());
        }

        parent = (FlightDataFragment) parentFragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        parent = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mode_guided, container, false);
    }

    @Override
    public void onViewCreated(View parentView, Bundle savedInstanceState) {
        super.onViewCreated(parentView, savedInstanceState);

        final DroidPlannerPrefs dpPrefs = getAppPrefs();

        final LengthUnitProvider lengthUnitProvider = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(getContext(), R.layout.wheel_text_centered,
                lengthUnitProvider.boxBaseValueToTarget(dpPrefs.getMinAltitude()),
                lengthUnitProvider.boxBaseValueToTarget(dpPrefs.getMaxAltitude()));

        mAltitudeWheel = (CardWheelHorizontalView<LengthUnit>) parentView.findViewById(R.id.altitude_spinner);
        mAltitudeWheel.setViewAdapter(altitudeAdapter);
        mAltitudeWheel.addScrollListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAltitudeWheel != null) {
            mAltitudeWheel.removeChangingListener(this);
        }
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, LengthUnit startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, LengthUnit oldValue, LengthUnit newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView cardWheel, LengthUnit startValue, LengthUnit endValue) {
        switch (cardWheel.getId()) {
            case R.id.altitude_spinner:
                final Drone drone = getDrone();
                if (drone.isConnected())
                    drone.getGuidedPoint().changeGuidedAltitude(endValue.toBase().getValue());
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final Drone drone = getDrone();

        if (mAltitudeWheel != null) {
            final DroidPlannerPrefs dpPrefs = getAppPrefs();

            final double maxAlt = dpPrefs.getMaxAltitude();
            final double minAlt = dpPrefs.getMinAltitude();
            final double defaultAlt = dpPrefs.getDefaultAltitude();

            GuidedPoint guidedState = drone.getGuidedPoint();
            if (guidedState.getLatLongAlt() != null) {
                LatLongAlt coordinate = guidedState.getLatLongAlt();

                final double baseValue = Math.min(maxAlt, Math.max(minAlt, coordinate == null ? defaultAlt : coordinate.getAltitude()));
                final LengthUnit initialValue = getLengthUnitProvider().boxBaseValueToTarget(baseValue);
                mAltitudeWheel.setCurrentValue(initialValue);
            }
        }

        parent.setGuidedClickListener(this);
        Type droneType = drone.getType();
        if (droneType.getType() == Type.TYPE_ROVER) {
            mAltitudeWheel.setVisibility(View.GONE);
        } else {
            mAltitudeWheel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        parent.setGuidedClickListener(null);
    }

    @Override
    public void onGuidedClick(LatLong coord) {
        final Drone drone = getDrone();
        GuidedPoint guidedPoint = drone.getGuidedPoint();
        if (guidedPoint.isInitialized()) {
            guidedPoint.newGuidedCoord(coord);
        }
    }
}
