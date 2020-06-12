package org.farring.gcs.proxy.mission.item.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.waypoints.CircleImpl;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;

import java.util.List;

public class MissionCircleFragment extends MissionDetailFragment implements CardWheelHorizontalView.OnCardWheelScrollListener {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_circle;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getActivity().getApplicationContext();

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.CIRCLE));

        final LengthUnitProvider lengthUP = getLengthUnitProvider();

        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE));

        CardWheelHorizontalView<LengthUnit> altitudePicker = (CardWheelHorizontalView<LengthUnit>) view.findViewById(R.id.altitudePicker);
        altitudePicker.setViewAdapter(altitudeAdapter);
        altitudePicker.addScrollListener(this);

        final NumericWheelAdapter loiterTurnAdapter = new NumericWheelAdapter(context,
                R.layout.wheel_text_centered, 0, 50, "%d");

        CardWheelHorizontalView<Integer> loiterTurnPicker = (CardWheelHorizontalView<Integer>) view.findViewById(R.id.loiterTurnPicker);
        loiterTurnPicker.setViewAdapter(loiterTurnAdapter);
        loiterTurnPicker.addScrollListener(this);

        final LengthWheelAdapter loiterRadiusAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(Utils.MIN_DISTANCE), lengthUP.boxBaseValueToTarget(Utils.MAX_DISTANCE));
        CardWheelHorizontalView<LengthUnit> loiterRadiusPicker = (CardWheelHorizontalView<LengthUnit>) view.findViewById(R.id.loiterRadiusPicker);
        loiterRadiusPicker.setViewAdapter(loiterRadiusAdapter);
        loiterRadiusPicker.addScrollListener(this);

        // Use the first one as reference.
        final CircleImpl firstItem = getMissionItems().get(0);
        altitudePicker.setCurrentValue(lengthUP.boxBaseValueToTarget(firstItem.getCoordinate().getAltitude()));
        loiterTurnPicker.setCurrentValue(firstItem.getNumberOfTurns());
        loiterRadiusPicker.setCurrentValue(lengthUP.boxBaseValueToTarget(firstItem.getRadius()));
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Object startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Object oldValue, Object newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView cardWheel, Object startValue, Object endValue) {
        switch (cardWheel.getId()) {
            case R.id.altitudePicker: {
                final double baseValue = ((LengthUnit) endValue).toBase().getValue();
                for (CircleImpl item : getMissionItems()) {
                    item.getCoordinate().setAltitude(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
            }

            case R.id.loiterRadiusPicker: {
                final double baseValue = ((LengthUnit) endValue).toBase().getValue();
                for (CircleImpl item : getMissionItems()) {
                    item.setRadius(baseValue);
                }

                MissionProxy missionProxy = getMissionProxy();
                if (missionProxy != null)
                    missionProxy.notifyMissionUpdate();
                break;
            }

            case R.id.loiterTurnPicker:
                int turns = (Integer) endValue;
                for (CircleImpl item : getMissionItems()) {
                    item.setTurns(turns);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }

    @Override
    public List<CircleImpl> getMissionItems() {
        return (List<CircleImpl>) super.getMissionItems();
    }
}
