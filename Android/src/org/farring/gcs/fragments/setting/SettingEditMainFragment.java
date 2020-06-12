package org.farring.gcs.fragments.setting;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.farring.gcs.R;
import org.farring.gcs.fragments.AppSetupFragment;
import org.farring.gcs.fragments.SensorSetupFragment;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import butterknife.ButterKnife;

public class SettingEditMainFragment extends BaseFragment implements View.OnClickListener {

    private DroidPlannerPrefs dpPrefs;
    private Activity mActivity;
    private View button1;
    private View button2;
    private View button3;
    private FrameLayout fl1;
    private FrameLayout fl2;
    private FrameLayout fl3;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dpPrefs = DroidPlannerPrefs.getInstance(mActivity.getApplicationContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main_setting, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        button1 =  view.findViewById(R.id.btn1);
        button2 =  view.findViewById(R.id.btn2);
        button3 =  view.findViewById(R.id.btn3);
        fl1 = (FrameLayout)view.findViewById(R.id.settings_fl1) ;
        fl2 = (FrameLayout)view.findViewById(R.id.settings_fl2) ;
        fl3 = (FrameLayout)view.findViewById(R.id.settings_fl3) ;
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getActivity().getSupportFragmentManager();

        Fragment sensorSetupFragment = fm.findFragmentById(R.id.settings_fl1);
        if (sensorSetupFragment == null) {
            sensorSetupFragment = new SensorSetupFragment();
            fm.beginTransaction().add(R.id.settings_fl1, sensorSetupFragment, "sensorSetupFragment").commit();
        }

        Fragment appSetupFragment = fm.findFragmentById(R.id.settings_fl2);
        if (appSetupFragment == null) {
            appSetupFragment = new AppSetupFragment();
            fm.beginTransaction().add(R.id.settings_fl2, appSetupFragment, "appSetupFragment").commit();
        }

        Fragment settingsFragment = fm.findFragmentById(R.id.settings_fl3);
        if (settingsFragment == null) {
            settingsFragment = new SettingMainFragment();
            fm.beginTransaction().add(R.id.settings_fl3, settingsFragment, "SettingMainFragment").commit();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1:
                showStatus();
                fl1.setVisibility(View.VISIBLE);
                button1.setBackgroundColor(getResources().getColor(R.color.green));
                break;

            case R.id.btn2:
                showStatus();
                fl2.setVisibility(View.VISIBLE);
                button2.setBackgroundColor(getResources().getColor(R.color.green));
                break;

            case R.id.btn3:
                showStatus();
                fl3.setVisibility(View.VISIBLE);
                button3.setBackgroundColor(getResources().getColor(R.color.green));
                break;

        }
    }

    private void showStatus(){
        fl1.setVisibility(View.GONE);
        fl2.setVisibility(View.GONE);
        fl3.setVisibility(View.GONE);
        button1.setBackgroundColor(getResources().getColor(R.color.bottom_top_bar));
        button2.setBackgroundColor(getResources().getColor(R.color.bottom_top_bar));
        button3.setBackgroundColor(getResources().getColor(R.color.bottom_top_bar));
    }

}
