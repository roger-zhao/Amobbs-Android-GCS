package org.farring.gcs.proxy.mission.item.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.waypoints.StructureScannerImpl;
import com.dronekit.core.survey.CameraInfo;
import com.dronekit.core.survey.SurveyData;

import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.R.id;
import org.farring.gcs.proxy.mission.item.adapters.CamerasAdapter;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView.OnCardWheelScrollListener;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;
import org.farring.gcs.view.spinners.SpinnerSelfSelect;

import java.util.List;

public class MissionStructureScannerFragment extends MissionDetailFragment implements OnCardWheelScrollListener, OnCheckedChangeListener {

    private CamerasAdapter cameraAdapter;
    private final SpinnerSelfSelect.OnSpinnerItemSelectedListener cameraSpinnerListener = new SpinnerSelfSelect.OnSpinnerItemSelectedListener() {
        @Override
        public void onSpinnerItemSelected(Spinner spinner, int position) {
            if (spinner.getId() == id.cameraFileSpinner) {

                if (cameraAdapter.isEmpty())
                    return;

                CameraInfo cameraInfo = cameraAdapter.getItem(position);
                for (StructureScannerImpl scan : getMissionItems()) {
                    SurveyData surveyDetail = scan.getSurveyData();
                    surveyDetail.setCameraInfo(cameraInfo);
                }

                submitForBuilding();
            }
        }
    };

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_structure_scanner;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getActivity().getApplicationContext();

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.CYLINDRICAL_SURVEY));

        cameraAdapter = new CamerasAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, getDrone().getCameraDetails());
        SpinnerSelfSelect cameraSpinner = (SpinnerSelfSelect) view.findViewById(id.cameraFileSpinner);
        cameraSpinner.setAdapter(cameraAdapter);
        cameraSpinner.setOnSpinnerItemSelectedListener(cameraSpinnerListener);

        final LengthUnitProvider lengthUP = getLengthUnitProvider();

        CardWheelHorizontalView<LengthUnit> radiusPicker = (CardWheelHorizontalView) view.findViewById(R.id.radiusPicker);
        radiusPicker.setViewAdapter(new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(Utils.MIN_DISTANCE), lengthUP.boxBaseValueToTarget(Utils.MAX_DISTANCE)));
        radiusPicker.addScrollListener(this);

        CardWheelHorizontalView<LengthUnit> startAltitudeStepPicker = (CardWheelHorizontalView) view.findViewById(R.id.startAltitudePicker);
        startAltitudeStepPicker.setViewAdapter(new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE)));
        startAltitudeStepPicker.addScrollListener(this);

        CardWheelHorizontalView<LengthUnit> endAltitudeStepPicker = (CardWheelHorizontalView) view.findViewById(R.id.heightStepPicker);
        endAltitudeStepPicker.setViewAdapter(new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE)));
        endAltitudeStepPicker.addScrollListener(this);

        CardWheelHorizontalView<Integer> numberStepsPicker = (CardWheelHorizontalView<Integer>) view.findViewById(R.id.stepsPicker);
        numberStepsPicker.setViewAdapter(new NumericWheelAdapter(context, R.layout.wheel_text_centered, 1, 100, "%d"));
        numberStepsPicker.addScrollListener(this);

        CheckBox checkBoxAdvanced = (CheckBox) view.findViewById(R.id.checkBoxSurveyCrossHatch);
        checkBoxAdvanced.setOnCheckedChangeListener(this);

        // Use the first one as reference.
        final StructureScannerImpl firstItem = getMissionItems().get(0);

        final int cameraSelection = cameraAdapter.getPosition(firstItem.getSurveyData().getCameraInfo());
        cameraSpinner.setSelection(Math.max(cameraSelection, 0));

        radiusPicker.setCurrentValue(lengthUP.boxBaseValueToTarget(firstItem.getRadius()));
        startAltitudeStepPicker.setCurrentValue(lengthUP.boxBaseValueToTarget(firstItem.getCoordinate().getAltitude()));
        endAltitudeStepPicker.setCurrentValue(lengthUP.boxBaseValueToTarget(firstItem.getEndAltitude()));
        numberStepsPicker.setCurrentValue(firstItem.getNumberOfSteps());
        checkBoxAdvanced.setChecked(firstItem.isCrossHatchEnabled());
    }

    private void submitForBuilding() {
        final List<StructureScannerImpl> scannerList = getMissionItems();
        if (scannerList.isEmpty()) return;

        getMissionProxy().notifyMissionUpdate();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        for (StructureScannerImpl item : getMissionItems()) {
            item.enableCrossHatch(isChecked);
        }

        submitForBuilding();
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
            case R.id.radiusPicker: {
                final double radius = ((LengthUnit) endValue).toBase().getValue();
                for (StructureScannerImpl item : getMissionItems()) {
                    item.setRadius(radius);
                }
                break;
            }

            case R.id.startAltitudePicker: {
                final double altitude = ((LengthUnit) endValue).toBase().getValue();
                for (StructureScannerImpl item : getMissionItems()) {
                    item.getCoordinate().setAltitude(altitude);
                }
                break;
            }

            case R.id.heightStepPicker: {
                final double heightStep = ((LengthUnit) endValue).toBase().getValue();
                for (StructureScannerImpl item : getMissionItems()) {
                    item.setAltitudeStep((int) heightStep);
                }
                break;
            }

            case R.id.stepsPicker:
                final int stepsCount = (Integer) endValue;
                for (StructureScannerImpl item : getMissionItems()) {
                    item.setNumberOfSteps(stepsCount);
                }
                break;
        }

        getMissionProxy().notifyMissionUpdate();
    }

    @Override
    protected List<StructureScannerImpl> getMissionItems() {
        return (List<StructureScannerImpl>) super.getMissionItems();
    }
}

