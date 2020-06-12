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
import org.farring.gcs.fragments.calibration.FragmentSetupIMU;
import org.farring.gcs.fragments.calibration.FragmentSetupMAG;
import org.farring.gcs.fragments.calibration.FragmentSetupRCCalibration;
import org.farring.gcs.fragments.calibration.FragmentSetupMotorTest;
import org.farring.gcs.fragments.calibration.FragmentSetupESC;
import org.farring.gcs.fragments.calibration.FragmentSetupFrame;


/**
 * Used to calibrate the drone's compass and accelerometer.
 */
public class SensorSetupFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensor_setup, container, false);
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
                    return new FragmentSetupMAG();
                case 2:
                    return new FragmentSetupRCCalibration();
               case 3:
                   return new FragmentSetupMotorTest();
                case 4:
                    return new FragmentSetupESC();
                case 5:
                    return new FragmentSetupFrame();
                default:
                case 0:
                    return new FragmentSetupIMU();
            }
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return "指南针校准";
                case 2:
                    return "遥控器校准";
                case 3:
                    return "电机测试";
                case 4:
                    return "电调校准";
                case 5:
                    return "机型配置";
                default:
                case 0:
                    return "IMU校准";

            }
        }
    }
}
