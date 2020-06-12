package org.farring.gcs.fragments.setting;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.ListCallbackSingleChoice;
import com.dronekit.core.drone.autopilot.Drone;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;
import com.tencent.bugly.beta.Beta;

import org.beyene.sius.unit.length.LengthUnit;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.SettingsConnextActivity;
import org.farring.gcs.activities.helpers.MapPreferencesActivity;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.fragments.widget.WidgetsListPrefFragment;
import org.farring.gcs.maps.providers.DPMapProvider;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.utils.unit.UnitManager;
import org.farring.gcs.utils.unit.providers.length.LengthUnitProvider;
import org.farring.gcs.utils.unit.systems.UnitSystem;
import org.farring.gcs.view.button.SpringSwitchButton;
import org.farring.gcs.view.button.ToggleButton;
import org.farring.gcs.view.button.ToggleButton.OnToggleChanged;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.grantland.widget.AutofitTextView;

/**
 * 作者： 林杰强
 * 日期： 2016/2/8 00:45.
 * 备注:
 */
public class SettingMainFragment extends BaseFragment {

    public static final String EXTRA_ADD_WIDGET = "extra_add_widget";
    public static final String EXTRA_WIDGET_PREF_KEY = "extra_widget_pref_key";
    private static final String PACKAGE_NAME = Utils.PACKAGE_NAME;

    /**
     * Action used to broadcast updates to the period for the spoken status summary.
     */
    public static final String ACTION_WIDGET_PREFERENCE_UPDATED = PACKAGE_NAME + ".ACTION_WIDGET_PREFERENCE_UPDATED";

    @BindView(R.id.TextView_MaxAltitude)
    TextView maxAltitudeText;
    @BindView(R.id.TextView_MinAltitude)
    TextView minAltitudeText;
    @BindView(R.id.TextView_DefaultAltitude)
    TextView defaultAltitudeText;
    @BindView(R.id.TextView_AppVersion)
    TextView appVersionText;
    @BindView(R.id.TextView_FirmwareVersion)
    TextView firmwareVersionText;
    @BindView(R.id.unit_type)
    SpringSwitchButton unitType;
    @BindView(R.id.ttsToggleButton)
    ToggleButton ttsToggleButton;
    @BindView(R.id.TextView_speechType)
    TextView speechTypeText;
    @BindView(R.id.tts_periodic_status_period)
    TextView ttsPeriodicStatusPeriod;
    @BindView(R.id.providerNameText)
    AutofitTextView providerNameText;

