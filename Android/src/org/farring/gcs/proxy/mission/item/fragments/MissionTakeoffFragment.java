package org.farring.gcs.proxy.mission.item.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.TakeoffImpl;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;

public class MissionTakeoffFragment extends MissionDetailFragment implements CardWheelHorizontalView.OnCardWheelScrollListener {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_takeoff;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getContext();

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.TAKEOFF));

        final LengthUnitProvider lengthUP = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE));
        CardWheelHorizontalView<LengthUnit> cardAltitudePicker = (CardWheelHorizontalView) view.findViewById(R.id.altitudePicker);
        cardAltitudePicker.setViewAdapter(altitudeAdapter);
        cardAltitudePicker.addScrollListener(this);

        final NumericWheelAdapter pitchAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 90, "%d°");
        final CardWheelHorizontalView<Integer> pitchPicker = (CardWheelHorizontalView) view.findViewById(R.id.pitchPicker);
        pitchPicker.setViewAdapter(pitchAdapter);
        pitchPicker.addScrollListener(this);

        TakeoffImpl item = (TakeoffImpl) getMissionItems().get(0);
        cardAltitudePicker.setCurrentValue(lengthUP.boxBaseValueToTarget(item.getFinishedAlt()));
        pitchPicker.setCurrentValue((int) item.getPitch());
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
                for (MissionItemImpl missionItem : getMissionItems()) {
                    TakeoffImpl item = (TakeoffImpl) missionItem;
                    item.setFinishedAlt(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;

            case R.id.pitchPicker:
                final int pitch = (Integer) endValue;
                for (MissionItemImpl missionItem : getMissionItems()) {
                    ((TakeoffImpl) missionItem).setPitch(pitch);
                }

                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
