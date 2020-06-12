package org.farring.gcs.proxy.mission.item.fragments;

import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.CameraTriggerImpl;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;

public class MissionCameraTriggerFragment extends MissionDetailFragment
        implements CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit> {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_camera_trigger;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.CAMERA_TRIGGER));

        CameraTriggerImpl item = (CameraTriggerImpl) getMissionItems().get(0);

        final LengthUnitProvider lengthUnitProvider = getLengthUnitProvider();
        final LengthWheelAdapter adapter = new LengthWheelAdapter(getContext(), R.layout.wheel_text_centered,
                lengthUnitProvider.boxBaseValueToTarget(Utils.MIN_DISTANCE),
                lengthUnitProvider.boxBaseValueToTarget(Utils.MAX_DISTANCE));
        final CardWheelHorizontalView<LengthUnit> cardAltitudePicker = (CardWheelHorizontalView<LengthUnit>) view.findViewById(R.id.picker1);
        cardAltitudePicker.setViewAdapter(adapter);
        cardAltitudePicker.addScrollListener(this);
        cardAltitudePicker.setCurrentValue(lengthUnitProvider.boxBaseValueToTarget(item.getTriggerDistance()));
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
            case R.id.picker1:
                double baseValue = endValue.toBase().getValue();
                for (MissionItemImpl missionItem : getMissionItems()) {
                    CameraTriggerImpl item = (CameraTriggerImpl) missionItem;
                    item.setTriggerDistance(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
