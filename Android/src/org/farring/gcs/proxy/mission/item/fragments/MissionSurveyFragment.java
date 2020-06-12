package org.farring.gcs.proxy.mission.item.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.survey.SplineSurveyImpl;
import com.dronekit.core.mission.survey.SurveyImpl;
import com.dronekit.core.survey.CameraInfo;
import com.dronekit.core.survey.SurveyData;
import com.evenbus.ActionEvent;

import org.beyene.sius.unit.length.LengthUnit;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.R.id;
import org.farring.gcs.proxy.mission.item.adapters.CamerasAdapter;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView.OnCardWheelScrollListener;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;
import org.farring.gcs.view.spinnerWheel.adapters.NumericWheelAdapter;
import org.farring.gcs.view.spinners.SpinnerSelfSelect;
import org.farring.gcs.view.spinners.SpinnerSelfSelect.OnSpinnerItemSelectedListener;

import java.util.List;

public class MissionSurveyFragment<T extends SurveyImpl> extends MissionDetailFragment implements OnCardWheelScrollListener {

    private static final String TAG = MissionSurveyFragment.class.getSimpleName();

    public TextView waypointType;
    public TextView distanceBetweenLinesTextView;
    public TextView areaTextView;
    public TextView distanceTextView;
    public TextView footprintTextView;
    public TextView groundResolutionTextView;
    public TextView numberOfPicturesView;
    public TextView numberOfStripsView;
    public TextView lengthView;

    private CardWheelHorizontalView<Integer> mOverlapPicker;
    private CardWheelHorizontalView<Integer> mAnglePicker;
    private CardWheelHorizontalView<LengthUnit> mAltitudePicker;
    private CardWheelHorizontalView<Integer> mSidelapPicker;
    private CardWheelHorizontalView<LengthUnit> mwpWidthPicker;
    private CardWheelHorizontalView<Integer> delayPicker; //  = (CardWheelHorizontalView<Integer>) view.findViewById(R.id.waypointDelayPicker);


    private CamerasAdapter cameraAdapter;
    private final OnSpinnerItemSelectedListener cameraSpinnerListener = new OnSpinnerItemSelectedListener() {
        @Override
        public void onSpinnerItemSelected(Spinner spinner, int position) {
            if (spinner.getId() == id.cameraFileSpinner) {
                if (cameraAdapter.isEmpty())
                    return;

                CameraInfo cameraInfo = cameraAdapter.getItem(position);

                for (T survey : getMissionItems()) {
                    survey.setCameraInfo(cameraInfo);
                }

                onScrollingEnded(mAnglePicker, 0, 0);
            }
        }
    };
    private SpinnerSelfSelect cameraSpinner;

