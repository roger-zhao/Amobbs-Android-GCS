package org.farring.gcs.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import org.farring.gcs.R;
import org.farring.gcs.fragments.setting.SettingConnectionFragment;

/**
 * This activity holds the SettingsFragment.
 */
public class SettingsConnextActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FragmentManager fm = getSupportFragmentManager();
        Fragment settingsFragment = fm.findFragmentById(R.id.fragment_settings_layout);
        if (settingsFragment == null) {
            settingsFragment = new SettingConnectionFragment();
            fm.beginTransaction().add(R.id.fragment_settings_layout, settingsFragment, "SettingMainFragment").commit();
        }
    }


}
