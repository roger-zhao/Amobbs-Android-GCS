package com.tlog.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.dronekit.core.drone.autopilot.APMConstants;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.Type;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.utils.MathUtils;
import com.tlog.database.LogRecordBean;
import com.tlog.helper.TLogParser;
import com.tlog.helper.TLogParser.Event;
import com.tlog.helper.TLogParserCallback;

import org.farring.gcs.R;
import org.farring.gcs.maps.DPMap.PathSource;
import org.farring.gcs.utils.unit.UnitManager;
import org.farring.gcs.utils.unit.providers.speed.SpeedUnitProvider;
import org.farring.gcs.utils.unit.systems.UnitSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.grantland.widget.AutofitTextView;

/**
 * This implements the map locator activity. The map locator activity allows the user to find
 * a lost drone using last known GPS positions from the tlogs.
 */
public class HistoryActivity extends AppCompatActivity {

    @BindView(R.id.flightAltitudeTextView)
    AutofitTextView flightAltitudeTextView;
    @BindView(R.id.flightHomeDistanceTextView)
    AutofitTextView flightHomeDistanceTextView;
    @BindView(R.id.flightVerticalSpeedTextView)
    AutofitTextView flightVerticalSpeedTextView;
    @BindView(R.id.flightHorizontalSpeedTextView)
    AutofitTextView flightHorizontalSpeedTextView;
    @BindView(R.id.flightBatteryTextView)
    AutofitTextView flightBatteryTextView;
    @BindView(R.id.flightModeTextView)
    AutofitTextView flightModeTextView;
    @BindView(R.id.multipleSpeed)
    TextView multipleSpeedText;
    @BindView(R.id.number_progress_bar)
    NumberProgressBar numberProgressBar;
    @BindView(R.id.latitudeText)
    AutofitTextView latitudeText;
    @BindView(R.id.longitudeText)
    AutofitTextView longitudeText;
    @BindView(R.id.start_btn)
    ImageButton startBtn;

    private ReadLogThread loadLogThread;

    private List<Event> eventList;
    private Handler mHandler;
    private LogRecordBean logRecordBean;
    private HistoryMapFragment locatorMapFragment;
    private volatile boolean shutdownThreadRequested = false;
    private LatLong lastDroneLocation;
    /**
     * 【速率】
     * 1：1s     [基准]
     * 2：0.5s
     * 4：0.25s
     * 8：0.125s
     * 16:
     */
    private int multipleSpeed = 1;

    private static LatLong coordFromMsgGlobalPositionInt(msg_global_position_int msg) {
        double lat = msg.lat;
        lat /= 1E7;

        double lon = msg.lon;
        lon /= 1E7;

        return new LatLong(lat, lon);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);

