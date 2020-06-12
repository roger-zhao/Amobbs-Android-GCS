package org.farring.gcs.activities.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback;
import com.dronekit.api.CommonApiUtils;
import com.dronekit.core.MAVLink.connection.MavLinkConnectionTypes;
import com.dronekit.core.drone.DroneManager;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.SimpleCommandListener;
import com.dronekit.core.error.CommandExecutionError;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.AppService;
import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.R;
import org.farring.gcs.dialogs.SlideToUnlockDialog;
import org.farring.gcs.fragments.actionbar.VehicleStatusFragment;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.utils.unit.UnitManager;
import org.farring.gcs.utils.unit.systems.UnitSystem;

/**
 * Parent class for the app activity classes.
 */
public abstract class SuperUI extends AppCompatActivity implements ServiceConnection {

    public static final String ACTION_TOGGLE_DRONE_CONNECTION = Utils.PACKAGE_NAME + ".ACTION_TOGGLE_DRONE_CONNECTION";
    /**
     * Handle to the app preferences.
     */
    protected DroidPlannerPrefs mAppPrefs;
    protected UnitSystem unitSystem;
    protected FishDroneGCSApp dpApp;
//    private ScreenOrientation screenOrientation = new ScreenOrientation(this);
    private VehicleStatusFragment statusFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getApplicationContext();
        dpApp = (FishDroneGCSApp) getApplication();
        mAppPrefs = DroidPlannerPrefs.getInstance(context);
        unitSystem = UnitManager.getUnitSystem(context);

        // 屏幕常亮
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

//        screenOrientation.unlock();
        Utils.updateUILanguage(context);

        bindService(new Intent(context, AppService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        unitSystem = UnitManager.getUnitSystem(getApplicationContext());
        EventBus.getDefault().post(ActionEvent.ACTION_MISSION_PROXY_UPDATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTED:
                invalidateOptionsMenu();
//                screenOrientation.requestLock();
                break;

            case STATE_DISCONNECTED:
                invalidateOptionsMenu();
//                screenOrientation.unlock();
                break;
        }
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_ADVANCED_MENU_UPDATED:
                supportInvalidateOptionsMenu();
                break;
        }
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);