    @Override
    protected List<T> getMissionItems() {
        return (List<T>) super.getMissionItems();
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_MISSION_PROXY_UPDATE:
                updateViews();
                break;
        }
        super.onReceiveActionEvent(actionEvent);
    }

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_survey;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Context context = getContext();
        waypointType = (TextView) view.findViewById(id.WaypointType);

        cameraAdapter = new CamerasAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, getDrone().getCameraDetails());

        cameraSpinner = (SpinnerSelfSelect) view.findViewById(id.cameraFileSpinner);
        cameraSpinner.setAdapter(cameraAdapter);
        cameraSpinner.setOnSpinnerItemSelectedListener(cameraSpinnerListener);

        mAnglePicker = (CardWheelHorizontalView) view.findViewById(id.anglePicker);
        mAnglePicker.setViewAdapter(new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 180, "%dº"));

        delayPicker = (CardWheelHorizontalView) view.findViewById(id.waypointDelayPicker);
        delayPicker.setViewAdapter(new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 60, "%d 秒"));

        mOverlapPicker = (CardWheelHorizontalView) view.findViewById(id.overlapPicker);
        mOverlapPicker.setViewAdapter(new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 99, "%d %%"));

        mSidelapPicker = (CardWheelHorizontalView) view.findViewById(R.id.sidelapPicker);
        mSidelapPicker.setViewAdapter(new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0, 99, "%d %%"));

        final LengthUnitProvider lengthUP = getLengthUnitProvider();
        mAltitudePicker = (CardWheelHorizontalView) view.findViewById(R.id.altitudePicker);
        mAltitudePicker.setViewAdapter(new LengthWheelAdapter(context, R.layout.wheel_text_centered, lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE)));

        final LengthUnitProvider lengthUP1 = getLengthUnitProvider();
        mwpWidthPicker = (CardWheelHorizontalView) view.findViewById(id.wpWidthPicker);
        mwpWidthPicker.setViewAdapter(new LengthWheelAdapter(context, R.layout.wheel_text_centered, lengthUP1.boxBaseValueToTarget(3), lengthUP1.boxBaseValueToTarget(200)));


        areaTextView = (TextView) view.findViewById(R.id.areaTextView);
        distanceBetweenLinesTextView = (TextView) view.findViewById(R.id.distanceBetweenLinesTextView);
        footprintTextView = (TextView) view.findViewById(R.id.footprintTextView);
        groundResolutionTextView = (TextView) view.findViewById(R.id.groundResolutionTextView);
        distanceTextView = (TextView) view.findViewById(R.id.distanceTextView);
        numberOfPicturesView = (TextView) view.findViewById(R.id.numberOfPicturesTextView);
        numberOfStripsView = (TextView) view.findViewById(R.id.numberOfStripsTextView);
        lengthView = (TextView) view.findViewById(R.id.lengthTextView);

        updateViews();
        updateCamera();

        mAnglePicker.addScrollListener(this);
        mOverlapPicker.addScrollListener(this);
        mSidelapPicker.addScrollListener(this);
        mAltitudePicker.addScrollListener(this);
        mwpWidthPicker.addScrollListener(this);
        delayPicker.addScrollListener(this);

        if (!getMissionItems().isEmpty()) {
            final T referenceItem = getMissionItems().get(0);
            if (referenceItem instanceof SplineSurveyImpl)
                typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.SPLINE_SURVEY));
            else
                typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.SURVEY));
        }
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
            case R.id.anglePicker:
            case R.id.altitudePicker:
            case R.id.overlapPicker:
            case R.id.sidelapPicker:
            case R.id.wpWidthPicker:
            case id.waypointDelayPicker:
                try {
                    final List<T> surveyList = getMissionItems();
                    if (!surveyList.isEmpty()) {
                        for (final T survey : surveyList) {
                            // 更新数据
                            survey.update(mAnglePicker.getCurrentValue(),
                                    mAltitudePicker.getCurrentValue().toBase().getValue(),
                                    mOverlapPicker.getCurrentValue(),
                                    mSidelapPicker.getCurrentValue(),
                                    mwpWidthPicker.getCurrentValue().toBase().getValue(),
                                    delayPicker.getCurrentValue()
                            );

                            // 开始构建！
                            survey.build();
                            getMissionProxy().notifyMissionUpdate();
                        }
                        mAltitudePicker.setBackgroundResource(R.drawable.bg_cell_white);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error while building the survey.", e);
                    mAltitudePicker.setBackgroundColor(Color.RED);
                    Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void updateViews() {
        if (getActivity() == null)
            return;

        updateTextViews();
        updateSeekBars();
    }

    private void updateSeekBars() {
        List<T> surveyList = getMissionItems();
        if (!surveyList.isEmpty()) {
            T survey = surveyList.get(0);
            SurveyData surveyDetail = survey.getSurveyData();
            if (surveyDetail != null) {
                mAnglePicker.setCurrentValue((int) surveyDetail.getAngle());
                mOverlapPicker.setCurrentValue((int) surveyDetail.getOverlap());
                mSidelapPicker.setCurrentValue((int) surveyDetail.getSidelap());
                mAltitudePicker.setCurrentValue(getLengthUnitProvider().boxBaseValueToTarget(surveyDetail.getAltitude()));
            }
        }
    }

    private void updateCamera() {
        List<T> surveyList = getMissionItems();
        if (!surveyList.isEmpty()) {
            T survey = surveyList.get(0);
            final int cameraSelection = cameraAdapter.getPosition(survey.getSurveyData().getCameraInfo());
            cameraSpinner.setSelection(Math.max(cameraSelection, 0));
        }
    }

    private void updateTextViews() {
        return;
/*
        boolean setDefault = true;
        List<T> surveyList = getMissionItems();

        if (!surveyList.isEmpty()) {
            SurveyImpl survey = surveyList.get(0);
            SurveyData surveyDetail = survey.getSurveyData();
            if (survey instanceof SplineSurveyImpl) {
                waypointType.setText(getResources().getText(R.string.waypointType_Spline_Survey));
            }

            try {
                final LengthUnitProvider lengthUnitProvider = getLengthUnitProvider();
                final AreaUnitProvider areaUnitProvider = getAreaUnitProvider();

                footprintTextView.setText(String.format("%s: %s x %s", getString(R.string.footprint),
                        lengthUnitProvider.boxBaseValueToTarget(surveyDetail.getLateralFootPrint()),
                        lengthUnitProvider.boxBaseValueToTarget(surveyDetail.getLongitudinalFootPrint())));

                groundResolutionTextView.setText(String.format("%s: %s /px", getString(R.string.ground_resolution), areaUnitProvider.boxBaseValueToTarget(surveyDetail.getGroundResolution().valueInSqMeters())));

                distanceTextView.setText(String.format("%s: %s", getString(R.string.distance_between_pictures), lengthUnitProvider.boxBaseValueToTarget(surveyDetail.getLongitudinalPictureDistance())));

                distanceBetweenLinesTextView.setText(String.format("%s: %s", getString(R.string.distance_between_lines), lengthUnitProvider.boxBaseValueToTarget(surveyDetail.getLateralPictureDistance())));

                areaTextView.setText(String.format("%s: %s", getString(R.string.area), areaUnitProvider.boxBaseValueToTarget(survey.polygon.getArea().valueInSqMeters())));

                lengthView.setText(String.format("%s: %s", getString(R.string.mission_length), lengthUnitProvider.boxBaseValueToTarget(survey.grid.getLength())));

                numberOfPicturesView.setText(String.format("%s: %d", getString(R.string.pictures), survey.grid.getCameraCount()));

                numberOfStripsView.setText(String.format("%s: %d", getString(R.string.number_of_strips), survey.grid.getNumberOfLines()));

                setDefault = false;
            } catch (Exception e) {
                setDefault = true;
            }
        }

        if (setDefault) {
            footprintTextView.setText(getString(R.string.footprint) + ": ---");
            groundResolutionTextView.setText(getString(R.string.ground_resolution) + ": ---");
            distanceTextView.setText(getString(R.string.distance_between_pictures) + ": ---");
            distanceBetweenLinesTextView.setText(getString(R.string.distance_between_lines) + ": ---");
            areaTextView.setText(getString(R.string.area) + ": ---");
            lengthView.setText(getString(R.string.mission_length) + ": ---");
            numberOfPicturesView.setText(getString(R.string.pictures) + ": ---");
            numberOfStripsView.setText(getString(R.string.number_of_strips) + ": ---");
        }*/
    }
}
