package org.farring.gcs.fragments.mode;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.gcs.follow.Follow;
import com.dronekit.core.gcs.follow.FollowAlgorithm;
import com.dronekit.core.gcs.follow.FollowAlgorithm.FollowModes;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;
import com.orhanobut.logger.Logger;

import org.beyene.sius.unit.length.LengthUnit;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.DroneMap;
import org.farring.gcs.graphic.map.GuidedScanROIMarkerInfo;
import org.farring.gcs.maps.MarkerInfo;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.view.spinnerWheel.CardWheelHorizontalView;
import org.farring.gcs.view.spinnerWheel.adapters.LengthWheelAdapter;

import java.util.HashMap;
import java.util.Map;

public class ModeFollowFragment extends ModeGuidedFragment implements AdapterView.OnItemSelectedListener, DroneMap.MapMarkerProvider {

    private static final double DEFAULT_MIN_RADIUS = 2; //meters
    private static final int ROI_TARGET_MARKER_INDEX = 0;
    private final GuidedScanROIMarkerInfo roiMarkerInfo = new GuidedScanROIMarkerInfo();
    private final MarkerInfo[] emptyMarkers = {};
    private final MarkerInfo[] markers = new MarkerInfo[1];
    private TextView modeDescription;
    private Spinner spinner;
    private ArrayAdapter<FollowModes> adapter;
    private CardWheelHorizontalView<LengthUnit> mRadiusWheel;
    private CardWheelHorizontalView<LengthUnit> roiHeightWheel;

