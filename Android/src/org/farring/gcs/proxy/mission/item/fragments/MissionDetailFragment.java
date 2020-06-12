package org.farring.gcs.proxy.mission.item.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Home;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.MissionItemType;
import com.dronekit.core.mission.commands.MissionCMD;
import com.dronekit.core.mission.survey.SurveyImpl;
import com.dronekit.core.mission.waypoints.SpatialCoordItem;
import com.dronekit.core.mission.waypoints.StructureScannerImpl;
import com.dronekit.core.survey.SurveyData;
import com.dronekit.utils.MathUtils;
import com.evenbus.AttributeEvent;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseDialogFragment;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;
import org.farring.gcs.proxy.mission.item.adapters.AdapterMissionItems;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.view.spinners.SpinnerSelfSelect;
import org.farring.gcs.view.spinners.SpinnerSelfSelect.OnSpinnerItemSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MissionDetailFragment extends BaseDialogFragment {

    public static final List<MissionItemType> typeWithNoMultiEditSupport = new ArrayList<>();
    private static final MissionItemType[] SUPPORTED_MISSION_ITEM_TYPES = {
            MissionItemType.WAYPOINT,
            MissionItemType.SPLINE_WAYPOINT,
            MissionItemType.CIRCLE,
            MissionItemType.ROI,
            MissionItemType.CHANGE_SPEED,
            MissionItemType.TAKEOFF,
            MissionItemType.LAND,
            MissionItemType.RTL,
            MissionItemType.CYLINDRICAL_SURVEY,
            MissionItemType.CAMERA_TRIGGER,
            MissionItemType.EPM_GRIPPER,
            MissionItemType.CONDITION_YAW,
            MissionItemType.SET_SERVO,
            MissionItemType.SPLINE_SURVEY,
            MissionItemType.DO_JUMP,
            MissionItemType.RESET_ROI
    };

    static {
        typeWithNoMultiEditSupport.add(MissionItemType.LAND);
        typeWithNoMultiEditSupport.add(MissionItemType.TAKEOFF);
        typeWithNoMultiEditSupport.add(MissionItemType.RTL);
        typeWithNoMultiEditSupport.add(MissionItemType.SURVEY);
        typeWithNoMultiEditSupport.add(MissionItemType.SPLINE_SURVEY);
    }

    private final List<MissionItemImpl> mSelectedItems = new ArrayList<MissionItemImpl>();
    private final List<MissionItemProxy> mSelectedProxies = new ArrayList<MissionItemProxy>();
    protected double MIN_ALTITUDE; // meters
    protected double MAX_ALTITUDE; // meters
    protected SpinnerSelfSelect typeSpinner;
    protected AdapterMissionItems commandAdapter;
    private OnMissionDetailListener mListener;
    private TextView distanceView;
    private TextView distanceLabelView;
    private MissionProxy mMissionProxy;

    private final OnSpinnerItemSelectedListener missionItemSpinnerListener = new OnSpinnerItemSelectedListener() {
        @Override
        public void onSpinnerItemSelected(Spinner spinner, int position) {
            // 获取所选择的任务类型
            MissionItemType selectedType = commandAdapter.getItem(position);
            try {
                if (mSelectedProxies.isEmpty())
                    return;

                List<Pair<MissionItemProxy, List<MissionItemProxy>>> updatesList = new ArrayList<>(mSelectedProxies.size());
                for (MissionItemProxy missionItemProxy : mSelectedProxies) {
                    MissionItemImpl oldItem = missionItemProxy.getMissionItem();
                    MissionItemType previousType = oldItem.getType();
                    if (previousType != selectedType) {
                        List<MissionItemProxy> newItems = new ArrayList<>();

                        // Survey类
                        Logger.i("类型：" + previousType.getName());

                        if (previousType == MissionItemType.SURVEY || previousType == MissionItemType.SPLINE_SURVEY) {
                            switch (selectedType) {
                                case SURVEY: {
                                    SurveyImpl newItem = (SurveyImpl) oldItem;
                                    newItems.add(new MissionItemProxy(mMissionProxy, newItem));
                                    break;
                                }

                                case SPLINE_SURVEY: {
//                                    SplineSurveyImpl newItem = (SplineSurveyImpl) oldItem;
//                                    newItems.add(new MissionItemProxy(mMissionProxy, newItem));
                                    SurveyImpl newItem = (SurveyImpl) oldItem;
                                    newItems.add(new MissionItemProxy(mMissionProxy, newItem));
                                    break;
                                }

                                default: {
                                    SurveyImpl previousSurvey = (SurveyImpl) oldItem;
                                    SurveyData surveyDetail = previousSurvey.getSurveyData();
                                    double altitude = surveyDetail == null ? mMissionProxy.getLastAltitude() : surveyDetail.getAltitude();

                                    List<LatLong> polygonPoints = previousSurvey.polygon.getPoints();
                                    for (LatLong coordinate : polygonPoints) {
                                        MissionItemImpl newItem = selectedType.getNewItem(oldItem);
                                        if (newItem instanceof SpatialCoordItem) {
                                            ((SpatialCoordItem) newItem).setCoordinate(new LatLongAlt(coordinate.getLatitude(),
                                                    coordinate.getLongitude(), altitude));
                                        }

                                        newItems.add(new MissionItemProxy(mMissionProxy, newItem));
                                    }
                                    break;
                                }
                            }
                        } else {
                            MissionItemImpl newItem = selectedType.getNewItem(oldItem);

                            if (oldItem instanceof SpatialCoordItem && newItem instanceof SpatialCoordItem) {
                                ((SpatialCoordItem) newItem).setCoordinate(((SpatialCoordItem) oldItem).getCoordinate());
                            }

                            newItems.add(new MissionItemProxy(mMissionProxy, newItem));
                        }

                        updatesList.add(Pair.create(missionItemProxy, newItems));
                    }
                }

                if (!updatesList.isEmpty()) {
                    mListener.onWaypointTypeChanged(selectedType, updatesList);
                    dismiss();
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    };

    public static MissionDetailFragment newInstance(MissionItemType itemType) {
        MissionDetailFragment fragment;
        switch (itemType) {
            case LAND:
                fragment = new MissionLandFragment();
                break;
            case CIRCLE:
                fragment = new MissionCircleFragment();
                break;
            case CHANGE_SPEED:
                fragment = new MissionChangeSpeedFragment();
                break;
            case ROI:
                fragment = new MissionRegionOfInterestFragment();
                break;
            case RTL:
                fragment = new MissionRTLFragment();
                break;
            case SPLINE_SURVEY:
                fragment = new MissionSurveyFragment();
                break;
            case SURVEY:
                fragment = new MissionSurveyFragment();
                break;
            case TAKEOFF:
                fragment = new MissionTakeoffFragment();
                break;
            case WAYPOINT:
                fragment = new MissionWaypointFragment();
                break;
            case SPLINE_WAYPOINT:
                fragment = new MissionSplineWaypointFragment();
                break;
            case CYLINDRICAL_SURVEY:
                fragment = new MissionStructureScannerFragment();
                break;
            case CAMERA_TRIGGER:
                fragment = new MissionCameraTriggerFragment();
                break;
            case EPM_GRIPPER:
                fragment = new MissionEpmGrabberFragment();
                break;
            case SET_SERVO:
                fragment = new SetServoFragment();
                break;
            case CONDITION_YAW:
                fragment = new MissionConditionYawFragment();
                break;
            case DO_JUMP:
                fragment = new MissionDoJumpFragment();
                break;
            case RESET_ROI:
                fragment = new MissionResetROIFragment();
                break;

            default:
                fragment = null;
                break;
        }
        return fragment;
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case HEARTBEAT_RESTORED:
            case STATE_DISCONNECTED:
            case STATE_CONNECTED:
            case HOME_UPDATED:
                updateHomeDistance();
                break;
        }
    }

    protected int getResource() {
        return R.layout.fragment_editor_detail_generic;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        DroidPlannerPrefs dpPrefs = getAppPrefs();
        MIN_ALTITUDE = dpPrefs.getMinAltitude();
        MAX_ALTITUDE = dpPrefs.getMaxAltitude();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMissionProxy = getMissionProxy();

        mSelectedProxies.clear();
        mSelectedProxies.addAll(mMissionProxy.selection.getSelected());

        mSelectedItems.clear();
        for (MissionItemProxy mip : mSelectedProxies) {
            mSelectedItems.add(mip.getMissionItem());
        }

        if (view == null) return;

        distanceView = (TextView) view.findViewById(R.id.DistanceValue);
        if (distanceView != null)
            distanceView.setVisibility(View.GONE);

        distanceLabelView = (TextView) view.findViewById(R.id.DistanceLabel);
        if (distanceLabelView != null)
            distanceLabelView.setVisibility(View.GONE);

        List<MissionItemType> list = new LinkedList<>(Arrays.asList(SUPPORTED_MISSION_ITEM_TYPES));

        if (mSelectedProxies.size() == 1) {
            MissionItemProxy itemProxy = mSelectedProxies.get(0);
            MissionItemImpl currentItem = itemProxy.getMissionItem();

            if (currentItem instanceof SurveyImpl) {
                list.clear();
                list.add(MissionItemType.SURVEY);
                list.add(MissionItemType.SPLINE_SURVEY);
            } else {
                list.remove(MissionItemType.SURVEY);
                list.remove(MissionItemType.SPLINE_SURVEY);
            }

            if ((currentItem instanceof StructureScannerImpl)) {
                list.clear();
                list.add(MissionItemType.CYLINDRICAL_SURVEY);
            }

            if (mMissionProxy.getItems().indexOf(itemProxy) != 0) {
                list.remove(MissionItemType.TAKEOFF);
            }

            if (mMissionProxy.getItems().indexOf(itemProxy) != (mMissionProxy.getItems().size() - 1)) {
                list.remove(MissionItemType.LAND);
                list.remove(MissionItemType.RTL);
            }

            if (currentItem instanceof MissionCMD) {
                list.remove(MissionItemType.LAND);
                list.remove(MissionItemType.SPLINE_WAYPOINT);
                list.remove(MissionItemType.CIRCLE);
                list.remove(MissionItemType.ROI);
                list.remove(MissionItemType.WAYPOINT);
                list.remove(MissionItemType.CYLINDRICAL_SURVEY);
            }

            TextView waypointIndex = (TextView) view.findViewById(R.id.WaypointIndex);
            if (waypointIndex != null) {
                int itemOrder = mMissionProxy.getOrder(itemProxy);
                waypointIndex.setText(String.valueOf(itemOrder));
            }

        } else if (mSelectedProxies.size() > 1) {
            //Remove the mission item types that don't apply to multiple items.
            list.removeAll(typeWithNoMultiEditSupport);

            if (hasCommandItems(mSelectedProxies)) {
                //Remove all the spatial and complex type choices.
                list.remove(MissionItemType.LAND);
                list.remove(MissionItemType.SPLINE_WAYPOINT);
                list.remove(MissionItemType.CIRCLE);
                list.remove(MissionItemType.ROI);
                list.remove(MissionItemType.WAYPOINT);
                list.remove(MissionItemType.CYLINDRICAL_SURVEY);
                list.remove(MissionItemType.SURVEY);
                list.remove(MissionItemType.SPLINE_SURVEY);
            }

            if (hasSpatialOrComplexItems(mSelectedProxies)) {
                //Remove all the command type choices.
                list.remove(MissionItemType.CONDITION_YAW);
                list.remove(MissionItemType.CHANGE_SPEED);
                list.remove(MissionItemType.TAKEOFF);
                list.remove(MissionItemType.SET_SERVO);
                list.remove(MissionItemType.RTL);
                list.remove(MissionItemType.EPM_GRIPPER);
                list.remove(MissionItemType.CAMERA_TRIGGER);
                list.remove(MissionItemType.DO_JUMP);
                list.remove(MissionItemType.RESET_ROI);
            }
        } else {
            //Invalid state. We should not have been able to get here.
            //If the parent activity is listening, it will remove this fragment when the selection is empty.
            mMissionProxy.selection.notifySelectionUpdate();

            //Dismiss this dialog fragment
            dismiss();
            return;
        }

        if (getResource() == R.layout.fragment_editor_detail_generic) {
            TextView spinnerTitle = (TextView) view.findViewById(R.id.WaypointType);
            TextView spinnerDescription = (TextView) view.findViewById(R.id.mission_item_type_selection_description);

            if (list.isEmpty()) {
                if (spinnerTitle != null)
                    spinnerTitle.setText(R.string.label_mission_item_type_no_selection);

                if (spinnerDescription != null)
                    spinnerDescription.setText(R.string.description_mission_item_type_no_selection);
            } else {
                if (spinnerTitle != null)
                    spinnerTitle.setText(R.string.label_mission_item_type_selection);

                if (spinnerDescription != null)
                    spinnerDescription.setText(R.string.description_mission_item_type_selection);
            }
        }

        commandAdapter = new AdapterMissionItems(getActivity(), android.R.layout.simple_list_item_1, list.toArray(new MissionItemType[list.size()]));

        typeSpinner = (SpinnerSelfSelect) view.findViewById(R.id.spinnerWaypointType);
        typeSpinner.setAdapter(commandAdapter);
        typeSpinner.setOnSpinnerItemSelectedListener(missionItemSpinnerListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHomeDistance();
    }

    private void updateHomeDistance() {
        if (distanceView == null && distanceLabelView == null)
            return;

        boolean hideDistanceInfo = true;

        Drone drone = getDrone();
        Home home = drone == null ? null : drone.<Home>getVehicleHome();

        if (home != null && home.isValid() && mSelectedProxies.size() == 1) {
            MissionItemProxy itemProxy = mSelectedProxies.get(0);
            MissionItemImpl item = itemProxy.getMissionItem();
            if (item instanceof SpatialCoordItem) {
                LatLongAlt itemCoordinate = ((SpatialCoordItem) item).getCoordinate();
                LatLongAlt homeCoordinate = home.getCoordinate();
                double homeDistance = MathUtils.getDistance3D(homeCoordinate, itemCoordinate);
                if (homeDistance > 0) {
                    hideDistanceInfo = false;

                    if (distanceView != null) {
                        distanceView.setText(getLengthUnitProvider().boxBaseValueToTarget(homeDistance).toString());
                        distanceView.setVisibility(View.VISIBLE);

                        if (distanceLabelView != null) {
                            distanceLabelView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }

        if (hideDistanceInfo) {
            if (distanceView != null)
                distanceView.setVisibility(View.GONE);

            if (distanceLabelView != null) {
                distanceLabelView.setVisibility(View.GONE);
            }
        }
    }

    private boolean hasCommandItems(List<MissionItemProxy> items) {
        for (MissionItemProxy item : items) {
            if (item.getMissionItem() instanceof MissionCMD)
                return true;
        }

        return false;
    }

    private boolean hasSpatialOrComplexItems(List<MissionItemProxy> items) {
        for (MissionItemProxy item : items) {
            MissionItemImpl missionItem = item.getMissionItem();
            if (missionItem instanceof SpatialCoordItem)
                return true;
        }

        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getResource(), container, false);
    }

    protected List<? extends MissionItemImpl> getMissionItems() {
        return mSelectedItems;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof OnMissionDetailListener)) {
            throw new IllegalStateException("Parent activity must be an instance of " + OnMissionDetailListener.class.getName());
        }

        mListener = (OnMissionDetailListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onDetailDialogDismissed(mSelectedProxies);
        }
    }

    public interface OnMissionDetailListener {
        /**
         * Only fired when the mission detail is shown as a dialog. Notifies the
         * listener that the mission detail dialog has been dismissed.
         *
         * @param itemList list of mission items proxies whose details the dialog is showing.
         */
        void onDetailDialogDismissed(List<MissionItemProxy> itemList);

        /**
         * Notifies the listener that the mission item proxy was changed.
         *
         * @param newType         the new selected mission item type
         * @param oldNewItemsList a list of pairs containing the previous,
         *                        and the new mission item proxy.
         */
        void onWaypointTypeChanged(MissionItemType newType, List<Pair<MissionItemProxy, List<MissionItemProxy>>> oldNewItemsList);
    }
}