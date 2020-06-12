package org.farring.gcs.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dronekit.core.drone.DroneInterfaces;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Parameter;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.dialogs.openfile.OpenFileDialog;
import org.farring.gcs.dialogs.openfile.OpenParameterDialog;
import org.farring.gcs.dialogs.parameters.DialogParameterInfo;
import org.farring.gcs.fragments.helpers.BaseListFragment;
import org.farring.gcs.utils.file.FileStream;
import org.farring.gcs.utils.file.IO.ParameterWriter;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;
import org.farring.gcs.view.adapterViews.ParamsAdapter;
import org.farring.gcs.view.adapterViews.ParamsAdapterItem;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ParamsFragment extends BaseListFragment implements DroneInterfaces.OnParameterManagerListener {

    public static final String ADAPTER_ITEMS = ParamsFragment.class.getName() + ".adapter.items";
    public static final int SNACKBAR_HEIGHT = 48;
    private static final String PREF_PARAMS_FILTER_ON = "pref_params_filter_on";
    private static final boolean DEFAULT_PARAMS_FILTER_ON = true;
    private static final String EXTRA_OPENED_PARAMS_FILENAME = "extra_opened_params_filename";

    private ProgressDialog progressDialog;
    private SearchView searchParams;
    private ProgressBar mLoadingProgress;

    private DroidPlannerPrefs mPrefs;
    private ParamsAdapter adapter;

    /**
     * If the parameters were loaded from a file, the filename is stored here.
     */
    private String openedParamsFilename;
    private View searchButton;
    private Snackbar snackbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mPrefs = DroidPlannerPrefs.getInstance(getActivity().getApplicationContext());

        // create adapter
        if (savedInstanceState != null) {
            this.openedParamsFilename = savedInstanceState.getString(EXTRA_OPENED_PARAMS_FILENAME);

            // load adapter items
            @SuppressWarnings("unchecked")
            final ArrayList<ParamsAdapterItem> pwms = savedInstanceState.getParcelableArrayList(ADAPTER_ITEMS);
            adapter = new ParamsAdapter(getActivity(), R.layout.row_params, pwms);

        } else {
            // empty adapter
            adapter = new ParamsAdapter(getActivity(), R.layout.row_params);
        }
        setListAdapter(adapter);

        // help handler
        adapter.setOnInfoListener(new ParamsAdapter.OnInfoListener() {
            @Override
            public void onHelp(int position, EditText valueView) {
                showInfo(position, valueView);
            }
        });
        adapter.setOnParametersChangeListener(new ParamsAdapter.OnParametersChangeListener() {
            @Override
            public void onParametersChange(int dirtyCount) {
                if (dirtyCount > 0) {
                    View view = getView();
                    if (view != null && snackbar == null) {
                        snackbar = Snackbar.make(view, R.string.unsaved_param_warning, Snackbar.LENGTH_INDEFINITE)
                                .setAction(getString(R.string.upload), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        writeModifiedParametersToDrone();
                                    }
                                });
                        snackbar.show();
                    }
                } else {
                    if (snackbar != null) {
                        snackbar.dismiss();
                        snackbar = null;
                    }
                }
            }
        });
    }

    // 初始化UI
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // bind & initialize UI
        return inflater.inflate(R.layout.fragment_params, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchParams = (SearchView) view.findViewById(R.id.params_filter);
        searchParams.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                filterInput(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filterInput(s);
                return true;
            }
        });

        searchButton = searchParams.findViewById(android.support.v7.appcompat.R.id.search_button);
        searchParams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                    searchButton.performClick();
                else
                    searchButton.callOnClick();
            }
        });

        mLoadingProgress = (ProgressBar) view.findViewById(R.id.reload_progress);
        mLoadingProgress.setVisibility(View.GONE);

        View space = new View(getActivity().getApplicationContext());
        space.setLayoutParams(new AbsListView.LayoutParams(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SNACKBAR_HEIGHT, getResources().getDisplayMetrics())));
        getListView().addFooterView(space);
    }

    private void filterInput(CharSequence input) {
        if (TextUtils.isEmpty(input)) {
            adapter.getFilter().filter("");
        } else {
            adapter.getFilter().filter(input);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // 添加参数对象监听器
        getDrone().getParameterManager().setParameterListener(this);

        if (adapter.isEmpty() && getDrone().isConnected()) {
            List<Parameter> parametersList = getDrone().getParameterManager().getParametersList();
            if (!parametersList.isEmpty())
                loadAdapter(parametersList, false);
        }

        toggleParameterFilter(isParameterFilterVisible());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save adapter items
        final ArrayList<ParamsAdapterItem> pwms = new ArrayList<ParamsAdapterItem>(adapter.getOriginalValues());
        outState.putParcelableArrayList(ADAPTER_ITEMS, pwms);

        outState.putString(EXTRA_OPENED_PARAMS_FILENAME, this.openedParamsFilename);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        getActivity().getMenuInflater().inflate(R.menu.menu_parameters, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        adapter.clearFocus();
        switch (item.getItemId()) {
            case R.id.menu_download_parameters:
                refreshParameters();
                break;

            case R.id.menu_write_parameters:
                writeModifiedParametersToDrone();
                break;

            case R.id.menu_open_parameters:
                openParametersFromFile();
                break;

            case R.id.menu_save_parameters:
                saveParametersToFile();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void toggleParameterFilter(boolean isVisible) {
        if (isVisible) {
            //Show the parameter filter
            searchParams.setVisibility(View.VISIBLE);
            filterInput(searchParams.getQuery());

        } else {
            //Hide the parameter filter
            searchParams.setVisibility(View.GONE);
            filterInput(null);
        }

        mPrefs.prefs.edit().putBoolean(PREF_PARAMS_FILTER_ON, isVisible).apply();
    }

    private boolean isParameterFilterVisible() {
        return mPrefs.prefs.getBoolean(PREF_PARAMS_FILTER_ON, DEFAULT_PARAMS_FILTER_ON);
    }

    private void showInfo(int position, EditText valueView) {
        final ParamsAdapterItem item = adapter.getItem(position);
        if (!item.getParameter().hasInfo())
            return;

        DialogParameterInfo.build(item, valueView, getActivity()).show();
    }

    private void refreshParameters() {
        if (getDrone().isConnected()) {
            getDrone().getParameterManager().refreshParameters();
        } else {
            Toast.makeText(getActivity(), R.string.msg_connect_first, Toast.LENGTH_SHORT).show();
        }
    }

    private void writeModifiedParametersToDrone() {
        final Drone drone = getDrone();
        if (!drone.isConnected())
            return;

        final int adapterCount = adapter.getCount();
        List<Parameter> parametersList = new ArrayList<Parameter>(adapterCount);
        for (int i = 0; i < adapterCount; i++) {
            final ParamsAdapterItem item = adapter.getItem(i);
            if (!item.isDirty())
                continue;

            parametersList.add(item.getParameter());
            item.commit();
        }

        final int parametersCount = parametersList.size();
        if (parametersCount > 0) {
            for (Parameter proxyParam : parametersList) {
                drone.getParameterManager().sendParameter(proxyParam);
            }
            adapter.notifyDataSetChanged();
            Toast.makeText(getActivity(), parametersCount + " " + getString(R.string.msg_parameters_written_to_drone), Toast.LENGTH_SHORT).show();
        }
        snackbar = null;
    }

    private void openParametersFromFile() {
        OpenFileDialog dialog = new OpenParameterDialog() {
            @Override
            public void parameterFileLoaded(List<Parameter> parameters) {
                openedParamsFilename = getSelectedFilename();
                loadAdapter(parameters, true);
            }
        };
        dialog.openDialog(getActivity());
    }

    private void saveParametersToFile() {
        final String defaultFilename = TextUtils.isEmpty(openedParamsFilename)
                ? FileStream.getParameterFilename("Parameters-")
                : openedParamsFilename;

        new MaterialDialog.Builder(getActivity())
                .iconRes(R.drawable.ic_launcher)
                .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                .title(R.string.label_enter_filename)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(defaultFilename, "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        final List<Parameter> parameters = new ArrayList<Parameter>();
                        for (int i = 0; i < adapter.getCount(); i++) {
                            parameters.add(adapter.getItem(i).getParameter());
                        }

                        if (parameters.size() > 0) {
                            ParameterWriter parameterWriter = new ParameterWriter(parameters);
                            if (parameterWriter.saveParametersToFile(input.toString())) {
                                Toast.makeText(getActivity(), R.string.parameters_saved, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }).show();
    }

    // 装载适配器（参数列表，是否更新）
    private void loadAdapter(List<Parameter> parameters, boolean isUpdate) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        TreeMap<String, Parameter> prunedParameters = new TreeMap<String, Parameter>();
        for (Parameter parameter : parameters) {
            prunedParameters.put(parameter.getName(), parameter);
        }

        if (isUpdate) {
            adapter.updateParameters(prunedParameters);
        } else {
            adapter.loadParameters(prunedParameters);
        }

        filterInput(searchParams.getQuery());
    }

    // 开始进度
    private void startProgress() {
        final Activity activity = getActivity();
        if (activity == null)
            return;
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(R.string.refreshing_parameters);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();

        mLoadingProgress.setIndeterminate(true);
        mLoadingProgress.setVisibility(View.VISIBLE);
    }

    // 更新进度条（进度，总进度）
    private void updateProgress(int progress, int max) {
        if (progressDialog == null) {
            startProgress();
        }

        if (progressDialog.isIndeterminate()) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(max);
        }
        progressDialog.setProgress(progress);

        if (mLoadingProgress.isIndeterminate()) {
            mLoadingProgress.setIndeterminate(false);
            mLoadingProgress.setMax(max);
        }
        mLoadingProgress.setProgress(progress);
    }

    // 停止进度条
    private void stopProgress() {
        // dismiss progress dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        mLoadingProgress.setVisibility(View.GONE);
    }

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        switch (attributeEvent) {
            case STATE_CONNECTED:
            case TYPE_UPDATED:
                final Drone drone = getDrone();
                if (drone != null && drone.isConnected()) {
                    List<Parameter> parametersList = getDrone().getParameterManager().getParametersList();
                    loadAdapter(parametersList, false);
                }
                break;

            case STATE_DISCONNECTED:
                stopProgress();
                break;
        }
    }

    @Override
    public void onBeginReceivingParameters() {
        startProgress();
    }

    @Override
    public void onParameterReceived(Parameter parameter, int index, int count) {
        final int defaultValue = -1;
        if (index != defaultValue && count != defaultValue) {
            updateProgress(index, count);
        }
    }

    @Override
    public void onEndReceivingParameters() {
        stopProgress();
        loadAdapter(getDrone().getParameterManager().getParametersList(), false);
    }

    @Override
    public void onStop() {
        super.onStop();
        getDrone().getParameterManager().setParameterListener(null);
    }
}
