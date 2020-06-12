package org.farring.gcs.utils.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.dronekit.core.MAVLink.connection.MavLinkConnectionTypes;
import com.dronekit.core.drone.Preferences;
import com.dronekit.core.drone.profiles.VehicleProfile;
import com.dronekit.core.drone.variables.StreamRates.Rates;
import com.dronekit.core.firmware.FirmwareType;
import com.dronekit.utils.file.IO.VehicleProfileReader;

import org.greenrobot.eventbus.EventBus;
import org.farring.gcs.fragments.widget.TowerWidgets;
import org.farring.gcs.maps.providers.DPMapProvider;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.unit.systems.UnitSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides structured access to Droidplanner preferences
 * <p/>
 * Over time it might be good to move the various places that are doing
 * prefs.getFoo(blah, default) here - to collect prefs in one place and avoid
 * duplicating string constants (which tend to become stale as code evolves).
 * This is called the DRY (don't repeat yourself) principle of software
 * development.
 */
public class DroidPlannerPrefs implements Preferences {
    /*
     * Default preference value
     */
    public static final String PREF_CONNECTION_TYPE = "pref_connection_param_type";
    public static final String DEFAULT_CONNECTION_TYPE = String.valueOf(MavLinkConnectionTypes.MAVLINK_CONNECTION_USB);
    public static final String PREF_MAPS_PROVIDERS = "pref_maps_providers_key";
    public static final boolean DEFAULT_PREF_UI_LANGUAGE = false;
    public static final String PREF_MAX_ALT_WARNING = "pref_max_alt_warning";
    public static final boolean DEFAULT_MAX_ALT_WARNING = false;
    public static final String PREF_TTS_LOST_SIGNAL = "tts_lost_signal";
    public static final boolean DEFAULT_TTS_WARNING_LOST_SIGNAL = true;
    public static final String PREF_TTS_LOW_SIGNAL = "tts_low_signal";
    public static final boolean DEFAULT_TTS_WARNING_LOW_SIGNAL = false;
    public static final String PREF_TTS_AUTOPILOT_WARNING = "tts_autopilot_warning";
    public static final boolean DEFAULT_TTS_WARNING_AUTOPILOT_WARNING = true;
    public static final String PREF_USB_BAUD_RATE = "pref_baud_type";
    public static final String PREF_TCP_SERVER_IP = "pref_server_ip";
    public static final String PREF_TCP_SERVER_PORT = "pref_server_port";
    public static final String PREF_UDP_PING_RECEIVER_IP = "pref_udp_ping_receiver_ip";
    public static final String PREF_UDP_PING_RECEIVER_PORT = "pref_udp_ping_receiver_port";
    public static final String PREF_UDP_SERVER_PORT = "pref_udp_server_port";
    public static final String PREF_UNIT_SYSTEM = "pref_unit_system";
    public static final String PREF_WARNING_GROUND_COLLISION = "pref_ground_collision_warning";
    public static final String PREF_ENABLE_MAP_ROTATION = "pref_map_enable_rotation";
    public static final String PREF_ENABLE_KILL_SWITCH = "pref_enable_kill_switch";
    public static final String PREF_ENABLE_UDP_PING = "pref_enable_udp_server_ping";
    public static final String PREF_ALT_MAX_VALUE = "pref_alt_max_value";
    public static final String PREF_ALT_MIN_VALUE = "pref_alt_min_value";
    public static final String PREF_ALT_DEFAULT_VALUE = "pref_alt_default_value";
    public static final String PREF_BT_DEVICE_ADDRESS = "pref_bluetooth_device_address";
    public static final String PREF_SHOW_GPS_HDOP = "pref_ui_gps_hdop";
    public static final boolean DEFAULT_SHOW_GPS_HDOP = false;
    public static final String PREF_TTS_VOICER = "tts_voicer";
    public static final String PREF_TTS_VOICER_Num = "tts_voicer_num";
    public static final String PREF_TTS_PERIODIC_BAT_VOLT = "tts_periodic_bat_volt";
    public static final String PREF_TTS_PERIODIC_ALT = "tts_periodic_alt";
    public static final String PREF_TTS_PERIODIC_RSSI = "tts_periodic_rssi";
    public static final String PREF_TTS_PERIODIC_AIRSPEED = "tts_periodic_airspeed";
    public static final String PREF_TOWER_WIDGETS = "pref_tower_widgets";
    public static final String ACTION_PREF_RETURN_TO_ME_UPDATED = Utils.PACKAGE_NAME + ".action.PREF_RETURN_TO_ME_UPDATED";
    public static final String PREF_RETURN_TO_ME = "pref_enable_return_to_me";
    public static final boolean DEFAULT_RETURN_TO_ME = false;
    public static final String PREF_VEHICLE_HOME_UPDATE_WARNING = "pref_vehicle_home_update_warning";
    public static final boolean DEFAULT_VEHICLE_HOME_UPDATE_WARNING = true;
    public static final String PREF_UVC_VIDEO_ASPECT_RATIO = "pref_uvc_video_aspect_ratio";
    public static final int DEFAULT_SPEECH_PERIOD = 0;
    private static final boolean DEFAULT_KEEP_SCREEN_ON = true;
    private static final String DEFAULT_MAPS_PROVIDER = DPMapProvider.DEFAULT_MAP_PROVIDER.name();
    private static final AutoPanMode DEFAULT_AUTO_PAN_MODE = AutoPanMode.DISABLED;
    private static final String PREF_UI_LANGUAGE = "pref_ui_language_english";
    private static final String DEFAULT_USB_BAUD_RATE = "57600";
    private static final String DEFAULT_TCP_SERVER_IP = "192.168.40.100";
    private static final String DEFAULT_TCP_SERVER_PORT = "5763";
    private static final String DEFAULT_UDP_SERVER_PORT = "14550";
    private static final int DEFAULT_UNIT_SYSTEM = UnitSystem.METRIC;
    private static final boolean DEFAULT_WARNING_GROUND_COLLISION = true;
    private static final boolean DEFAULT_ENABLE_MAP_ROTATION = true;
    private static final boolean DEFAULT_ENABLE_KILL_SWITCH = false;
    private static final boolean DEFAULT_ENABLE_UDP_PING = false;
    private static final double DEFAULT_MAX_ALT = 200; //meters
    private static final double DEFAULT_MIN_ALT = 1; // meter
    private static final double DEFAULT_ALT = 5; // meters
    private static final String PREF_IS_TTS_ENABLED = "pref_enable_tts";
    private static final boolean DEFAULT_TTS_ENABLED = true;
    private static final String PREF_BT_DEVICE_NAME = "pref_bluetooth_device_name";
    private static final String PREF_UI_REALTIME_FOOTPRINTS = "pref_ui_realtime_footprints_key";
    private static final boolean DEFAULT_UI_REALTIME_FOOTPRINTS = false;
    private static final boolean DEFAULT_TTS_PERIODIC_BAT_VOLT = true;
    private static final boolean DEFAULT_TTS_PERIODIC_ALT = true;
    private static final boolean DEFAULT_TTS_PERIODIC_RRSI = true;
    private static final boolean DEFAULT_TTS_PERIODIC_AIRSPEED = true;
    private static final float DEFAULT_UVC_VIDEO_ASPECT_RATIO = 3f / 4f;
    private static final String PREF_SPEECH_PERIOD = "tts_periodic_status_period";
    private static DroidPlannerPrefs instance;
    // Public for legacy usage
    public final SharedPreferences prefs;
    private final Context context;

    public DroidPlannerPrefs(Context context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized DroidPlannerPrefs getInstance(Context context) {
        if (instance == null) {
            instance = new DroidPlannerPrefs(context);
        }

        return instance;
    }

    /**
     * How many times has this application been started? (will increment for each call)
     */
    public int getNumberOfRuns() {
        int r = prefs.getInt("num_runs", 0) + 1;

        prefs.edit().putInt("num_runs", r).apply();

        return r;
    }

    /**
     * @return the selected mavlink connection type.
     */
    public int getConnectionParameterType() {
        return Integer.parseInt(prefs.getString(PREF_CONNECTION_TYPE, DEFAULT_CONNECTION_TYPE));
    }

    public void setConnectionParameterType(int connectionType) {
        prefs.edit().putString(PREF_CONNECTION_TYPE, String.valueOf(connectionType)).apply();
    }

    /**
     * 获取当前单位系统类型
     */
    public int getUnitSystemType() {
        return prefs.getInt(PREF_UNIT_SYSTEM, DEFAULT_UNIT_SYSTEM);
    }

    /**
     * 设置当前单位系统类型
     */
    public void setUnitSystemType(int unitSystemType) {
        prefs.edit().putInt(PREF_UNIT_SYSTEM, unitSystemType).apply();
    }

    public int getUsbBaudRate() {
        return Integer.parseInt(prefs.getString(PREF_USB_BAUD_RATE, DEFAULT_USB_BAUD_RATE));
    }

    public void setUsbBaudRate(int baudRate) {
        prefs.edit().putString(PREF_USB_BAUD_RATE, String.valueOf(baudRate)).apply();
    }

    public String getTcpServerIp() {
        return prefs.getString(PREF_TCP_SERVER_IP, DEFAULT_TCP_SERVER_IP);
    }

    public void setTcpServerIp(String serverIp) {
        prefs.edit().putString(PREF_TCP_SERVER_IP, serverIp).apply();
    }

    public int getTcpServerPort() {
        return Integer.parseInt(prefs.getString(PREF_TCP_SERVER_PORT, DEFAULT_TCP_SERVER_PORT));
    }

    public void setTcpServerPort(int serverPort) {
        prefs.edit().putString(PREF_TCP_SERVER_PORT, String.valueOf(serverPort)).apply();
    }

    public int getUdpServerPort() {
        return Integer.parseInt(prefs.getString(PREF_UDP_SERVER_PORT, DEFAULT_UDP_SERVER_PORT));
    }

    public void setUdpServerPort(int serverPort) {
        prefs.edit().putString(PREF_UDP_SERVER_PORT, String.valueOf(serverPort)).apply();
    }

    public boolean isUdpPingEnabled() {
        return prefs.getBoolean(PREF_ENABLE_UDP_PING, DEFAULT_ENABLE_UDP_PING);
    }

    public String getUdpPingReceiverIp() {
        return prefs.getString(PREF_UDP_PING_RECEIVER_IP, null);
    }

    public int getUdpPingReceiverPort() {
        return Integer.parseInt(prefs.getString(PREF_UDP_PING_RECEIVER_PORT, DEFAULT_UDP_SERVER_PORT));
    }

    public String getBluetoothDeviceName() {
        return prefs.getString(PREF_BT_DEVICE_NAME, null);
    }

    public void setBluetoothDeviceName(String deviceName) {
        prefs.edit().putString(PREF_BT_DEVICE_NAME, deviceName).apply();
    }

    public String getBluetoothDeviceAddress() {
        return prefs.getString(PREF_BT_DEVICE_ADDRESS, null);
    }

    public void setBluetoothDeviceAddress(String newAddress) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_BT_DEVICE_ADDRESS, newAddress)
                .apply();
    }

    /**
     * @return the target for the map auto panning.
     */
    public AutoPanMode getAutoPanMode() {
        final String defaultAutoPanModeName = DEFAULT_AUTO_PAN_MODE.name();
        final String autoPanTypeString = prefs.getString(AutoPanMode.PREF_KEY,
                defaultAutoPanModeName);
        try {
            return AutoPanMode.valueOf(autoPanTypeString);
        } catch (IllegalArgumentException e) {
            return DEFAULT_AUTO_PAN_MODE;
        }
    }

    /**
     * Updates the map auto panning target.
     *
     * @param target
     */
    public void setAutoPanMode(AutoPanMode target) {
        prefs.edit().putString(AutoPanMode.PREF_KEY, target.name()).apply();
    }

    /**
     * Use HDOP instead of satellite count on infobar
     */
    public boolean shouldGpsHdopBeDisplayed() {
        return prefs.getBoolean(PREF_SHOW_GPS_HDOP, DEFAULT_SHOW_GPS_HDOP);
    }

    public boolean isEnglishDefaultLanguage() {
        return prefs.getBoolean(PREF_UI_LANGUAGE, DEFAULT_PREF_UI_LANGUAGE);
    }

    public boolean isRealtimeFootprintsEnabled() {
        return prefs.getBoolean(PREF_UI_REALTIME_FOOTPRINTS, DEFAULT_UI_REALTIME_FOOTPRINTS);
    }

    public String getMapProviderName() {
        return prefs.getString(PREF_MAPS_PROVIDERS, DEFAULT_MAPS_PROVIDER);
    }

    /**
     * Returns the map provider selected by the user.
     *
     * @return selected map provider
     */
    public DPMapProvider getMapProvider() {
        final String mapProviderName = getMapProviderName();
        return mapProviderName == null ? DPMapProvider.DEFAULT_MAP_PROVIDER : DPMapProvider.getMapProvider(mapProviderName);
    }

    /**
     * Updates the map provider.
     */
    public void setMapProvider(String mapProvider) {
        prefs.edit().putString(DroidPlannerPrefs.PREF_MAPS_PROVIDERS, mapProvider).apply();
    }

    public Map<String, Boolean> getPeriodicSpeechPrefs() {
        final Map<String, Boolean> speechPrefs = new HashMap<>();
        speechPrefs.put(PREF_TTS_PERIODIC_BAT_VOLT, prefs.getBoolean(PREF_TTS_PERIODIC_BAT_VOLT, DEFAULT_TTS_PERIODIC_BAT_VOLT));
        speechPrefs.put(PREF_TTS_PERIODIC_ALT, prefs.getBoolean(PREF_TTS_PERIODIC_ALT, DEFAULT_TTS_PERIODIC_ALT));
        speechPrefs.put(PREF_TTS_PERIODIC_AIRSPEED, prefs.getBoolean(PREF_TTS_PERIODIC_AIRSPEED, DEFAULT_TTS_PERIODIC_AIRSPEED));
        speechPrefs.put(PREF_TTS_PERIODIC_RSSI, prefs.getBoolean(PREF_TTS_PERIODIC_RSSI, DEFAULT_TTS_PERIODIC_RRSI));

        return speechPrefs;
    }

    public int getSpokenStatusInterval() {
        return prefs.getInt(PREF_SPEECH_PERIOD, DEFAULT_SPEECH_PERIOD);
    }

    public void setSpokenStatusInterval(int period) {
        prefs.edit().putInt(PREF_SPEECH_PERIOD, period).apply();
    }

    public int getSpokenStatusIntervalSelectedNum() {
        return prefs.getInt("Spoken_Status_Interval_Selected_Num", 0);
    }

    public void setSpokenStatusIntervalSelectedNum(int selectedNum) {
        prefs.edit().putInt("Spoken_Status_Interval_Selected_Num", selectedNum).apply();
    }

    public String getTTSVoicer() {
        return prefs.getString(PREF_TTS_VOICER, "xiaoyan");
    }

    public void setTTSVoicer(String voicer) {
        prefs.edit().putString(PREF_TTS_VOICER, voicer).apply();
    }

    public int getVoicerSelectedNum() {
        return prefs.getInt(PREF_TTS_VOICER_Num, 0);
    }

    public void setVoicerSelectedNum(int selectedNum) {
        prefs.edit().putInt(PREF_TTS_VOICER_Num, selectedNum).apply();
    }

    public boolean hasExceededMaxAltitude(double currentAltInMeters) {
        final boolean isWarningEnabled = prefs.getBoolean(PREF_MAX_ALT_WARNING, DEFAULT_MAX_ALT_WARNING);
        if (!isWarningEnabled)
            return false;

        final double maxAltitude = getMaxAltitude();
        return currentAltInMeters > maxAltitude;
    }

    public boolean getWarningOnLostOrRestoredSignal() {
        return prefs.getBoolean(PREF_TTS_LOST_SIGNAL, DEFAULT_TTS_WARNING_LOST_SIGNAL);
    }

    public boolean getWarningOnLowSignalStrength() {
        return prefs.getBoolean(PREF_TTS_LOW_SIGNAL, DEFAULT_TTS_WARNING_LOW_SIGNAL);
    }

    public boolean getWarningOnAutopilotWarning() {
        return prefs.getBoolean(PREF_TTS_AUTOPILOT_WARNING, DEFAULT_TTS_WARNING_AUTOPILOT_WARNING);
    }

    public boolean getImminentGroundCollisionWarning() {
        return prefs.getBoolean(PREF_WARNING_GROUND_COLLISION, DEFAULT_WARNING_GROUND_COLLISION);
    }

    public boolean isMapRotationEnabled() {
        return prefs.getBoolean(PREF_ENABLE_MAP_ROTATION, DEFAULT_ENABLE_MAP_ROTATION);
    }

    public boolean isKillSwitchEnabled() {
        return prefs.getBoolean(PREF_ENABLE_KILL_SWITCH, DEFAULT_ENABLE_KILL_SWITCH);
    }

    /**
     * @return the max altitude in meters
     */
    public double getMaxAltitude() {
        return getAltitudePreference(PREF_ALT_MAX_VALUE, DEFAULT_MAX_ALT);
    }

    /**
     * @return the min altitude in meters
     */
    public double getMinAltitude() {
        return getAltitudePreference(PREF_ALT_MIN_VALUE, DEFAULT_MIN_ALT);
    }

    /**
     * @return the default starting altitude in meters
     */
    public double getDefaultAltitude() {
        return getAltitudePreference(PREF_ALT_DEFAULT_VALUE, DEFAULT_ALT);
    }

    public void setAltitudePreference(String prefKey, double altitude) {
        prefs.edit().putString(prefKey, String.valueOf(altitude)).apply();
    }

    private double getAltitudePreference(String prefKey, double defaultValue) {
        final String maxAltValue = prefs.getString(prefKey, null);
        if (TextUtils.isEmpty(maxAltValue))
            return defaultValue;

        try {
            final double maxAlt = Double.parseDouble(maxAltValue);
            return maxAlt;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean isTtsEnabled() {
        return prefs.getBoolean(PREF_IS_TTS_ENABLED, DEFAULT_TTS_ENABLED);
    }

    public void setIsTtsEnabled(boolean enable) {
        prefs.edit().putBoolean(PREF_IS_TTS_ENABLED, enable).apply();
    }

    public void enableWidget(TowerWidgets widget, boolean enable) {
        prefs.edit().putBoolean(widget.getPrefKey(), enable).apply();
    }

    public boolean isWidgetEnabled(TowerWidgets widget) {
        return prefs.getBoolean(widget.getPrefKey(), widget.isEnabledByDefault());
    }

    public boolean isReturnToMeEnabled() {
        return prefs.getBoolean(PREF_RETURN_TO_ME, DEFAULT_RETURN_TO_ME);
    }

    public void enableReturnToMe(boolean isEnabled) {
        prefs.edit().putBoolean(PREF_RETURN_TO_ME, isEnabled).apply();
        EventBus.getDefault().post(ACTION_PREF_RETURN_TO_ME_UPDATED);
    }

    public boolean getWarningOnVehicleHomeUpdate() {
        return prefs.getBoolean(PREF_VEHICLE_HOME_UPDATE_WARNING, DEFAULT_VEHICLE_HOME_UPDATE_WARNING);
    }

    public Float getUVCVideoAspectRatio() {
        return prefs.getFloat(PREF_UVC_VIDEO_ASPECT_RATIO, DEFAULT_UVC_VIDEO_ASPECT_RATIO);
    }

    public void setUVCVideoAspectRatio(Float aspectRatio) {
        prefs.edit().putFloat(PREF_UVC_VIDEO_ASPECT_RATIO, aspectRatio).apply();
    }

    // 读取飞行器配置
    @Override
    public VehicleProfile loadVehicleProfile(FirmwareType firmwareType) {
        return VehicleProfileReader.load(context, firmwareType);
    }

    @Override
    public Rates getRates() {
        final int defaultRate = 2;

        Rates rates = new Rates();

        rates.extendedStatus = defaultRate; //Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_ext_stat", "2"));
        rates.extra1 = defaultRate; //Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_extra1", "2"));
        rates.extra2 = defaultRate; //Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_extra2", "2"));
        rates.extra3 = defaultRate; //Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_extra3", "2"));
        rates.position = defaultRate; //Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_position", "2"));
        rates.rcChannels = defaultRate; //Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_rc_channels", "2"));
        rates.rawSensors = defaultRate; //Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_raw_sensors", "2"));
        rates.rawController = defaultRate; //Integer.parseInt(prefs.getString ("pref_mavlink_stream_rate_raw_controller", "2"));

        return rates;
    }
}
