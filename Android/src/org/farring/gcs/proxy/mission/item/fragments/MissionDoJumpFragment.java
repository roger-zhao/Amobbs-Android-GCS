package org.farring.gcs.proxy.mission.item.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.DoJumpImpl;

import org.farring.gcs.R;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;

/**
 * Created by Toby on 7/31/2015.
 */
public class MissionDoJumpFragment extends MissionDetailFragment implements CardWheelHorizontalView.OnCardWheelScrollListener<Integer> {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_do_jump;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.DO_JUMP));

        final DoJumpImpl item = (DoJumpImpl) getMissionItems().get(0);
        final Context context = getContext();

        final MissionProxy missionProxy = getMissionProxy();
        final NumericWheelAdapter adapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, missionProxy.getFirstWaypoint(), missionProxy.getLastWaypoint(), "%d");
        final CardWheelHorizontalView<Integer> waypointPicker = (CardWheelHorizontalView) view.findViewById(R.id.waypoint_picker);
        waypointPicker.setViewAdapter(adapter);
        waypointPicker.addScrollListener(this);
        waypointPicker.setCurrentValue(item.getWaypoint());

        final int repeatCount = item.getRepeatCount();
        final boolean isIndefinite = repeatCount == -1;

        final NumericWheelAdapter repeatCountAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 1337, "%d");
        final CardWheelHorizontalView<Integer> repeatCountPicker = (CardWheelHorizontalView) view.findViewById(R.id.repeat_picker);
        repeatCountPicker.setViewAdapter(repeatCountAdapter);
        repeatCountPicker.addScrollListener(this);
        repeatCountPicker.setCurrentValue(isIndefinite ? 0 : repeatCount);
        repeatCountPicker.setVisibility(isIndefinite ? View.GONE : View.VISIBLE);

        final CheckBox indefiniteRepeatCheck = (CheckBox) view.findViewById(R.id.repeat_indefinitely_toggle);
        indefiniteRepeatCheck.setChecked(isIndefinite);
        indefiniteRepeatCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final int newRepeatCount = isChecked ? -1 : repeatCountPicker.getCurrentValue();
                repeatCountPicker.setVisibility(isChecked ? View.GONE : View.VISIBLE);

                for (MissionItemImpl missionItem : getMissionItems()) {
                    final DoJumpImpl item = (DoJumpImpl) missionItem;
                    item.setRepeatCount(newRepeatCount);
                }

                getMissionProxy().notifyMissionUpdate();
            }
        });
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
            case R.id.waypoint_picker:
                for (MissionItemImpl missionItem : getMissionItems()) {
                    DoJumpImpl item = (DoJumpImpl) missionItem;
                    item.setWaypoint(endValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;

            case R.id.repeat_picker:
                for (MissionItemImpl missionItem : getMissionItems()) {
                    DoJumpImpl item = (DoJumpImpl) missionItem;
                    item.setRepeatCount(endValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
