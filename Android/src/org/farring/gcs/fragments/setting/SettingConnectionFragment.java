package org.farring.gcs.fragments.setting;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.ListCallbackSingleChoice;
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback;

import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.grantland.widget.AutofitTextView;

/**
 * 作者： 林杰强
 * 日期： 2016/2/11 10:29.
 * 备注:
 */
public class SettingConnectionFragment extends BaseFragment {

    @BindView(R.id.pref_udp_server_port)
    TextView prefUdpServerPort;
    @BindView(R.id.pref_bluetooth_device_address)
    TextView prefBluetoothDeviceAddress;
    @BindView(R.id.pref_connection_param_type)
    TextView prefConnectionParamType;
    @BindView(R.id.pref_server)
    AutofitTextView prefServerInfo;
    @BindView(R.id.pref_baud_type)
    TextView prefBaudType;

    private DroidPlannerPrefs dpPrefs;

    private EditText serverIpView;
    private EditText serverPortView;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting_connection, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateSettingView();
    }

    private int getUsbBaudIndex() {
        switch (dpPrefs.getUsbBaudRate()) {
            case 38400:
                return 0;

            case 115200:
                return 2;

            default:
            case 57600:
                return 1;
        }
    }

    private void updateSettingView() {
        prefConnectionParamType.setText(getResources().getStringArray(R.array.TelemetryConnectionTypes)[dpPrefs.getConnectionParameterType()]);
        prefBaudType.setText(dpPrefs.getUsbBaudRate() + "");
        prefServerInfo.setText(String.format("服务器地址:%s;端口号:%d", dpPrefs.getTcpServerIp(), dpPrefs.getTcpServerPort()));
        prefUdpServerPort.setText(dpPrefs.getUdpServerPort() + "");
        prefBluetoothDeviceAddress.setText(String.format("%s---%s", dpPrefs.getBluetoothDeviceName(), dpPrefs.getBluetoothDeviceAddress()));
    }

    @OnClick({R.id.ViewContainer_ConnectionType, R.id.ViewContainer_UdpPort, R.id.ViewContainer_UsbBaud,
            R.id.ViewContainer_bluetooth, R.id.ViewContainer_TcpConnection})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ViewContainer_ConnectionType:
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(getString(R.string.pref_connection_type))
                        .items(getResources().getStringArray(R.array.TelemetryConnectionTypes))
                        .itemsCallbackSingleChoice(dpPrefs.getConnectionParameterType(), new ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                dpPrefs.setConnectionParameterType(which);
                                prefConnectionParamType.setText(getResources().getStringArray(R.array.TelemetryConnectionTypes)[which]);
                                return true;
                            }
                        })
                        .show();
                break;

            case R.id.ViewContainer_UsbBaud:
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(getString(R.string.pref_baud_type))
                        .items(getResources().getStringArray(R.array.TelemetryBaudTypes))
                        .itemsCallbackSingleChoice(getUsbBaudIndex(), new ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                dpPrefs.setUsbBaudRate(Integer.parseInt(getResources().getStringArray(R.array.TelemetryBaudTypes)[which]));
                                prefBaudType.setText(dpPrefs.getUsbBaudRate() + "");
                                return true;
                            }
                        })
                        .show();
                break;

            case R.id.ViewContainer_TcpConnection:
                MaterialDialog dialogTcpConnection = new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(R.string.pref_server)
                        .customView(R.layout.dialog_tcp_connection, true)
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .onPositive(new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                String resultInput = serverIpView.getText().toString();
                                if (resultInput.equals(""))
                                    return;
                                dpPrefs.setTcpServerIp(resultInput);
                                try {
                                    dpPrefs.setTcpServerPort(Integer.parseInt(serverPortView.getText().toString()));
                                } catch (NumberFormatException exception) {
                                    Toast.makeText(mActivity, "输入有误，请重新输入端口号", Toast.LENGTH_LONG).show();
                                }
                                prefServerInfo.setText(String.format("服务器地址:%s；端口号:%d",
                                        dpPrefs.getTcpServerIp(), dpPrefs.getTcpServerPort()));
                            }
                        }).build();

                serverIpView = (EditText) dialogTcpConnection.getCustomView().findViewById(R.id.tcpConnectionIP);
                serverIpView.setHint(dpPrefs.getTcpServerIp());

                serverPortView = (EditText) dialogTcpConnection.getCustomView().findViewById(R.id.tcpConnectionPort);
                serverPortView.setHint(dpPrefs.getTcpServerPort() + "");
                dialogTcpConnection.show();
                break;

            case R.id.ViewContainer_UdpPort:
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(R.string.pref_udp_server_port)
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input(dpPrefs.getUdpServerPort() + "", "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                try {
                                    dpPrefs.setUdpServerPort(Integer.parseInt(input.toString()));
                                } catch (NumberFormatException exception) {
                                    Toast.makeText(mActivity, "输入有误，请重新输入端口号", Toast.LENGTH_LONG).show();
                                } finally {
                                    prefUdpServerPort.setText(dpPrefs.getUdpServerPort() + "");
                                }
                            }
                        }).show();
                break;

            case R.id.ViewContainer_bluetooth:
                new MaterialDialog.Builder(mActivity)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .title(getString(R.string.pref_forget_bluetooth_device_address))
                        .positiveText(getString(android.R.string.yes))
                        .negativeText(getString(android.R.string.no))
                        .onPositive(new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dpPrefs.setBluetoothDeviceAddress("");
                                dpPrefs.setBluetoothDeviceName("");
                                prefBluetoothDeviceAddress.setText(String.format("%s---%s",
                                        dpPrefs.getBluetoothDeviceName(), dpPrefs.getBluetoothDeviceAddress()));
                            }
                        })
                        .show();
                break;
        }
    }
}
