package org.farring.gcs.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.widget.Toast;

import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.fragments.FlightDataFragment;
import org.farring.gcs.fragments.WidgetsListFragment;
import org.farring.gcs.fragments.actionbar.ActionBarTelemFragment;

public class FlightActivity extends SuperUI  {

    private FlightDataFragment flightData;
    private long exitTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);

        final FragmentManager fm = getSupportFragmentManager();
        // Add the flight data fragment
        flightData = (FlightDataFragment) fm.findFragmentById(R.id.flight_data_container);
        if (flightData == null) {
            Bundle args = new Bundle();
            args.putBoolean(FlightDataFragment.EXTRA_SHOW_ACTION_DRAWER_TOGGLE, true);

            flightData = new FlightDataFragment();
            flightData.setArguments(args);
            fm.beginTransaction().add(R.id.flight_data_container, flightData).commit();
        }

        // Add the telemetry fragment
        WidgetsListFragment widgetsListFragment = (WidgetsListFragment) fm.findFragmentById(R.id.flight_widgets_container);
        if (widgetsListFragment == null) {
            widgetsListFragment = new WidgetsListFragment();
            fm.beginTransaction()
                    .add(R.id.flight_widgets_container, widgetsListFragment)
                    .commit();
        }
    }


    @Override
    protected void addToolbarFragment() {
        final int toolbarId = getToolbarId();
        final FragmentManager fm = getSupportFragmentManager();
        Fragment actionBarTelem = fm.findFragmentById(toolbarId);
        if (actionBarTelem == null) {
            actionBarTelem = new ActionBarTelemFragment();
            fm.beginTransaction().add(toolbarId, actionBarTelem).commit();
        }
    }


    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }


    @Override
    protected boolean enableMissionMenus() {
        return true;
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出该程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
