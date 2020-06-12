package org.farring.gcs.proxy.mission.item.fragments;

import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.waypoints.RegionOfInterestImpl;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;

public class MissionRegionOfInterestFragment extends MissionDetailFragment implements CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit> {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_roi;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.ROI));

        final LengthUnitProvider lengthUP = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(getContext(), R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE));
        CardWheelHorizontalView<LengthUnit> altitudePicker = (CardWheelHorizontalView<LengthUnit>) view.findViewById(R.id.altitudePicker);
        altitudePicker.setViewAdapter(altitudeAdapter);
        altitudePicker.addScrollListener(this);

        altitudePicker.setCurrentValue(lengthUP.boxBaseValueToTarget(((RegionOfInterestImpl) getMissionItems().get(0)).getCoordinate().getAltitude()));
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, LengthUnit startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, LengthUnit oldValue, LengthUnit newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, LengthUnit startValue, LengthUnit endValue) {
        switch (wheel.getId()) {
            case R.id.altitudePicker:
                final double baseValue = endValue.toBase().getValue();
                for (MissionItemImpl missionItem : getMissionItems()) {
                    ((RegionOfInterestImpl) missionItem).getCoordinate().setAltitude(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
