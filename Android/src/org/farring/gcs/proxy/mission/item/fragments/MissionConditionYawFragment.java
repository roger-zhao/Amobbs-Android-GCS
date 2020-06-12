package org.farring.gcs.proxy.mission.item.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.ConditionYawImpl;

import org.farring.gcs.R;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;

public class MissionConditionYawFragment extends MissionDetailFragment implements CardWheelHorizontalView.OnCardWheelScrollListener<Integer>, OnCheckedChangeListener {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_condition_yaw;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.CONDITION_YAW));

        ConditionYawImpl item = (ConditionYawImpl) getMissionItems().get(0);

        final NumericWheelAdapter adapter = new NumericWheelAdapter(getContext(), R.layout.wheel_text_centered, 0, 359, "%d°");
        final CardWheelHorizontalView<Integer> cardAltitudePicker = (CardWheelHorizontalView<Integer>) view.findViewById(R.id.picker1);
        cardAltitudePicker.setViewAdapter(adapter);
        cardAltitudePicker.addScrollListener(this);
        cardAltitudePicker.setCurrentValue((int) item.getAngle());

        CheckBox checkBoxRelative = (CheckBox) view.findViewById(R.id.checkBox1);
        checkBoxRelative.setOnCheckedChangeListener(this);
        checkBoxRelative.setChecked(item.isRelative());
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
                    ConditionYawImpl item = (ConditionYawImpl) missionItem;
                    item.setAngle(endValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.checkBox1) {
            for (MissionItemImpl missionItem : getMissionItems()) {
                ((ConditionYawImpl) missionItem).setRelative(isChecked);
            }
            getMissionProxy().notifyMissionUpdate();
        }
    }
}