        FragmentManager fragmentManager = getSupportFragmentManager();
        locatorMapFragment = ((HistoryMapFragment) fragmentManager.findFragmentById(R.id.locator_map_fragment));
        if (locatorMapFragment == null) {
            locatorMapFragment = new HistoryMapFragment();
            fragmentManager.beginTransaction().add(R.id.locator_map_fragment, locatorMapFragment).commit();
        }

        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onStart() {
        super.onStart();
        initControlMap();
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .iconRes(R.drawable.ic_launcher)
                .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                .content("日志读取中……")
                .contentGravity(GravityEnum.CENTER)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();

        // 1.读取日志信息
        logRecordBean = (LogRecordBean) getIntent().getSerializableExtra("data");

        if (logRecordBean == null) {
            Toast.makeText(this, "读取日志出错，请重试", Toast.LENGTH_SHORT).show();
            finish();
        }

        TLogParser.getAllEventsAsync(mHandler, new File(logRecordBean.getFilePath()), new TLogParserCallback() {
            @Override
            public void onResult(final List<Event> events) {
                // 储存事情
                HistoryActivity.this.eventList = events;

                // 根据列表中的所有经纬点画出路径红线
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        final ArrayList<LatLong> pathList = new ArrayList<>();
                        for (final Event event : eventList) {
                            switch (event.getMavLinkMessage().msgid) {
                                case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                                    LatLong latLong = coordFromMsgGlobalPositionInt((msg_global_position_int) event.getMavLinkMessage());
                                    if (latLong.getLongitude() != 0 && latLong.getLatitude() != 0)
                                        pathList.add(latLong);
                            }
                        }

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                locatorMapFragment.getMapFragment().updateDroneLeashPath(new PathSource() {
                                    @Override
                                    public List<LatLong> getPathPoints() {
                                        return pathList;
                                    }
                                });
                                dialog.dismiss();
                            }
                        });
                    }
                }.start();
                loadLogThread = new ReadLogThread();
                loadLogThread.start();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(HistoryActivity.this, "读取日志出错，请重试！", Toast.LENGTH_LONG).show();
                dialog.dismiss();
                HistoryActivity.this.finish();
            }
        });
    }

    private void initControlMap() {
        // 清空地图
        locatorMapFragment.getMapFragment().clearFlightPath();
        locatorMapFragment.getMapFragment().clearMarkers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shutdownThreadRequested = true;
    }

    @OnClick({R.id.my_location_button, R.id.drone_location_button, R.id.start_btn, R.id.multipleSpeed})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.my_location_button:
                locatorMapFragment.goToMyLocation();
                break;

            case R.id.drone_location_button:
                locatorMapFragment.getMapFragment().updateCamera(lastDroneLocation, 17);
                break;

            case R.id.start_btn:
                if (!shutdownThreadRequested) {
                    // 线程处于运行状态，切换为暂停状态
                    shutdownThreadRequested = true;
                    loadLogThread.interrupt();
                    loadLogThread = null;
                    startBtn.setImageResource(R.drawable.ic_pause_blue_48dp);
                } else {
                    // 线程处于暂停状态，切换为开始状态
                    shutdownThreadRequested = false;
                    startBtn.setImageResource(R.drawable.ic_play_arrow_blue_48dp);
                    if (loadLogThread == null) {
                        loadLogThread = new ReadLogThread();
                        loadLogThread.start();
                    }
                }
                break;

            case R.id.multipleSpeed:
                // 更改速度倍率
                switch (multipleSpeed) {
                    case 1:
                        multipleSpeedText.setText("X 2");
                        multipleSpeed = 2;
                        break;

                    case 2:
                        multipleSpeedText.setText("X 4");
                        multipleSpeed = 4;
                        break;

                    case 4:
                        multipleSpeedText.setText("X 8");
                        multipleSpeed = 8;
                        break;

                    case 8:
                        multipleSpeedText.setText("X 16");
                        multipleSpeed = 16;
                        break;

                    case 16:
                        multipleSpeedText.setText("X 1");
                        multipleSpeed = 1;
                        break;
                }
                break;
        }
    }

    public class ReadLogThread extends Thread {
        @Override
        public void run() {
            super.run();
            // 2.新开线程分析日志信息，并分发到主线程
            long tempControlTimeStamp = 0;
            final LatLong homeCoord = new LatLong(0, 0);
            final UnitSystem unitSystem = UnitManager.getUnitSystem(HistoryActivity.this);

            for (Event event : eventList) {
                if (shutdownThreadRequested) {
                    return;
                }

                // 时间控制
                if (event.getTimestamp() - tempControlTimeStamp > 1000) {
                    try {
                        // 线程休眠1s，等待下一条消息
                        Thread.sleep((1000 / multipleSpeed));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    tempControlTimeStamp = event.getTimestamp();
                }

                final long tempProgress = ((tempControlTimeStamp - logRecordBean.getLogStartTime()));
                final long allTimeStamp = ((logRecordBean.getLogEndTime() - logRecordBean.getLogStartTime()));
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        numberProgressBar.setMax((int) allTimeStamp / 1000);
                        numberProgressBar.setProgress((int) tempProgress / 1000);
                    }
                });

                // 事件控制
                switch (event.getMavLinkMessage().msgid) {
                    // 经纬度信息
                    case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                        final msg_global_position_int msg_position = (msg_global_position_int) event.getMavLinkMessage();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                lastDroneLocation = coordFromMsgGlobalPositionInt(msg_position);

                                locatorMapFragment.getMapFragment().addFlightPathPoint(lastDroneLocation);
                                locatorMapFragment.updateMarkerPosition(lastDroneLocation);
                                locatorMapFragment.updateMarkerHeading(msg_position.hdg / 100);

                                latitudeText.setText("纬度:" + lastDroneLocation.getLatitude());
                                longitudeText.setText("经度:" + lastDroneLocation.getLongitude());

                                String distanceToHome = "距离:--";
                                if (homeCoord.getLongitude() != 0 && homeCoord.getLatitude() != 0) {
                                    distanceToHome = "距离:" + unitSystem.getLengthUnitProvider().boxBaseValueToTarget(MathUtils.getDistance2D(homeCoord, lastDroneLocation)).toString();
                                }
                                flightHomeDistanceTextView.setText(distanceToHome);
                            }
                        });
                        break;

                    // 速度
                    case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                        final msg_vfr_hud msg_vfr = (msg_vfr_hud) event.getMavLinkMessage();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                final SpeedUnitProvider speedUnitProvider = unitSystem.getSpeedUnitProvider();
                                flightHorizontalSpeedTextView.setText("水平:" + speedUnitProvider.boxBaseValueToTarget(msg_vfr.groundspeed));
                                flightVerticalSpeedTextView.setText("垂直:" + speedUnitProvider.boxBaseValueToTarget(msg_vfr.climb));
                                flightAltitudeTextView.setText("高度:" + unitSystem.getLengthUnitProvider().boxBaseValueToTarget(msg_vfr.alt).toString());
                            }
                        });
                        break;

                    // 心跳包
                    case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                        final msg_heartbeat msg_heartbeat = (msg_heartbeat) event.getMavLinkMessage();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (Type.isCopter(msg_heartbeat.type)) {
                                    flightModeTextView.setText(ApmModes.getMode(msg_heartbeat.custom_mode, Type.TYPE_COPTER).getLabel());
                                } else if (Type.isPlane(msg_heartbeat.type)) {
                                    flightModeTextView.setText(ApmModes.getMode(msg_heartbeat.custom_mode, Type.TYPE_PLANE).getLabel());
                                }
                            }
                        });
                        break;

                    // 电池电量
                    case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
                        final msg_sys_status msg_sys_status = (msg_sys_status) event.getMavLinkMessage();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                flightBatteryTextView.setText("电压:" + msg_sys_status.voltage_battery / 1000.0 + "V");
                            }
                        });
                        break;

                    // 家的位置
                    case msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM:
                        final msg_mission_item msg_mission_item = (msg_mission_item) event.getMavLinkMessage();
                        if (msg_mission_item.seq != APMConstants.HOME_WAYPOINT_INDEX) {
                            break;
                        }
                        homeCoord.setLatitude(msg_mission_item.x);
                        homeCoord.setLongitude(msg_mission_item.y);
                        break;
                }
            }
        }
    }
}