    {
        markers[ROI_TARGET_MARKER_INDEX] = roiMarkerInfo;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case FOLLOW_UPDATE:
                final Follow followState = dpApp.getDroneManager().getFollowMe();
                if (followState != null) {
                    final FollowModes followType = followState.getFollowAlgorithm().getType();
                    spinner.setSelection(adapter.getPosition(followType));
                    onFollowTypeUpdate(followType, followState.getFollowAlgorithm().getParams());
                }
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mode_follow, container, false);
    }

    @Override
    public void onViewCreated(View parentView, Bundle savedInstanceState) {
        super.onViewCreated(parentView, savedInstanceState);

        modeDescription = (TextView) parentView.findViewById(R.id.ModeDetail);

        final Context context = getContext();
        final LengthUnitProvider lengthUP = getLengthUnitProvider();

        final DroidPlannerPrefs dpPrefs = getAppPrefs();

        final LengthWheelAdapter radiusAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(Utils.MIN_DISTANCE), lengthUP.boxBaseValueToTarget(Utils.MAX_DISTANCE));

        mRadiusWheel = (CardWheelHorizontalView<LengthUnit>) parentView.findViewById(R.id.radius_spinner);
        mRadiusWheel.setViewAdapter(radiusAdapter);
        mRadiusWheel.addScrollListener(this);

        final LengthWheelAdapter roiHeightAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(dpPrefs.getMinAltitude()), lengthUP.boxBaseValueToTarget(dpPrefs.getMaxAltitude()));

        roiHeightWheel = (CardWheelHorizontalView<LengthUnit>) parentView.findViewById(R.id.roi_height_spinner);
        roiHeightWheel.setViewAdapter(roiHeightAdapter);
        roiHeightWheel.addScrollListener(this);

        spinner = (Spinner) parentView.findViewById(R.id.follow_type_spinner);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, FollowModes.values());
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mRadiusWheel != null) {
            mRadiusWheel.removeChangingListener(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        final FollowAlgorithm followAlgorithm = dpApp.getDroneManager().getFollowMe().getFollowAlgorithm();
        // 获取当前类型
        final FollowModes followType = followAlgorithm.getType();
        // 设置当前类型
        spinner.setSelection(adapter.getPosition(followType));
        onFollowTypeUpdate(followType, followAlgorithm.getParams());

        parent.addMapMarkerProvider(this);
    }

    private void onFollowTypeUpdate(FollowModes followType, Map<String, Object> params) {
        if (followType == null)
            return;

        updateModeDescription(followType);

        // 更新半径
        if (followType.hasParam(FollowModes.EXTRA_FOLLOW_RADIUS)) {
            double radius = DEFAULT_MIN_RADIUS;
            if (params != null) {
                if (params.get(FollowModes.EXTRA_FOLLOW_RADIUS) != null)
                    radius = (Double) params.get(FollowModes.EXTRA_FOLLOW_RADIUS);
            }

            mRadiusWheel.setVisibility(View.VISIBLE);
            mRadiusWheel.setCurrentValue((getLengthUnitProvider().boxBaseValueToTarget(radius)));
        } else {
            mRadiusWheel.setVisibility(View.GONE);
        }

        // 更新ROI高度
        double roiHeight = GuidedScanROIMarkerInfo.DEFAULT_FOLLOW_ROI_ALTITUDE;
        LatLong roiTarget = null;
        if (followType.hasParam(FollowModes.EXTRA_FOLLOW_ROI_TARGET)) {
            roiTarget = roiMarkerInfo.getPosition();

            if (params != null) {
                roiTarget = (LatLong) params.get(FollowModes.EXTRA_FOLLOW_ROI_TARGET);
            }

            if (roiTarget instanceof LatLongAlt)
                roiHeight = ((LatLongAlt) roiTarget).getAltitude();
        }

        roiHeightWheel.setCurrentValue(getLengthUnitProvider().boxBaseValueToTarget(roiHeight));
        updateROITargetMarker(roiTarget);
    }

    // 更新描述
    private void updateModeDescription(FollowModes followType) {
        if (followType == null)
            return;

        switch (followType) {
            case GUIDED_SCAN:
                modeDescription.setText(R.string.mode_follow_guided_scan);
                break;

            default:
                modeDescription.setText(R.string.mode_follow);
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        parent.removeMapMarkerProvider(this);
    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView cardWheel, LengthUnit oldValue, LengthUnit newValue) {
        final Drone drone = getDrone();
        switch (cardWheel.getId()) {
            case R.id.radius_spinner:
                // 更改半径
                if (drone.isConnected()) {
                    Map<String, Object> params = new HashMap<>();
                    params.put(FollowModes.EXTRA_FOLLOW_RADIUS, newValue.toBase().getValue());
                    dpApp.getDroneManager().getFollowMe().getFollowAlgorithm().updateAlgorithmParams(params);
                    Logger.i("更改半径：" + newValue.toBase().getValue());
                }
                break;

            case R.id.roi_height_spinner:
                // 更改ROI高度
                if (drone.isConnected()) {
                    final LatLongAlt roiCoord = roiMarkerInfo.getPosition();
                    if (roiCoord != null) {
                        roiCoord.setAltitude(newValue.toBase().getValue());
                        pushROITargetToVehicle(roiCoord);
                        Logger.i("更改高度：" + newValue.toBase().getValue());
                    }
                }
                break;

            default:
                super.onScrollingEnded(cardWheel, oldValue, newValue);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // 获取所选的模式
        final FollowModes selectedMode = adapter.getItem(position);

        // 获取对象
        final Drone drone = getDrone();
        if (drone.isConnected()) {
            final Follow followMe = dpApp.getDroneManager().getFollowMe();
            if (!followMe.isEnabled())
                followMe.enableFollowMe();

            // 获取当前模式
            FollowAlgorithm currentAlg = followMe.getFollowAlgorithm();
            Logger.i("当前跟随模式：" + currentAlg.getType().toString());
            if (currentAlg.getType() != selectedMode) {
                followMe.setAlgorithm(selectedMode.getAlgorithmType(dpApp.getDroneManager()));
                Logger.i("重新设置跟随模式：" + currentAlg.getType().toString());
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onGuidedClick(LatLong coord) {
        final Drone drone = getDrone();
        final Follow followState = dpApp.getDroneManager().getFollowMe();
        if (followState != null && followState.isEnabled() && followState.getFollowAlgorithm().getType().hasParam(FollowModes.EXTRA_FOLLOW_ROI_TARGET)) {
            Toast.makeText(getContext(), R.string.guided_scan_roi_set_message, Toast.LENGTH_LONG).show();

            final double roiHeight = roiHeightWheel.getCurrentValue().toBase().getValue();
            final LatLongAlt roiCoord = new LatLongAlt(coord.getLatitude(), coord.getLongitude(), roiHeight);

            pushROITargetToVehicle(roiCoord);
            updateROITargetMarker(coord);
        } else {
            super.onGuidedClick(coord);
        }
    }

    private void pushROITargetToVehicle(LatLongAlt roiCoord) {
        if (roiCoord == null)
            return;

        Map<String, Object> params = new HashMap<>();
        params.put(FollowModes.EXTRA_FOLLOW_ROI_TARGET, roiCoord);
        dpApp.getDroneManager().getFollowMe().getFollowAlgorithm().updateAlgorithmParams(params);
    }

    private void updateROITargetMarker(LatLong target) {
        roiMarkerInfo.setPosition(target);
        EventBus.getDefault().post(ActionEvent.ACTION_UPDATE_MAP);
        if (target == null) {
            roiHeightWheel.setVisibility(View.GONE);
        } else {
            roiHeightWheel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public MarkerInfo[] getMapMarkers() {
        if (roiMarkerInfo.isVisible())
            return markers;
        else
            return emptyMarkers;
    }
}
