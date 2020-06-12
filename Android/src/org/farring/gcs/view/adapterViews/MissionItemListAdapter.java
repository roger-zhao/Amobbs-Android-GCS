package org.farring.gcs.view.adapterViews;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dronekit.core.mission.MissionItemImpl;
import com.dronekit.core.mission.commands.CameraTriggerImpl;
import com.dronekit.core.mission.commands.ChangeSpeedImpl;
import com.dronekit.core.mission.commands.ConditionYawImpl;
import com.dronekit.core.mission.commands.EpmGripperImpl;
import com.dronekit.core.mission.commands.MissionCMD;
import com.dronekit.core.mission.commands.ReturnToHomeImpl;
import com.dronekit.core.mission.commands.SetServoImpl;
import com.dronekit.core.mission.commands.TakeoffImpl;
import com.dronekit.core.mission.survey.SplineSurveyImpl;
import com.dronekit.core.mission.survey.SurveyImpl;
import com.dronekit.core.mission.waypoints.CircleImpl;
import com.dronekit.core.mission.waypoints.LandImpl;
import com.dronekit.core.mission.waypoints.RegionOfInterestImpl;
import com.dronekit.core.mission.waypoints.SpatialCoordItem;
import com.dronekit.core.mission.waypoints.SplineWaypointImpl;

import org.beyene.sius.unit.composition.speed.SpeedUnit;
import org.beyene.sius.unit.length.LengthUnit;
import org.farring.gcs.R;
import org.farring.gcs.activities.interfaces.OnEditorInteraction;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;
import org.farring.gcs.utils.ReorderRecyclerView;
import org.farring.gcs.utils.unit.UnitManager;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.utils.unit.providers.speed.SpeedUnitProvider;
import org.farring.gcs.utils.unit.systems.UnitSystem;

/**
 * Created by fhuya on 12/9/14.
 */
public class MissionItemListAdapter extends ReorderRecyclerView.ReorderAdapter<MissionItemListAdapter.ViewHolder> {

    private final MissionProxy missionProxy;
    private final OnEditorInteraction editorListener;
    private final LengthUnitProvider lengthUnitProvider;
    private final SpeedUnitProvider speedUnitProvider;

    public MissionItemListAdapter(Context context, MissionProxy missionProxy, OnEditorInteraction editorListener) {
        this.missionProxy = missionProxy;
        this.editorListener = editorListener;

        final UnitSystem unitSystem = UnitManager.getUnitSystem(context);
        this.lengthUnitProvider = unitSystem.getLengthUnitProvider();
        this.speedUnitProvider = unitSystem.getSpeedUnitProvider();
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return missionProxy.getItems().get(position).getStableId();
    }

    @Override
    public int getItemCount() {
        return missionProxy.getItems().size();
    }

    @Override
    public void swapElements(int fromIndex, int toIndex) {
        if (isIndexValid(fromIndex) && isIndexValid(toIndex)) {
            missionProxy.swap(fromIndex, toIndex);
        }
    }

    private boolean isIndexValid(int index) {
        return index >= 0 && index < getItemCount();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_editor_list_item, parent, false);

        final TextView nameView = (TextView) view.findViewById(R.id.rowNameView);
        final TextView altitudeView = (TextView) view.findViewById(R.id.rowAltitudeView);