    private DroidPlannerPrefs dpPrefs;
    private String[] mCloudVoicersEntries;
    private Activity mActivity;

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);

        unitType.setOnToggleListener(new SpringSwitchButton.OnToggleListener() {
            @Override
            public void onToggle(boolean left) {
                left = true; // always meter
                if (left) {
                    dpPrefs.setUnitSystemType(UnitSystem.METRIC);
                } else {
                    dpPrefs.setUnitSystemType(UnitSystem.IMPERIAL);
                }
                EventBus.getDefault().post(ActionEvent.ACTION_PREF_UNIT_SYSTEM_UPDATE);
            }
        });

        ttsToggleButton.setOnToggleChanged(new OnToggleChanged() {
            @Override
            public void onToggle(boolean on) {
                dpPrefs.setIsTtsEnabled(on);
            }
        });

        updateContainerView();
        setupAltitudeTextView();
    }

    @OnClick({R.id.tower_widgets, R.id.ViewContainer_VersionUpdate, R.id.ViewContainer_helperDocument, R.id.ViewContainer_Connection,
            R.id.ViewContainer_MaxAltitude, R.id.ViewContainer_MinAltitude, R.id.ViewContainer_DefaultAltitude,
            R.id.ViewContainer_Speech, R.id.ViewContainer_Speech_Period, R.id.ViewContainer_MapSetting, R.id.ViewContainer_Provider})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ViewContainer_Connection:
                Intent intent = new Intent(getActivity(), SettingsConnextActivity.class);
                startActivity(intent);
                break;

            case R.id.tower_widgets:
                new WidgetsListPrefFragment().show(getFragmentManager(), "Widgets List Preferences");
                break;

            case R.id.ViewContainer_MaxAltitude:
            case R.id.ViewContainer_MinAltitude:
            case R.id.ViewContainer_DefaultAltitude:
                // 共有方法
                final LengthUnitProvider lup = getLengthUnitProvider();
                final View contentView = mActivity.getLayoutInflater().inflate(R.layout.dialog_edit_input_number_content, null);
                final EditText mEditText = (EditText) contentView.findViewById(R.id.dialog_edit_text_content);

                // 获取系统保留值
                final double maxAltValue = dpPrefs.getMaxAltitude();
                final double minAltValue = dpPrefs.getMinAltitude();
                final double defaultAltValue = dpPrefs.getDefaultAltitude();
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity).setView(contentView).setNegativeButton(android.R.string.cancel, null);

                // 私有方法
                switch (view.getId()) {
                    case R.id.ViewContainer_MaxAltitude:
                        mEditText.setHint(lup.boxBaseValueToTarget(dpPrefs.getMaxAltitude()).getValue() + "");
                        alertDialog.setTitle(getString(R.string.pref_max_alt_title))
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 获得所需的Edittext控件
                                        CharSequence input = mEditText.getText();
                                        // 如果输入为空，则重新设置隐藏提示语
                                        if (TextUtils.isEmpty(input)) {
                                            input = mEditText.getHint();
                                        }
                                        try {
                                            // 实际输入值，不含单位系统
                                            final double altValue = Double.parseDouble(input.toString().trim());
                                            // 转换为带有单位系统
                                            final LengthUnit newAltValue = lup.boxTargetValue(altValue);
                                            // 单位系统转出来的数值
                                            final double altPrefValue = lup.fromTargetToBase(newAltValue).getValue();

                                            // 记录标志位
                                            boolean isValueInvalid = false;
                                            String valueUpdateMsg = "最大高度值已更新!";
                                            // Compare the new altitude value with the max altitude value
                                            if (altPrefValue < defaultAltValue) {
                                                isValueInvalid = true;
                                                valueUpdateMsg = "最大高度值不能小于默认高度值!";
                                            } else if (altPrefValue < minAltValue) {
                                                isValueInvalid = true;
                                                valueUpdateMsg = "最大高度值不能小于最小高度值!";
                                            }

                                            if (!isValueInvalid) {
                                                mEditText.setText(String.format(Locale.US, "%2.1f", newAltValue.getValue()));
                                                maxAltitudeText.setText(newAltValue.toString());
                                                // 储存到Share
                                                dpPrefs.setAltitudePreference(DroidPlannerPrefs.PREF_ALT_MAX_VALUE, (float) altPrefValue);
                                            }

                                            if (mActivity != null) {
                                                Toast.makeText(mActivity, valueUpdateMsg, Toast.LENGTH_LONG).show();
                                            }
                                        } catch (NumberFormatException e) {
                                            if (mActivity != null) {
                                                Toast.makeText(mActivity, "输入有误，请重新输入: " + mEditText.getText(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                });
                        break;

                    case R.id.ViewContainer_MinAltitude:
                        mEditText.setHint(lup.boxBaseValueToTarget(dpPrefs.getMinAltitude()).getValue() + "");
                        alertDialog.setTitle(getString(R.string.pref_min_alt_title)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 获得所需的Edittext控件
                                CharSequence input = mEditText.getText();
                                // 如果输入为空，则重新设置隐藏提示语
                                if (TextUtils.isEmpty(input)) {
                                    input = mEditText.getHint();
                                }
                                try {
                                    // 实际输入值，不含单位系统
                                    final double altValue = Double.parseDouble(input.toString().trim());
                                    // 转换为带有单位系统
                                    final LengthUnit newAltValue = lup.boxTargetValue(altValue);
                                    // 单位系统转出来的数值
                                    final double altPrefValue = lup.fromTargetToBase(newAltValue).getValue();

                                    // 记录标志位
                                    boolean isValueInvalid = false;
                                    String valueUpdateMsg = "最小高度值已更新!";
                                    // Compare the new altitude value with the max altitude value
                                    if (altPrefValue > defaultAltValue) {
                                        isValueInvalid = true;
                                        valueUpdateMsg = "最小高度值不能大于默认高度值!";
                                    } else if (altPrefValue > maxAltValue) {
                                        isValueInvalid = true;
                                        valueUpdateMsg = "最小高度值不能大于最大高度值!";
                                    }

                                    if (!isValueInvalid) {
                                        mEditText.setText(String.format(Locale.US, "%2.1f", newAltValue.getValue()));
                                        minAltitudeText.setText(newAltValue.toString());
                                        // 储存到Share
                                        dpPrefs.setAltitudePreference(DroidPlannerPrefs.PREF_ALT_MIN_VALUE, (float) altPrefValue);
                                    }

                                    if (mActivity != null) {
                                        Toast.makeText(mActivity, valueUpdateMsg, Toast.LENGTH_LONG).show();
                                    }
                                } catch (NumberFormatException e) {
                                    if (mActivity != null) {
                                        Toast.makeText(mActivity, "输入有误，请重新输入: " + mEditText.getText(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                        break;

                    case R.id.ViewContainer_DefaultAltitude:
                        mEditText.setHint(lup.boxBaseValueToTarget(dpPrefs.getDefaultAltitude()).getValue() + "");
                        alertDialog.setTitle(getString(R.string.pref_default_alt_title))
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 获得所需的Edittext控件
                                        CharSequence input = mEditText.getText();
                                        // 如果输入为空，则重新设置隐藏提示语
                                        if (TextUtils.isEmpty(input)) {
                                            input = mEditText.getHint();
                                        }
                                        try {
                                            // 实际输入值，不含单位系统
                                            final double altValue = Double.parseDouble(input.toString().trim());
                                            // 转换为带有单位系统
                                            final LengthUnit newAltValue = lup.boxTargetValue(altValue);
                                            // 单位系统转出来的数值
                                            final double altPrefValue = lup.fromTargetToBase(newAltValue).getValue();

                                            // 记录标志位
                                            boolean isValueInvalid = false;
                                            String valueUpdateMsg = "默认高度值已更新!";

                                            // Compare the new altitude value with the max altitude value
                                            if (altPrefValue > maxAltValue) {
                                                isValueInvalid = true;
                                                valueUpdateMsg = "默认高度值不能大于最大高度值";
                                            } else if (altPrefValue < minAltValue) {
                                                isValueInvalid = true;
                                                valueUpdateMsg = "默认高度值不能小于最小高度值";
                                            }

                                            if (!isValueInvalid) {
                                                mEditText.setText(String.format(Locale.US, "%2.1f", newAltValue.getValue()));
                                                defaultAltitudeText.setText(newAltValue.toString());
                                                // 储存到Share
                                                dpPrefs.setAltitudePreference(DroidPlannerPrefs.PREF_ALT_DEFAULT_VALUE, (float) altPrefValue);
                                            }

                                            if (mActivity != null) {
                                                Toast.makeText(mActivity, valueUpdateMsg, Toast.LENGTH_LONG).show();
                                            }
                                        } catch (NumberFormatException e) {
                                            if (mActivity != null) {
                                                Toast.makeText(mActivity, "输入有误，请重新输入: " + mEditText.getText(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                });
                        break;
                }
                alertDialog.create().show();
                break;

            case R.id.ViewContainer_VersionUpdate:
                Beta.checkUpgrade();
                break;

            case R.id.ViewContainer_helperDocument:
                Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT).show();
                break;

            case R.id.ViewContainer_Speech:
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title("选择发音人及语种")
                        .items(mCloudVoicersEntries)
                        .itemsCallbackSingleChoice(dpPrefs.getVoicerSelectedNum(), new ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                dpPrefs.setTTSVoicer(getResources().getStringArray(R.array.voicer_cloud_values)[which]);
                                dpPrefs.setVoicerSelectedNum(which);
                                speechTypeText.setText(mCloudVoicersEntries[dpPrefs.getVoicerSelectedNum()]);
                                EventBus.getDefault().post(ActionEvent.ACTION_UPDATE_VOICE);
                                return true;
                            }
                        })
                        .show();
                break;

            case R.id.ViewContainer_Speech_Period:
                final String[] periodic = getResources().getStringArray(R.array.tts_periodic_period);
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(R.string.pref_tts_periodic_period)
                        .items(periodic)
                        .itemsCallbackSingleChoice(dpPrefs.getSpokenStatusIntervalSelectedNum(), new ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                dpPrefs.setSpokenStatusInterval(getResources().getIntArray(R.array.tts_periodic_period_values)[which]);
                                dpPrefs.setSpokenStatusIntervalSelectedNum(which);
                                updateSpeechPeriodText();
                                EventBus.getDefault().post(ActionEvent.ACTION_UPDATED_STATUS_PERIOD);
                                return true;
                            }
                        })
                        .show();
                break;

            case R.id.ViewContainer_MapSetting:
                startActivity(new Intent(mActivity, MapPreferencesActivity.class).putExtra(
                        MapPreferencesActivity.EXTRA_MAP_PROVIDER_NAME, dpPrefs.getMapProviderName()));
                break;

            case R.id.ViewContainer_Provider:
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(getString(R.string.pref_maps_providers_title))
                        .items(DPMapProvider.高德地图.toString(), DPMapProvider.谷歌地图.toString())
                        .itemsCallbackSingleChoice(dpPrefs.getMapProvider().ordinal(), new ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                if (text.equals(DPMapProvider.谷歌地图.toString()) && !Utils.isGooglePlayServicesValid(mActivity)) {
                                    new MaterialDialog.Builder(mActivity)
                                            .iconRes(R.drawable.ic_launcher)
                                            .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                                            .title("温馨提示")
                                            .content("系统检测到你没有安装谷歌服务套件，暂时无法使用谷歌地图。\n请先下载安装谷歌服务……")
                                            .positiveText("自行安装")
                                            .show();
                                    return false;
                                }
                                dpPrefs.setMapProvider(text.toString());
                                providerNameText.setText(dpPrefs.getMapProviderName());
                                Toast.makeText(mActivity, "更改成功，重启应用以作生效!", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        })
                        .show();
                break;
        }
    }

    private void updateSpeechPeriodText() {
        int interval = dpPrefs.getSpokenStatusInterval();
        if (interval != 0)
            ttsPeriodicStatusPeriod.setText("每隔" + interval + "广播通知一遍事件更新");
        else
            ttsPeriodicStatusPeriod.setText("关闭周期性广播事件");
    }

    // 更新内容视图
    private void updateContainerView() {
        // 设置APP版本
        appVersionText.setText(Utils.getVersion(mActivity));
        // 是否使能语音
        if (dpPrefs.isTtsEnabled()) {
            ttsToggleButton.setToggleOn();
        } else {
            ttsToggleButton.setToggleOff();
        }
        // 语音类型
        speechTypeText.setText(mCloudVoicersEntries[dpPrefs.getVoicerSelectedNum()]);
        // 语音间隔
        updateSpeechPeriodText();
        // 设置地图提供商
        providerNameText.setText(dpPrefs.getMapProviderName());
        // 设置单位类型
        unitType.setSwitchState(dpPrefs.getUnitSystemType() == UnitSystem.METRIC);
        // 设置飞行器类型
        updateFirmwareVersionPreference(null);
        // 更新版本号
        updateFirmwareVersionPreference(dpApp.getDrone().getType().getFirmwareVersion());
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_DISCONNECTED:
                updateFirmwareVersionPreference(null);
                break;

            case STATE_CONNECTED:
            case TYPE_UPDATED:
                Drone drone = dpApp.getDrone();
                if (drone.isConnected()) {
                    updateFirmwareVersionPreference(drone.getType().getFirmwareVersion());
                } else
                    updateFirmwareVersionPreference(null);
                break;
        }
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_PREF_UNIT_SYSTEM_UPDATE:
                setupAltitudeTextView();
                break;
        }
    }

    private void setupAltitudeTextView() {
        maxAltitudeText.setText(getLengthUnitProvider().boxBaseValueToTarget(dpPrefs.getMaxAltitude()).toString());
        minAltitudeText.setText(getLengthUnitProvider().boxBaseValueToTarget(dpPrefs.getMinAltitude()).toString());
        defaultAltitudeText.setText(getLengthUnitProvider().boxBaseValueToTarget(dpPrefs.getDefaultAltitude()).toString());
    }

    /**
     * 更新固件型号
     */
    private void updateFirmwareVersionPreference(String firmwareVersion) {
        if (firmwareVersion == null) {
            firmwareVersionText.setText(R.string.empty_content);
        } else {
            firmwareVersionText.setText(firmwareVersion);
        }
    }

    /**
     * 获取单位提供者
     *
     * @return
     */
    protected LengthUnitProvider getLengthUnitProvider() {
        final UnitSystem unitSystem = UnitManager.getUnitSystem(mActivity.getApplicationContext());
        return unitSystem.getLengthUnitProvider();
    }
}