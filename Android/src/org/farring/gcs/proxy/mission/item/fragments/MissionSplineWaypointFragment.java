package org.farring.gcs.proxy.mission.item.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.waypoints.SplineWaypointImpl;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;

/**
 * This class renders the detail view for a spline waypoint mission item.
 */
public class MissionSplineWaypointFragment extends MissionDetailFragment implements
        CardWheelHorizontalView.OnCardWheelScrollListener {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_spline_waypoint;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getContext();

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.SPLINE_WAYPOINT));

        final NumericWheelAdapter delayAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 60, "%d 秒");
        CardWheelHorizontalView<Integer> delayPicker = (CardWheelHorizontalView<Integer>) view.findViewById(R.id.waypointDelayPicker);
        delayPicker.setViewAdapter(delayAdapter);
        delayPicker.addScrollListener(this);

        final LengthUnitProvider lengthUP = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE));
        CardWheelHorizontalView<LengthUnit> altitudePicker = (CardWheelHorizontalView<LengthUnit>) view.findViewById(R.id.altitudePicker);
        altitudePicker.setViewAdapter(altitudeAdapter);
        altitudePicker.addScrollListener(this);

        SplineWaypointImpl item = (SplineWaypointImpl) getMissionItems().get(0);
        delayPicker.setCurrentValue((int) item.getDelay());
        altitudePicker.setCurrentValue(lengthUP.boxBaseValueToTarget(item.getCoordinate().getAltitude()));
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Object startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Object oldValue, Object newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, Object startValue, Object endValue) {
        switch (wheel.getId()) {
            case R.id.altitudePicker:
                final double baseValue = ((LengthUnit) endValue).toBase().getValue();
                for (MissionItemImpl item : getMissionItems()) {
                    ((SplineWaypointImpl) item).getCoordinate().setAltitude(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;

            case R.id.waypointDelayPicker:
                final int delay = (Integer) endValue;
                for (MissionItemImpl item : getMissionItems()) {
                    ((SplineWaypointImpl) item).setDelay(delay);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
