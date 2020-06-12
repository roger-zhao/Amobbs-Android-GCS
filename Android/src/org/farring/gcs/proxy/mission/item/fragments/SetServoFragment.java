package org.farring.gcs.proxy.mission.item.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.SetServoImpl;

import org.farring.gcs.R;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;

public class SetServoFragment extends MissionDetailFragment implements CardWheelHorizontalView
        .OnCardWheelScrollListener<Integer> {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_set_servo;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.SET_SERVO));

        final SetServoImpl item = (SetServoImpl) getMissionItems().get(0);
        final Context context = getContext();

        final NumericWheelAdapter adapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 1, 8, "%d");
        final CardWheelHorizontalView<Integer> cardChannelPicker = (CardWheelHorizontalView) view.findViewById(R.id.picker1);
        cardChannelPicker.setViewAdapter(adapter);
        cardChannelPicker.addScrollListener(this);
        cardChannelPicker.setCurrentValue(item.getChannel());

        final NumericWheelAdapter pwmAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 2000, "%d");
        final CardWheelHorizontalView<Integer> pwmPicker = (CardWheelHorizontalView) view.findViewById(R.id.pwmPicker);
        pwmPicker.setViewAdapter(pwmAdapter);
        pwmPicker.addScrollListener(this);
        pwmPicker.setCurrentValue(item.getPwm());
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Integer startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Integer oldValue, Integer newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, Integer startValue, Integer endValue) {
        switch (wheel.getId()) {
            case R.id.picker1:
                for (MissionItemImpl missionItem : getMissionItems()) {
                    SetServoImpl item = (SetServoImpl) missionItem;
                    item.setChannel(endValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;

            case R.id.pwmPicker:
                for (MissionItemImpl missionItem : getMissionItems()) {
                    SetServoImpl item = (SetServoImpl) missionItem;
                    item.setPwm(endValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
