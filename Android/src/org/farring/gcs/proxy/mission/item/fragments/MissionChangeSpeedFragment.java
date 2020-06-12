package org.farring.gcs.proxy.mission.item.fragments;

import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.ChangeSpeedImpl;

import org.beyene.sius.unit.composition.speed.SpeedUnit;
import org.farring.gcs.R;
import org.farring.gcs.utils.unit.providers.speed.SpeedUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.SpeedWheelAdapter;

public class MissionChangeSpeedFragment extends MissionDetailFragment implements CardWheelHorizontalView.OnCardWheelScrollListener<SpeedUnit> {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_change_speed;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.CHANGE_SPEED));

        final SpeedUnitProvider speedUnitProvider = getSpeedUnitProvider();
        final SpeedWheelAdapter adapter = new SpeedWheelAdapter(getContext(), R.layout.wheel_text_centered,
                speedUnitProvider.boxBaseValueToTarget(0), speedUnitProvider.boxBaseValueToTarget(20));
        CardWheelHorizontalView<SpeedUnit> cardAltitudePicker = (CardWheelHorizontalView<SpeedUnit>) view.findViewById(R.id.picker1);
        cardAltitudePicker.setViewAdapter(adapter);
        cardAltitudePicker.addScrollListener(this);

        ChangeSpeedImpl item = (ChangeSpeedImpl) getMissionItems().get(0);
        cardAltitudePicker.setCurrentValue(speedUnitProvider.boxBaseValueToTarget(item.getSpeed()));
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, SpeedUnit startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, SpeedUnit oldValue, SpeedUnit newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, SpeedUnit startValue, SpeedUnit endValue) {
        switch (wheel.getId()) {
            case R.id.picker1:
                double baseValue = endValue.toBase().getValue();
                for (MissionItemImpl missionItem : getMissionItems()) {
                    ChangeSpeedImpl item = (ChangeSpeedImpl) missionItem;
                    item.setSpeed(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