        final int toolbarId = getToolbarId();
        final Toolbar toolbar = (Toolbar) findViewById(toolbarId);
        initToolbar(toolbar);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        final int toolbarId = getToolbarId();
        final Toolbar toolbar = (Toolbar) findViewById(toolbarId);
        initToolbar(toolbar);
    }

    protected void initToolbar(Toolbar toolbar) {
        if (toolbar == null)
            return;

        setSupportActionBar(toolbar);

//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setHomeButtonEnabled(true);
//            actionBar.setDisplayShowTitleEnabled(false);
//        }

        addToolbarFragment();
    }

    public void setToolbarTitle(int titleResId) {
        if (statusFragment == null)
            return;

        statusFragment.setTitle(getString(titleResId));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    protected void addToolbarFragment() {
        final int toolbarId = getToolbarId();
        final FragmentManager fm = getSupportFragmentManager();
        statusFragment = (VehicleStatusFragment) fm.findFragmentById(toolbarId);
        if (statusFragment == null) {
            statusFragment = new VehicleStatusFragment();
            fm.beginTransaction().add(toolbarId, statusFragment).commit();
        }
    }

    protected abstract int getToolbarId();

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_super_activiy, menu);

        // 获取菜单子项
        final MenuItem toggleConnectionItem = menu.findItem(R.id.menu_connect);

        Drone drone = dpApp.getDrone();
        if (!drone.isConnected()) {
            menu.setGroupEnabled(R.id.menu_group_connected, false);
            menu.setGroupVisible(R.id.menu_group_connected, false);

            toggleConnectionItem.setTitle(R.string.menu_connect);

            return super.onCreateOptionsMenu(menu);
        }

        menu.setGroupEnabled(R.id.menu_group_connected, true);
        menu.setGroupVisible(R.id.menu_group_connected, true);

        final MenuItem killSwitchItem = menu.findItem(R.id.menu_kill_switch);
        final boolean isKillEnabled = mAppPrefs.isKillSwitchEnabled();
        if (killSwitchItem != null && isKillEnabled) {
            if (CommonApiUtils.isKillSwitchSupported(drone)) {
                killSwitchItem.setEnabled(true);
                killSwitchItem.setVisible(true);
            } else {
                killSwitchItem.setEnabled(false);
                killSwitchItem.setVisible(false);
            }
        }

        final boolean areMissionMenusEnabled = enableMissionMenus();
        // 上传航点
        final MenuItem sendMission = menu.findItem(R.id.menu_upload_mission);
        sendMission.setEnabled(areMissionMenusEnabled);
        sendMission.setVisible(areMissionMenusEnabled);

        // 下载航点
        final MenuItem loadMission = menu.findItem(R.id.menu_download_mission);
        loadMission.setEnabled(areMissionMenusEnabled);
        loadMission.setVisible(areMissionMenusEnabled);

        // 设置标题
        toggleConnectionItem.setTitle(R.string.menu_disconnect);

        return super.onCreateOptionsMenu(menu);
    }

    protected boolean enableMissionMenus() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                toggleDroneConnection();
                return true;

            case R.id.menu_upload_mission: {
                UploadMission();
                return true;
            }

            case R.id.menu_download_mission:
                downloadMission();
                return true;

            case R.id.menu_kill_switch:
                killSwitch();
                return true;

            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void killSwitch(){
        SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("紧急停机", new Runnable() {
            @Override
            public void run() {
                CommonApiUtils.arm(dpApp.getDrone(), false, true, new SimpleCommandListener() {
                    @Override
                    public void onError(int error) {
                        final int errorMsgId;
                        switch (error) {
                            case CommandExecutionError.COMMAND_UNSUPPORTED:
                                errorMsgId = R.string.error_kill_switch_unsupported;
                                break;

                            default:
                                errorMsgId = R.string.error_kill_switch_failed;
                                break;
                        }

                        Toast.makeText(getApplicationContext(), errorMsgId, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onTimeout() {
                        Toast.makeText(getApplicationContext(), R.string.error_kill_switch_failed, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        unlockDialog.show(getSupportFragmentManager(), "Slide to use the Kill Switch");
    }


    public void downloadMission(){
        dpApp.getDrone().getWaypointManager().getWaypoints();
        Toast.makeText(getApplicationContext(), "正在接收航点任务，请耐心等待……", Toast.LENGTH_LONG).show();
    }

    public void UploadMission(){
        final MissionProxy missionProxy = dpApp.getMissionProxy();
        if (missionProxy.getItems().isEmpty() || missionProxy.hasTakeoffAndLandOrRTL()) {
            missionProxy.sendMissionToAPM();
        } else {
            new MaterialDialog.Builder(this)
                    .iconRes(R.drawable.ic_launcher)
                    .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                    .title(getString(R.string.mission_upload_title))
                    .content(getString(R.string.mission_upload_message))
                    .positiveText(getString(android.R.string.yes))
                    .negativeText(getString(android.R.string.no))
                    .onPositive(new SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            missionProxy.addTakeOffAndRTL();
                            missionProxy.sendMissionToAPM();
                        }
                    })
                    .onNegative(new SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            missionProxy.sendMissionToAPM();
                        }
                    })
                    .show();
        }
    }

    public void toggleDroneConnection() {
        final DroneManager droneManager = dpApp.getDroneManager();

        if (!droneManager.isConnected() &&
                DroidPlannerPrefs.getInstance(this).getConnectionParameterType() == MavLinkConnectionTypes.MAVLINK_CONNECTION_BLUETOOTH) {
            // Launch a bluetooth device selection screen for the user
            final String address = mAppPrefs.getBluetoothDeviceAddress();
            if (address == null || address.isEmpty()) {
                startActivity(new Intent(getApplicationContext(), BluetoothDevicesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            }
        }

        if (droneManager.isConnected()) {
            droneManager.disconnect();
        } else {
            droneManager.connect();
        }
    }
}