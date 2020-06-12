package org.farring.gcs.fragments.actionbar;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dronekit.core.MAVLink.MavLinkWaypoint;
import com.dronekit.core.MAVLink.command.doCmd.MavLinkDoCmds;
import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.APMConstants;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.gcs.ReturnToMe;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.orhanobut.logger.Logger;

import org.farring.gcs.R;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

/**
 * Created by Fredia Huya-Kouadio on 9/25/15.
 */
class ReturnToHomeAdapter extends SelectionListAdapter<Integer> {

    @StringRes
    private final int[] rthLabels = {
            R.string.label_rtl,
            R.string.label_rtm
    };

    @DrawableRes
    private final int[] rthIcons = {
            R.drawable.ic_home_grey_700_18dp,
            R.drawable.ic_person_grey_700_18dp
    };

    private final Context context;
    private final Drone drone;
    private final DroidPlannerPrefs dpPrefs;
    private final DroneManager droneManager;
    private int selectedLabel = 0;

    public ReturnToHomeAdapter(Context context, DroneManager droneManager, DroidPlannerPrefs dpPrefs) {
        super(context);
        this.context = context;
        this.droneManager = droneManager;
        this.drone = droneManager.getDrone();
        this.dpPrefs = dpPrefs;
        selectedLabel = dpPrefs.isReturnToMeEnabled() ? 1 : 0;
    }

    @Override
    public int getCount() {
        return rthLabels.length;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selection, parent, false);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();
        if (holder == null) {
            holder = new ViewHolder((TextView) convertView.findViewById(R.id.item_selectable_option),
                    (RadioButton) convertView.findViewById(R.id.item_selectable_check));
        }

        final OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedLabel = position;

                final boolean isReturnToMeEnabled = position == 1;
                // 设置参数到参数文件中
                dpPrefs.enableReturnToMe(isReturnToMeEnabled);

                final ReturnToMe returnToMe = droneManager.getReturnToMe();
                if (isReturnToMeEnabled) {
                    // Start return to me【使能】
                    returnToMe.enable(new ICommandListener() {
                        @Override
                        public void onSuccess() {
                            Logger.i("Started return to me.");
                            Toast.makeText(context, "Return to me started", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int i) {
                            Logger.e("Unable to start return to me.");
                        }

                        @Override
                        public void onTimeout() {
                            Logger.w("Starting return to me timed out.");
                        }
                    });
                } else {
                    // Stop return to me.【失能】
                    returnToMe.disable();
                    // 获取原来Home的坐标
                    final LatLongAlt originalHome = returnToMe.getOriginalHomeLocation();

                    // Set home position back to its original
                    if (originalHome != null) {
                        MavLinkDoCmds.setVehicleHome(drone, originalHome, new ICommandListener() {
                            @Override
                            public void onSuccess() {
                                Logger.i("Restored original home location.");
                                Toast.makeText(context, "Restored original home location", Toast.LENGTH_SHORT).show();
                                MavLinkWaypoint.requestWayPoint(drone, APMConstants.HOME_WAYPOINT_INDEX);
                            }

                            @Override
                            public void onError(int executionError) {
                                Logger.e("Unable to restore original home location.");
                                MavLinkWaypoint.requestWayPoint(drone, APMConstants.HOME_WAYPOINT_INDEX);
                            }

                            @Override
                            public void onTimeout() {
                                Logger.w("Timed out while attempting to restore the home location.");
                                MavLinkWaypoint.requestWayPoint(drone, APMConstants.HOME_WAYPOINT_INDEX);
                            }
                        });
                    }
                }

                if (listener != null)
                    listener.onSelection();
            }
        };

        holder.rthCheck.setChecked(position == selectedLabel);
        holder.rthCheck.setOnClickListener(clickListener);

        holder.rthOption.setText(rthLabels[position]);
        holder.rthOption.setOnClickListener(clickListener);
        holder.rthOption.setCompoundDrawablesWithIntrinsicBounds(rthIcons[position], 0, 0, 0);

        convertView.setOnClickListener(clickListener);
        convertView.setTag(holder);
        return convertView;
    }

    @Override
    public int getSelection() {
        return selectedLabel;
    }

    public static class ViewHolder {
        final TextView rthOption;
        final RadioButton rthCheck;

        public ViewHolder(TextView rthView, RadioButton check) {
            this.rthOption = rthView;
            this.rthCheck = check;
        }
    }
}
