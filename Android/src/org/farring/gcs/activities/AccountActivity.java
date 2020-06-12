package org.farring.gcs.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.tlog.ui.FlightRecordFragment;

import org.farring.gcs.R;
import org.farring.gcs.activities.interfaces.AccountLoginListener;
import org.farring.gcs.fragments.account.Model.MyUser;
import org.farring.gcs.fragments.account.UserLoginFragment;

import cn.bmob.v3.BmobUser;

public class AccountActivity extends DrawerNavigationUI implements AccountLoginListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        if (savedInstanceState == null) {
            Fragment droneShare;

            // 判断用户是否登陆
            if (BmobUser.getCurrentUser(this, MyUser.class) != null)
                droneShare = new FlightRecordFragment();
            else
                droneShare = new UserLoginFragment();

            getSupportFragmentManager().beginTransaction().add(R.id.droneshare_account, droneShare).commit();
        }
    }

    @Override
    public void addToolbarFragment() {
        // nothing
    }

    // 登陆
    @Override
    public void onLogin() {
        Fragment currentFragment = getCurrentFragment();
        if (!(currentFragment instanceof FlightRecordFragment)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.droneshare_account, new FlightRecordFragment())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onFailedLogin() {

    }

    // 登出
    @Override
    public void onLogout() {
        Fragment currentFragment = getCurrentFragment();
        if (!(currentFragment instanceof UserLoginFragment)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.droneshare_account, new UserLoginFragment()).commitAllowingStateLoss();
        }
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.droneshare_account);
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }

    @Override
    protected int getNavigationDrawerMenuItemId() {
        return R.id.navigation_account;
    }
}
