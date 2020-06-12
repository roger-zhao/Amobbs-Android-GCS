package org.farring.gcs.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import org.farring.gcs.R;
import org.farring.gcs.fragments.userSettings.FragmentUserSettingFly;
import org.farring.gcs.fragments.userSettings.FragmentUserSettingApp;
import org.farring.gcs.fragments.userSettings.FragmentUserSettingSafe;

/**
 * Used to calibrate the drone's compass and accelerometer.
 */
public class AppSetupFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_setup, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 数据适配器
        final ViewPager viewPager = (ViewPager) view.findViewById(R.id.configuration_pager);
        viewPager.setAdapter(new SensorPagerAdapter(getChildFragmentManager()));

        // Bind the tabs to the ViewPager
        final PagerSlidingTabStrip tabIndicator = (PagerSlidingTabStrip) view.findViewById(R.id.configuration_tab_strip);
        tabIndicator.setViewPager(viewPager);
    }

    private static class SensorPagerAdapter extends FragmentPagerAdapter {

        SensorPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 1:
                    return new FragmentUserSettingApp();

                case 2:
                    return new FragmentUserSettingSafe();

                default:
                case 0:
                    return new FragmentUserSettingFly();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return "作业设置";

                case 2:
                    return "安全设置";
                default:
                case 0:
                    return "巡航设置";
            }
        }
    }
}
