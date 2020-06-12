package org.farring.gcs.notifications;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Altitude;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.drone.variables.State;
import com.dronekit.core.error.ErrorType;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;
import com.evenbus.TTSEvent;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.notifications.NotificationHandler.NotificationProvider;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements DroidPlanner audible notifications.
 */
public class TTSNotificationProvider implements NotificationProvider {

    private static final long WARNING_DELAY = 1500l; //ms
    public final Watchdog watchdogCallback = new Watchdog();
    private final AtomicBoolean isMaxAltExceeded = new AtomicBoolean(false);
    private final Context context;
    private final DroidPlannerPrefs mAppPrefs;
    private final Drone drone;
    private final Handler handler = new Handler();

    // 语音合成对象
    private SpeechSynthesizer mTts;
    private final Runnable maxAltitudeExceededWarning = new Runnable() {
        @Override
        public void run() {
            speak(context.getString(R.string.speak_warning_max_alt_exceed));
            handler.removeCallbacks(maxAltitudeExceededWarning);
        }
    };
    private int statusInterval;

    TTSNotificationProvider(Context context, Drone drone) {
        this.context = context;
        this.drone = drone;
        mAppPrefs = DroidPlannerPrefs.getInstance(context);
    }

    @Subscribe
    public void onReceiveTTSEvent(TTSEvent ttsEvent) {
        if (ttsEvent != null) {
            speak(ttsEvent.getContents());
        }
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_UPDATE_VOICE:
                if (mTts == null)
                    return;
                // 设置在线合成发音人
                mTts.setParameter(SpeechConstant.VOICE_NAME, mAppPrefs.getTTSVoicer());
                break;

            case ACTION_UPDATED_STATUS_PERIOD:
                scheduleWatchdog();
                break;
        }
    }

    private void scheduleWatchdog() {
        handler.removeCallbacks(watchdogCallback);
        statusInterval = mAppPrefs.getSpokenStatusInterval();
        if (statusInterval != 0) {
            handler.postDelayed(watchdogCallback, statusInterval * 1000);
        }
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        if (mTts == null)
            return;
        State droneState = drone.getState();
        switch (attributeEvent) {
            case STATE_ARMING:
                if (droneState != null)
                    speakArmedState(droneState.isArmed());
                break;

            case STATE_VEHICLE_MODE:
                if (droneState != null)
                    speak(droneState.getMode().getLabel());
                break;

            case MISSION_SENT:
                Toast.makeText(context, R.string.toast_mission_sent, Toast.LENGTH_SHORT).show();
                speak(context.getString(R.string.speak_mission_sent));
                break;

            case GPS_FIX:
                Gps droneGps = drone.getVehicleGps();
                if (droneGps != null)
                    speakGpsMode(droneGps.getFixType());
                break;

            case MISSION_RECEIVED:
                Toast.makeText(context, R.string.toast_mission_received, Toast.LENGTH_SHORT).show();
                speak(context.getString(R.string.speak_mission_received));
                break;

            case HEARTBEAT_FIRST:
                speak(context.getString(R.string.speak_heartbeat_first));
                break;

            case HEARTBEAT_TIMEOUT:
                if (mAppPrefs.getWarningOnLostOrRestoredSignal()) {
                    speak(context.getString(R.string.speak_heartbeat_timeout));
                    handler.removeCallbacks(watchdogCallback);
                }
                break;

            case HEARTBEAT_RESTORED:
                scheduleWatchdog();
                if (mAppPrefs.getWarningOnLostOrRestoredSignal()) {
                    speak(context.getString(R.string.speak_heartbeat_restored));
                }
                break;

            case MISSION_ITEM_UPDATED:
                int currentWaypoint = drone.getMissionStats().getCurrentWP();
                if (currentWaypoint != 0) {
                    // Zeroth waypoint is the home location.
                    speak(context.getString(R.string.speak_mission_item_updated, currentWaypoint));
                }
                break;

            case FOLLOW_START:
                speak(context.getString(R.string.speak_follow_start));
                break;

            case ALTITUDE_UPDATED:
                final Altitude altitude = drone.getAltitude();
                if (mAppPrefs.hasExceededMaxAltitude(altitude.getAltitude())) {
                    if (isMaxAltExceeded.compareAndSet(false, true)) {
                        handler.postDelayed(maxAltitudeExceededWarning, WARNING_DELAY);
                    }
                } else {
                    handler.removeCallbacks(maxAltitudeExceededWarning);
                    isMaxAltExceeded.set(false);
                }
                break;

            case AUTOPILOT_ERROR:
                if (mAppPrefs.getWarningOnAutopilotWarning()) {
                    String errorId = drone.getState().getErrorId();
                    final ErrorType errorType = ErrorType.getErrorById(errorId);
                    if (errorType != null && errorType != ErrorType.NO_ERROR) {
                        speak(errorType.getLabel(context).toString());
                    }
                }
                break;

            case SIGNAL_WEAK:
                if (mAppPrefs.getWarningOnLowSignalStrength()) {
                    speak(context.getString(R.string.speak_warning_signal_weak));
                }
                break;

            case WARNING_NO_GPS:
                speak(context.getString(R.string.speak_warning_no_gps));
                break;

            case HOME_UPDATED:
                if (droneState.isFlying()) {
                    // Warn the user the home location was just updated while in flight.
                    if (mAppPrefs.getWarningOnVehicleHomeUpdate()) {
                        speak(context.getString(R.string.speak_warning_vehicle_home_updated));
                    }
                }
                break;
            case ESC_CALIBRATION:
                speak("开始电调校准，请重启飞行器");
                break;
            case RC_CALIBRATION_DONE:
                speak("遥控器校准完成");
                break;
            case MAG_CALIBRATION_DONE:
                speak("指南针校准完成");
                break;
            case LOW_BATTERY:
                speak("电池电量不足");
                break;
            case RC_LOST:
                speak("遥控器关闭或丢失");
                break;
            case LIDAR_ENABLED:
                speak("开启定高雷达");
                break;
            case LIDAR_DISABLED:
                speak("关闭定高雷达");
                break;
        }
    }

    @Override
    public void init() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(context, new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Toast.makeText(context, "语音合成系统初始化失败,错误代码：" + code, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ********** 设置播放器的参数 *************************
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, mAppPrefs.getTTSVoicer());
        // 设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        // 设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        // 设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "100");
        // 设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    @Override
    public void onTerminate() {
        EventBus.getDefault().unregister(this);
        speak(context.getString(R.string.speak_disconected));

        if (mTts != null) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
            mTts = null;
        }
    }

    /**
     * 暴露方法
     *
     * @param string
     */
    public void speak(String string) {
        if (mTts != null && mAppPrefs.isTtsEnabled()) {
            mTts.startSpeaking(string, null);
        }
    }

    private void speakArmedState(boolean armed) {
        if (armed) {
            speak(context.getString(R.string.speak_armed));
        } else {
            speak(context.getString(R.string.speak_disarmed));
        }
    }

    private void speakGpsMode(int fix) {
        switch (fix) {
            case 2:
                speak(context.getString(R.string.gps_mode_2d_lock));
                break;
            case 3:
                speak(context.getString(R.string.gps_mode_3d_lock));
                break;
            case 4:
                speak(context.getString(R.string.gps_mode_3d_dgps_lock));
                break;
            case 5:
                speak(context.getString(R.string.gps_mode_3d_rtk_lock));
                break;
            default:
                speak(context.getString(R.string.gps_mode_lost_gps_lock));
                break;
        }
    }

    private class Watchdog implements Runnable {
        private final StringBuilder mMessageBuilder = new StringBuilder();

        public void run() {
            handler.removeCallbacks(watchdogCallback);
            if (drone != null) {
                if (drone.isConnected() && drone.getState().isArmed())
                    speakPeriodic(drone);
            }

            if (statusInterval != 0) {
                handler.postDelayed(watchdogCallback, statusInterval * 1000);
            }
        }

        // Periodic status preferences
        private void speakPeriodic(Drone drone) {
            final Map<String, Boolean> speechPrefs = mAppPrefs.getPeriodicSpeechPrefs();

            mMessageBuilder.setLength(0);
            if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_BAT_VOLT)) {
                mMessageBuilder.append(context.getString(R.string.periodic_status_bat_volt, drone.getBattery().getBatteryVoltage()));
                mMessageBuilder.append(",,,");
            }


            if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_ALT)) {
                mMessageBuilder.append(context.getString(R.string.periodic_status_altitude, (int) (drone.getAltitude().getAltitude())));
                mMessageBuilder.append(",,,");
            }

            if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_AIRSPEED)) {
                mMessageBuilder.append(context.getString(R.string.periodic_status_airspeed, (int) (drone.getSpeed().getAirSpeed())));
                mMessageBuilder.append(",,,");
            }

            if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_RSSI)) {
                mMessageBuilder.append(context.getString(R.string.periodic_status_rssi, (int) drone.getSignal().getSignalStrength()));
            }

            speak(mMessageBuilder.toString());
        }
    }
}
