package org.farring.gcs.fragments.actionbar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Altitude;
import com.dronekit.core.drone.property.Battery;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.property.Home;
import com.dronekit.core.drone.property.Signal;
import com.dronekit.core.drone.property.Speed;
import com.dronekit.core.drone.property.Vibration;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.gcs.ReturnToMe;
import com.dronekit.utils.MathUtils;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;

import org.beyene.sius.unit.length.LengthUnit;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.dialogs.SelectionListDialog;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import java.util.Locale;

/**
 * Created by Fredia Huya-Kouadio on 1/14/15.
 */
public class ActionBarTelemFragment extends BaseFragment {

    private DroidPlannerPrefs appPrefs;
    private TextView homeTelem;
    private TextView altitudeTelem;
    private TextView gpsTelem;
    private PopupWindow gpsPopup;
    private TextView batteryTelem;
    private PopupWindow batteryPopup;
    private TextView signalTelem;
    private PopupWindow signalPopup;
    private TextView flightModeTelem;
    private String emptyString;
    private LinearLayout mLLBar;
    private TextView armStatus;

    private TextView groundVel;
    private TextView flightTime;
    private TextView watervel;

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        if (getActivity() == null)
            return;
        switch (attributeEvent) {
            case BATTERY_UPDATED:
                updateBatteryTelem();
                break;

            case STATE_CONNECTED:
                showTelemBar();
                updateAllTelem();
                break;

            case STATE_DISCONNECTED:
                hideTelemBar();
                updateAllTelem();
                break;

            case RETURN_TO_ME_STATE_UPDATE:
            case GPS_POSITION:
            case HOME_UPDATED:
                updateHomeTelem();
                break;

            case GPS_COUNT:
            case GPS_FIX:
                updateGpsTelem();
                break;

            case SIGNAL_UPDATED:
                updateSignalTelem();
                break;

            case STATE_VEHICLE_MODE:
            case TYPE_UPDATED:
                updateFlightModeTelem();
                break;

            case ALTITUDE_UPDATED:
                updateAltitudeTelem();
                break;
            case STATE_ARMING:
                updateArmStatus();
                break;
            case SPEED_UPDATED:
                updateVel();
            case STATE_VEHICLE_VIBRATION:
                updateWaterVel();
            default:
                break;
        }
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        super.onReceiveActionEvent(actionEvent);
        switch (actionEvent) {
            case ACTION_PREF_RETURN_TO_ME_UPDATED:
                updateHomeTelem();
                break;

            case ACTION_PREF_HDOP_UPDATE:
                updateGpsTelem();
                break;

            case ACTION_PREF_UNIT_SYSTEM_UPDATE:
                updateHomeTelem();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_action_bar_telem, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyString = getString(R.string.empty_content);

        final Context context = getActivity().getApplicationContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        final int popupWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int popupHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
        final Drawable popupBg = getResources().getDrawable(android.R.color.transparent);

        homeTelem = (TextView) view.findViewById(R.id.bar_home);
        homeTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch dialog to allow the user to select between rtl and rtm
                final SelectionListDialog selectionDialog = SelectionListDialog.newInstance(
                        new ReturnToHomeAdapter(context, dpApp.getDroneManager(), appPrefs));
                Utils.showDialog(selectionDialog, getChildFragmentManager(), "Return to home type", true);
            }
        });

        altitudeTelem = (TextView) view.findViewById(R.id.bar_altitude);
        armStatus = (TextView) view.findViewById(R.id.bar_arm_mode);

        groundVel = (TextView) view.findViewById(R.id.bar_vel);
        flightTime = (TextView) view.findViewById(R.id.bar_flight_time);
        watervel = (TextView) view.findViewById(R.id.bar_water_vel);

        gpsTelem = (TextView) view.findViewById(R.id.bar_gps);
        final View gpsPopupView = inflater.inflate(R.layout.popup_info_gps, (ViewGroup) view, false);
        gpsPopup = new PopupWindow(gpsPopupView, popupWidth, popupHeight, true);
        gpsPopup.setBackgroundDrawable(popupBg);
        gpsTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsPopup.showAsDropDown(gpsTelem);
            }
        });

        batteryTelem = (TextView) view.findViewById(R.id.bar_battery);
        final View batteryPopupView = inflater.inflate(R.layout.popup_info_power, (ViewGroup) view, false);
        batteryPopup = new PopupWindow(batteryPopupView, popupWidth, popupHeight, true);
        batteryPopup.setBackgroundDrawable(popupBg);
        batteryTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // batteryPopup.showAsDropDown(batteryTelem);
            }
        });

        signalTelem = (TextView) view.findViewById(R.id.bar_signal);
        final View signalPopupView = inflater.inflate(R.layout.popup_info_signal, (ViewGroup) view, false);
        signalPopup = new PopupWindow(signalPopupView, popupWidth, popupHeight, true);
        signalPopup.setBackgroundDrawable(popupBg);
        signalTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // signalPopup.showAsDropDown(signalTelem);
            }
        });

        flightModeTelem = (TextView) view.findViewById(R.id.bar_flight_mode);
        flightModeTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch dialog to allow the user to select vehicle modes
                final SelectionListDialog selectionDialog = SelectionListDialog.newInstance(new FlightModeAdapter(context, getDrone()));
                Utils.showDialog(selectionDialog, getChildFragmentManager(), "Flight modes selection", true);
            }
        });

        appPrefs = DroidPlannerPrefs.getInstance(context);
        mLLBar = (LinearLayout) view.findViewById(R.id.bar_ll);

    }

    private void showTelemBar() {
        final View view = getView();
        if (view != null)
            mLLBar.setVisibility(View.VISIBLE);
//            view.setVisibility(View.VISIBLE);
    }

    private void hideTelemBar() {
        final View view = getView();
        if (view != null)
            mLLBar.setVisibility(View.INVISIBLE);
//            view.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        hideTelemBar();
        super.onStart();

        final Drone drone = getDrone();
        if (drone.isConnected())
            showTelemBar();
        else
            hideTelemBar();

        updateAllTelem();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void updateAllTelem() {
        updateFlightModeTelem();
        updateSignalTelem();
        updateGpsTelem();
        updateHomeTelem();
        updateBatteryTelem();
        updateAltitudeTelem();
        updateArmStatus();
        updateVel();
        updateWaterVel();
    }

    private void updateFlightModeTelem() {
        final Drone drone = getDrone();

        final boolean isDroneConnected = drone.isConnected();
        final State droneState = drone.getState();
        if (isDroneConnected) {
            flightModeTelem.setText(droneState.getMode().getLabel());
            // flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_18dp, 0, 0, 0);
        } else {
            flightModeTelem.setText(emptyString);
            // flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_grey_700_18dp, 0, 0, 0);
        }
    }

    private void updateSignalTelem() {
        final Drone drone = getDrone();

        final View popupView = signalPopup.getContentView();
        TextView rssiView = (TextView) popupView.findViewById(R.id.bar_signal_rssi);
        TextView remRssiView = (TextView) popupView.findViewById(R.id.bar_signal_remrssi);
        TextView noiseView = (TextView) popupView.findViewById(R.id.bar_signal_noise);
        TextView remNoiseView = (TextView) popupView.findViewById(R.id.bar_signal_remnoise);
        TextView fadeView = (TextView) popupView.findViewById(R.id.bar_signal_fade);
        TextView remFadeView = (TextView) popupView.findViewById(R.id.bar_signal_remfade);

        final Signal droneSignal = drone.getSignal();
        if (!drone.isConnected() || !droneSignal.isValid()) {
            signalTelem.setText(emptyString);
            // signalTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_signal_cellular_null_grey_700_18dp, 0, 0, 0);

            rssiView.setText("RSSI: " + emptyString);
            remRssiView.setText("RemRSSI: " + emptyString);
            noiseView.setText("Noise: " + emptyString);
            remNoiseView.setText("RemNoise: " + emptyString);
            fadeView.setText("Fade: " + emptyString);
            remFadeView.setText("RemFade: " + emptyString);
        } else {
            final int signalStrength = (int) droneSignal.getSignalStrength();
            final int signalIcon;
            if (signalStrength >= 100)
                signalIcon = R.drawable.ic_signal_cellular_4_bar_grey_700_18dp;
            else if (signalStrength >= 75)
                signalIcon = R.drawable.ic_signal_cellular_3_bar_grey_700_18dp;
            else if (signalStrength >= 50)
                signalIcon = R.drawable.ic_signal_cellular_2_bar_grey_700_18dp;
            else if (signalStrength >= 25)
                signalIcon = R.drawable.ic_signal_cellular_1_bar_grey_700_18dp;
            else
                signalIcon = R.drawable.ic_signal_cellular_0_bar_grey_700_18dp;

            signalTelem.setText("信号:" + String.format(Locale.ENGLISH, "%d%%", signalStrength));
            // signalTelem.setCompoundDrawablesWithIntrinsicBounds(signalIcon, 0, 0, 0);

            rssiView.setText(String.format("RSSI: %2.0f dB", droneSignal.getRssi()));
            remRssiView.setText(String.format("RemRSSI: %2.0f dB", droneSignal.getRemrssi()));
            noiseView.setText(String.format("Noise: %2.0f dB", droneSignal.getNoise()));
            remNoiseView.setText(String.format("RemNoise: %2.0f dB", droneSignal.getRemnoise()));
            fadeView.setText(String.format("Fade: %2.0f dB", droneSignal.getFadeMargin()));
            remFadeView.setText(String.format("RemFade: %2.0f dB", droneSignal.getRemFadeMargin()));
        }

        signalPopup.update();
    }

    private void updateGpsTelem() {
        final Drone drone = getDrone();
        final boolean displayHdop = appPrefs.shouldGpsHdopBeDisplayed();

        final View popupView = gpsPopup.getContentView();
        TextView satNoView = (TextView) popupView.findViewById(R.id.bar_gps_satno);
        TextView hdopStatusView = (TextView) popupView.findViewById(R.id.bar_gps_hdop_status);
        hdopStatusView.setVisibility(displayHdop ? View.GONE : View.VISIBLE);

        final String update;
        final int gpsIcon;
        if (!drone.isConnected()) {
            update = (displayHdop ? "HDOP: " : "") + emptyString;
            gpsIcon = R.drawable.ic_gps_off_grey_700_18dp;
            satNoView.setText("卫星数: " + emptyString);
            hdopStatusView.setText("HDOP: " + emptyString);
        } else {
            Gps droneGps = drone.getVehicleGps();
            final String fixStatus = droneGps.getFixStatus();

           /* if (displayHdop) {
                update = String.format(Locale.ENGLISH, "HDOP: %.1f", droneGps.getGpsEph());
            } else {
                update = String.format(Locale.ENGLISH, "%s", fixStatus);
            }*/

            switch (fixStatus) {
                case Gps.LOCK_3D:
                case Gps.LOCK_3D_DGPS:
                case Gps.LOCK_3D_RTK:
                    gpsIcon = R.drawable.ic_gps_fixed_black_24dp;
                    break;

                case Gps.LOCK_2D:
                case Gps.NO_FIX:
                default:
                    gpsIcon = R.drawable.ic_gps_not_fixed_grey_700_18dp;
                    break;
            }

            satNoView.setText(String.format(Locale.ENGLISH, "卫星数: %d", droneGps.getSatellitesCount()));
            if (appPrefs.shouldGpsHdopBeDisplayed()) {
                hdopStatusView.setText(String.format(Locale.ENGLISH, "%s", fixStatus));
            } else {
                hdopStatusView.setText(String.format(Locale.ENGLISH, "Hdop: %.1f", droneGps.getGpsEph()));
            }

            update = String.format(Locale.ENGLISH, "星:%d/%.1f", droneGps.getSatellitesCount(), droneGps.getGpsEph());

        }

        //

        gpsTelem.setText(update);
        // gpsTelem.setCompoundDrawablesWithIntrinsicBounds(gpsIcon, 0, 0, 0);
        gpsPopup.update();
    }

    private void updateHomeTelem() {
        final Drone drone = getDrone();

        String update = getString(R.string.empty_content);
        int drawableResId = appPrefs.isReturnToMeEnabled()
                ? R.drawable.ic_person_grey_700_18dp
                : R.drawable.ic_home_grey_700_18dp;

        if (drone.isConnected()) {
            final Gps droneGps = drone.getVehicleGps();
            final Home droneHome = drone.getVehicleHome();
            if (droneGps.isValid() && droneHome.isValid()) {
                LengthUnit distanceToHome = getLengthUnitProvider().boxBaseValueToTarget(MathUtils.getDistance2D(droneHome.getCoordinate(), droneGps.getPosition()));
                update = String.format("%s", distanceToHome);

                final ReturnToMe returnToMe = dpApp.getDroneManager().getReturnToMe();
                switch (returnToMe.getState()) {

                    case ReturnToMe.STATE_UPDATING_HOME:
                        // Change the home telemetry icon
                        drawableResId = R.drawable.ic_person_blue_a400_18dp;
                        break;

                    case ReturnToMe.STATE_USER_LOCATION_INACCURATE:
                    case ReturnToMe.STATE_USER_LOCATION_UNAVAILABLE:
                    case ReturnToMe.STATE_WAITING_FOR_VEHICLE_GPS:
                    case ReturnToMe.STATE_ERROR_UPDATING_HOME:
                        drawableResId = R.drawable.ic_person_red_500_18dp;
                        update = getString(R.string.empty_content);
                        break;

                    case ReturnToMe.STATE_IDLE:
                        break;
                }
            }
        }

        // homeTelem.setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0);
        homeTelem.setText("家:"+ update.replace("M", "米"));
    }

    private void updateBatteryTelem() {
        final Drone drone = getDrone();

        final View batteryPopupView = batteryPopup.getContentView();
        final TextView currentView = (TextView) batteryPopupView.findViewById(R.id.bar_power_current);

        String update;
        Battery droneBattery;
        final int batteryIcon;
        if (!drone.isConnected() || ((droneBattery = drone.getBattery()) == null)) {
            update = emptyString;
            currentView.setText("电流: " + emptyString);
            batteryIcon = R.drawable.ic_battery_circle_0_24dp;
        } else {
            final double battRemain = droneBattery.getBatteryRemain();
            currentView.setText(String.format("电流: %2.1f A", droneBattery.getBatteryCurrent()));

            update = String.format(Locale.ENGLISH, "%2.1fV", droneBattery.getBatteryVoltage());

            if (battRemain >= 100) {
                batteryIcon = R.drawable.ic_battery_circle_8_24dp;
            } else if (battRemain >= 87.5) {
                batteryIcon = R.drawable.ic_battery_circle_7_24dp;
            } else if (battRemain >= 75) {
                batteryIcon = R.drawable.ic_battery_circle_6_24dp;
            } else if (battRemain >= 62.5) {
                batteryIcon = R.drawable.ic_battery_circle_5_24dp;
            } else if (battRemain >= 50) {
                batteryIcon = R.drawable.ic_battery_circle_4_24dp;
            } else if (battRemain >= 37.5) {
                batteryIcon = R.drawable.ic_battery_circle_3_24dp;
            } else if (battRemain >= 25) {
                batteryIcon = R.drawable.ic_battery_circle_2_24dp;
            } else if (battRemain >= 12.5) {
                batteryIcon = R.drawable.ic_battery_circle_1_24dp;
            } else {
                batteryIcon = R.drawable.ic_battery_circle_0_24dp;
            }
        }

        batteryPopup.update();
        batteryTelem.setText(update);
        // batteryTelem.setCompoundDrawablesWithIntrinsicBounds(batteryIcon, 0, 0, 0);
    }

    private void updateAltitudeTelem() {
        final Drone drone = getDrone();
        final Altitude altitude = drone.getAltitude();
        if (altitude != null) {
            double alt = altitude.getAltitude();
            LengthUnit altUnit = getLengthUnitProvider().boxBaseValueToTarget(alt);

            this.altitudeTelem.setText("高:"+ altUnit.toString().replace("M", "米"));
        }
    }

    private void updateVel() {
        final Drone drone = getDrone();
        final Speed velocity = drone.getSpeed();
        if (velocity != null) {
            double velVal = velocity.getGroundSpeed();
            LengthUnit velUnit = getLengthUnitProvider().boxBaseValueToTarget(velVal);

            String txtMsg = "速度:"+ velUnit.toString().replace("M", "米");
            this.groundVel.setText(txtMsg);
            // Toast.makeText(getActivity(), txtMsg, Toast.LENGTH_SHORT).show();
        }
    }
    private void updateWaterVel() {
        final Drone drone = getDrone();
        final Vibration userData = drone.getVibration();
        if (userData != null) {
            long flightime = (long)userData.getVibrationY();
            long minutes =  flightime / 60;
            long seconds = flightime % 60;
            String txtMsg = "时:"+ String.format("%02d:%02d", minutes, seconds);
            // Toast.makeText(getActivity(), txtMsg, Toast.LENGTH_SHORT).show();
            this.flightTime.setText(txtMsg);

            double waterVel = userData.getVibrationX();
            txtMsg = "药:"+ String.format("%.1f", waterVel) + ((((int)userData.getVibrationZ()) == 1)?"/有":"/无");
            this.watervel.setText(txtMsg);
            // Toast.makeText(getActivity(), txtMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateArmStatus() {
        final Drone drone = getDrone();
        final boolean armStatus = drone.getState().isArmed();
        if (armStatus) {
            this.armStatus.setText("解锁");
            this.armStatus.setTextColor(Color.GREEN);
        }
        else
        {
            this.armStatus.setText("锁定");
            this.armStatus.setTextColor(Color.RED);
        }
    }
}