        return new ViewHolder(view, nameView, altitudeView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        final MissionItemProxy proxy = missionProxy.getItems().get(position);

        final View container = viewHolder.viewContainer;
        container.setActivated(missionProxy.selection.selectionContains(proxy));
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editorListener != null)
                    editorListener.onItemClick(proxy, true);
            }
        });

        final TextView nameView = viewHolder.nameView;
        final TextView altitudeView = viewHolder.altitudeView;

        final MissionProxy missionProxy = proxy.getMission();
        final MissionItemImpl missionItem = proxy.getMissionItem();

        nameView.setText(String.format("%3d", missionProxy.getOrder(proxy)));

        int leftDrawable;

        // Spatial item's icons
        if (missionItem instanceof SpatialCoordItem) {
            if (missionItem instanceof SplineWaypointImpl) {
                leftDrawable = R.drawable.ic_mission_spline_wp;
            } else if (missionItem instanceof CircleImpl) {
                leftDrawable = R.drawable.ic_mission_circle_wp;
            } else if (missionItem instanceof RegionOfInterestImpl) {
                leftDrawable = R.drawable.ic_mission_roi_wp;
            } else if (missionItem instanceof LandImpl) {
                leftDrawable = R.drawable.ic_mission_land_wp;
            } else {
                leftDrawable = R.drawable.ic_mission_wp;
            }
            // Command icons
        } else if (missionItem instanceof MissionCMD) {
            if (missionItem instanceof CameraTriggerImpl) {
                leftDrawable = R.drawable.ic_mission_camera_trigger_wp;
            } else if (missionItem instanceof ChangeSpeedImpl) {
                leftDrawable = R.drawable.ic_mission_change_speed_wp;
            } else if (missionItem instanceof EpmGripperImpl) {
                leftDrawable = R.drawable.ic_mission_epm_gripper_wp;
            } else if (missionItem instanceof ReturnToHomeImpl) {
                leftDrawable = R.drawable.ic_mission_rtl_wp;
            } else if (missionItem instanceof SetServoImpl) {
                leftDrawable = R.drawable.ic_mission_set_servo_wp;
            } else if (missionItem instanceof TakeoffImpl) {
                leftDrawable = R.drawable.ic_mission_takeoff_wp;
            } else if (missionItem instanceof ConditionYawImpl) {
                leftDrawable = R.drawable.ic_mission_yaw_cond_wp;
            } else {
                leftDrawable = R.drawable.ic_mission_command_wp;
            }
            // Complex item's icons
        } else if (missionItem instanceof SplineSurveyImpl) {
            leftDrawable = R.drawable.ic_mission_spline_survey_wp;
        } else if (missionItem instanceof SurveyImpl) {
            leftDrawable = R.drawable.ic_mission_survey_wp;

            // Fallback icon
        } else {
            leftDrawable = R.drawable.ic_mission_wp;
        }

        altitudeView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, 0, 0, 0);

        if (missionItem instanceof SpatialCoordItem) {
            SpatialCoordItem waypoint = (SpatialCoordItem) missionItem;
            double altitude = waypoint.getCoordinate().getAltitude();
            LengthUnit convertedAltitude = lengthUnitProvider.boxBaseValueToTarget(altitude);
            LengthUnit roundedConvertedAltitude = (LengthUnit) convertedAltitude.valueOf(Math.round(convertedAltitude.getValue()));
            altitudeView.setText(roundedConvertedAltitude.toString());

            if (altitude < 0)
                altitudeView.setTextColor(Color.YELLOW);
            else
                altitudeView.setTextColor(Color.WHITE);

        } else if (missionItem instanceof SurveyImpl) {
            double altitude = ((SurveyImpl) missionItem).getSurveyData().getAltitude();
            LengthUnit convertedAltitude = lengthUnitProvider.boxBaseValueToTarget(altitude);
            LengthUnit roundedConvertedAltitude = (LengthUnit) convertedAltitude.valueOf(Math.round(convertedAltitude.getValue()));
            altitudeView.setText(roundedConvertedAltitude.toString());

            if (altitude < 0)
                altitudeView.setTextColor(Color.YELLOW);
            else
                altitudeView.setTextColor(Color.WHITE);

        } else if (missionItem instanceof TakeoffImpl) {
            double altitude = ((TakeoffImpl) missionItem).getFinishedAlt();
            LengthUnit convertedAltitude = lengthUnitProvider.boxBaseValueToTarget(altitude);
            LengthUnit roundedConvertedAltitude = (LengthUnit) convertedAltitude.valueOf(Math.round(convertedAltitude.getValue()));
            altitudeView.setText(roundedConvertedAltitude.toString());

            if (altitude < 0)
                altitudeView.setTextColor(Color.YELLOW);
            else
                altitudeView.setTextColor(Color.WHITE);
        } else if (missionItem instanceof ChangeSpeedImpl) {
            final double speed = ((ChangeSpeedImpl) missionItem).getSpeed();
            final SpeedUnit convertedSpeed = speedUnitProvider.boxBaseValueToTarget(speed);
            altitudeView.setText(convertedSpeed.toString());
        } else {
            altitudeView.setText("");
        }
    }

    // Provide a reference to the views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {

        final View viewContainer;
        final TextView nameView;
        final TextView altitudeView;

        public ViewHolder(View container, TextView nameView, TextView altitudeView) {
            super(container);
            this.viewContainer = container;
            this.nameView = nameView;
            this.altitudeView = altitudeView;
        }

    }
}
