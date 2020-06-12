package org.farring.gcs.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.farring.gcs.R;
import org.farring.gcs.fragments.setting.SettingMainFragment;

/**
 * This activity holds the SettingsFragment.
 */
public class SettingsActivity extends DrawerNavigationUI {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FragmentManager fm = getSupportFragmentManager();
        Fragment settingsFragment = fm.findFragmentById(R.id.fragment_settings_layout);
        if (settingsFragment == null) {
            settingsFragment = new SettingMainFragment();
            fm.beginTransaction().add(R.id.fragment_settings_layout, settingsFragment, "SettingMainFragment").commit();
        }
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }

    @Override
    protected int getNavigationDrawerMenuItemId() {
        return R.id.navigation_settings;
    }
}
