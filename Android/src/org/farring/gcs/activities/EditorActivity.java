package org.farring.gcs.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.mission.MissionItemType;
import com.evenbus.ActionEvent;
import com.evenbus.AttributeEvent;

import org.beyene.sius.unit.length.LengthUnit;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.activities.interfaces.OnEditorInteraction;
import org.farring.gcs.dialogs.openfile.OpenFileDialog;
import org.farring.gcs.dialogs.openfile.OpenMissionDialog;
import org.farring.gcs.fragments.EditorListFragment;
import org.farring.gcs.fragments.EditorMapFragment;
import org.farring.gcs.fragments.WidgetsListFragment;
import org.farring.gcs.fragments.actionbar.ActionBarTelemFragment;
import org.farring.gcs.fragments.control.EditCopterFlightControlFragment;
import org.farring.gcs.fragments.editor.EditorToolsFragment;
import org.farring.gcs.fragments.editor.EditorToolsFragment.EditorTools;
import org.farring.gcs.fragments.editor.EditorToolsImpl;
import org.farring.gcs.fragments.helpers.GestureMapFragment;
import org.farring.gcs.fragments.helpers.GestureMapFragment.OnPathFinishedListener;
import org.farring.gcs.fragments.setting.SettingEditMainFragment;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.proxy.mission.MissionSelection;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;
import org.farring.gcs.proxy.mission.item.fragments.MissionDetailFragment;
import org.farring.gcs.utils.file.FileStream;
import org.farring.gcs.utils.file.IO.MissionReader;
import org.farring.gcs.utils.prefs.AutoPanMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This implements the map editor activity. The map editor activity allows the
 * user to create and/or modify autonomous missions for the drone.
 */
public class EditorActivity extends SuperUI implements OnPathFinishedListener,
        EditorToolsFragment.EditorToolListener, MissionDetailFragment.OnMissionDetailListener,
        OnEditorInteraction, MissionSelection.OnSelectionUpdateListener, OnClickListener,
        OnLongClickListener {

    private static final double DEFAULT_SPEED = 5; //meters per second.

    /**
     * Used to retrieve the item detail window when the activity is destroyed, and recreated.
     */
    private static final String ITEM_DETAIL_TAG = "Item Detail Window";

    private static final String EXTRA_OPENED_MISSION_FILENAME = "extra_opened_mission_filename";

    /**
     * Used to provide access and interact with the {@link org.farring.gcs.proxy.mission.MissionProxy} object on the Android layer.
     */
    private MissionProxy missionProxy;
    /*
     * View widgets.
     */
    private GestureMapFragment gestureMapFragment;
    private EditorToolsFragment editorToolsFragment;
    private MissionDetailFragment itemDetailFragment;
    private FragmentManager fragmentManager;
    private TextView infoView;
    /**
     * If the mission was loaded from a file, the filename is stored here.
     */
    private String openedMissionFilename;
    private View itemDetailToggle;
    private View itemDetailToggleLeft;
    private EditorListFragment editorListFragment;
    private SettingEditMainFragment ettingEditMainFragment;
    private long exitTime = 0;
    private FrameLayout flSetting;
    private FrameLayout flInfo;
    private int enableLidarCnt = 0;
    private Button clearMissBtn; // = (Button) view.findViewById(R.id.mc_armBtn);
    private Button enableLidarBtn; // = (Button) view.findViewById(R.id.mc_armBtn);

    @Subscribe
    public void onReceiveAttributeEvent(AttributeEvent attributeEvent) {
        super.onReceiveAttributeEvent(attributeEvent);
        switch (attributeEvent) {
            case PARAMETERS_REFRESH_COMPLETED:
                updateMissionLength();
                break;

            case MISSION_RECEIVED:
                final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
                if (planningMapFragment != null) {
                    planningMapFragment.zoomToFit();
                }
                break;
        }
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_MISSION_PROXY_UPDATE:
                updateMissionLength();
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        fragmentManager = getSupportFragmentManager();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        /**
         * 地图操作
         */
        gestureMapFragment = ((GestureMapFragment) fragmentManager.findFragmentById(R.id.editor_map_fragment));
        if (gestureMapFragment == null) {
            gestureMapFragment = new GestureMapFragment();
            fragmentManager.beginTransaction().add(R.id.editor_map_fragment, gestureMapFragment).commit();
        }
        /**
         * 信息显示
         */
        WidgetsListFragment widgetsListFragment = (WidgetsListFragment) fragmentManager.findFragmentById(R.id.flight_widgets_container);
        if (widgetsListFragment == null) {
            widgetsListFragment = new WidgetsListFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.flight_widgets_container, widgetsListFragment)
                    .commit();
        }
        /**
         * 飞行器控制
         */
        EditCopterFlightControlFragment editCopterFlightControlFragment = (EditCopterFlightControlFragment) fragmentManager.findFragmentById(R.id.flight_control_container);
        if (editCopterFlightControlFragment == null) {
            editCopterFlightControlFragment = new EditCopterFlightControlFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.flight_control_container, editCopterFlightControlFragment)
                    .commit();
        }
        /**
         * 编辑工具
         */
        editorToolsFragment = (EditorToolsFragment) fragmentManager.findFragmentById(R.id.flight_tool_container);
        if (editorToolsFragment == null) {
            editorToolsFragment = new EditorToolsFragment();
            fragmentManager.beginTransaction().add(R.id.flight_tool_container, editorToolsFragment).commit();
        }
        /**
         * 设置
         */
        ettingEditMainFragment = (SettingEditMainFragment) fragmentManager.findFragmentById(R.id.editor_setting);
        if (ettingEditMainFragment == null) {
            ettingEditMainFragment = new SettingEditMainFragment();
            fragmentManager.beginTransaction().add(R.id.editor_setting, ettingEditMainFragment).commit();
        }
        flSetting = (FrameLayout) findViewById(R.id.editor_setting);
        flInfo = (FrameLayout) findViewById(R.id.flight_widgets_container);

        editorListFragment = (EditorListFragment) fragmentManager.findFragmentById(R.id.mission_list_fragment);

        infoView = (TextView) findViewById(R.id.editorInfoWindow);

        final FloatingActionButton zoomToFit = (FloatingActionButton) findViewById(R.id.zoom_to_fit_button);
        zoomToFit.setVisibility(View.INVISIBLE);
        zoomToFit.setOnClickListener(this);

        final FloatingActionButton mGoToMyLocation = (FloatingActionButton) findViewById(R.id.my_location_button);
        mGoToMyLocation.setOnClickListener(this);
        mGoToMyLocation.setOnLongClickListener(this);

        final FloatingActionButton mGoToDroneLocation = (FloatingActionButton) findViewById(R.id.drone_location_button);
        mGoToDroneLocation.setOnClickListener(this);
        mGoToDroneLocation.setOnLongClickListener(this);

        enableLidarBtn = (Button) findViewById(R.id.lidar_enable_button);
        enableLidarBtn.setOnClickListener(this);

        clearMissBtn = (Button) findViewById(R.id.clear_mission_button);
        clearMissBtn.setOnClickListener(this);

        itemDetailToggle =  findViewById(R.id.toggle_action_drawer);
        itemDetailToggle.setOnClickListener(this);
        itemDetailToggleLeft = findViewById(R.id.toggle_action_drawer_left);
        itemDetailToggleLeft.setOnClickListener(this);

        if (savedInstanceState != null) {
            openedMissionFilename = savedInstanceState.getString(EXTRA_OPENED_MISSION_FILENAME);
        }

        // Retrieve the item detail fragment using its tag
        itemDetailFragment = (MissionDetailFragment) fragmentManager.findFragmentByTag(ITEM_DETAIL_TAG);

        gestureMapFragment.setOnPathFinishedListener(this);

        View btnEdit = findViewById(R.id.editor_edit_btn);
        btnEdit.setOnClickListener(this);
        View btnSetting = findViewById(R.id.editor_setting_btn);
        btnSetting.setOnClickListener(this);
    }


    /**
     * Account for the various ui elements and update the map padding so that it remains 'visible'.
     */
    private void updateLocationButtonsMargin(boolean isOpened) {

        itemDetailToggle.setActivated(isOpened);
    }

    @Override
    public void onStart() {
        super.onStart();
        missionProxy = dpApp.getMissionProxy();
        if (missionProxy != null) {
            missionProxy.selection.addSelectionUpdateListener(this);
            itemDetailToggle.setVisibility(missionProxy.selection.getSelected().isEmpty() ? View.GONE : View.VISIBLE);
        }

        updateMissionLength();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (missionProxy != null)
            missionProxy.selection.removeSelectionUpdateListener(this);
    }

    @Override
    public void onClick(View v) {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        switch (v.getId()) {
            case R.id.toggle_action_drawer:
                if (missionProxy == null)
                    return;

                if (itemDetailFragment == null) {
                    List<MissionItemProxy> selected = missionProxy.selection.getSelected();
                    showItemDetail(selectMissionDetailType(selected));
                } else {
                    removeItemDetail();
                }
                break;
            case R.id.toggle_action_drawer_left:
                if (flInfo.isShown()) {
                    flInfo.setVisibility(View.GONE);
                } else {
                    flInfo.setVisibility(View.VISIBLE);
                }

                break;

            case R.id.zoom_to_fit_button:
                if (planningMapFragment != null) {
                    planningMapFragment.zoomToFit();
                }
                break;

            case R.id.drone_location_button:
                planningMapFragment.goToDroneLocation();
                break;
            case R.id.my_location_button:
                planningMapFragment.goToMyLocation();
                break;
            case R.id.editor_edit_btn:
                flSetting.setVisibility(View.GONE);
                break;
            case R.id.editor_setting_btn:
                flSetting.setVisibility(View.VISIBLE);
                break;
            case R.id.clear_mission_button:
                missionProxy.clear();
                break;
            case R.id.lidar_enable_button:
                if((enableLidarCnt & 1) == 1)
                {
                    enableLidar(true);
                }
                else
                {
                    enableLidar(false);
                }
                enableLidarCnt++;
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();

        switch (view.getId()) {
            case R.id.drone_location_button:
                planningMapFragment.setAutoPanMode(AutoPanMode.DRONE);
                return true;
            case R.id.my_location_button:
                planningMapFragment.setAutoPanMode(AutoPanMode.USER);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        editorToolsFragment.setToolAndUpdateView(getTool());
        setupTool();
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_container;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_OPENED_MISSION_FILENAME, openedMissionFilename);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_mission, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_open_mission:
                openMissionFile();
                return true;

            case R.id.menu_save_mission:
                saveMissionFile();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openMissionFile() {
        OpenFileDialog missionDialog = new OpenMissionDialog() {
            @Override
            public void waypointFileLoaded(MissionReader reader) {
                openedMissionFilename = getSelectedFilename();

                if (missionProxy != null) {
                    missionProxy.readMissionFromFile(reader);
                    gestureMapFragment.getMapFragment().zoomToFit();
                }
            }
        };
        missionDialog.openDialog(this);
    }

    private void saveMissionFile() {
        final String defaultFilename = TextUtils.isEmpty(openedMissionFilename)
                ? FileStream.getWaypointFilename("Mission")
                : openedMissionFilename;

        new MaterialDialog.Builder(this)
                .iconRes(R.drawable.ic_launcher)
                .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                .title(R.string.label_enter_filename)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(defaultFilename, "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        if (missionProxy.writeMissionToFile(input.toString())) {
                            Toast.makeText(EditorActivity.this, R.string.file_saved_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EditorActivity.this, R.string.file_saved_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        gestureMapFragment.getMapFragment().saveCameraPosition();
    }

    private void updateMissionLength() {
        if (missionProxy != null) {

            double missionLength = missionProxy.getMissionLength();
            LengthUnit convertedMissionLength = unitSystem.getLengthUnitProvider().boxBaseValueToTarget(missionLength);
            double speedParameter = dpApp.getDrone().getMission().getSpeedParameter();
            if (speedParameter <= 0)
                speedParameter = DEFAULT_SPEED;

            int time = (int) (missionLength / speedParameter);

            String infoString = getString(R.string.editor_info_window_distance, convertedMissionLength.toString())
                    + ", " + getString(R.string.editor_info_window_flight_time, time / 60, time % 60);

            infoView.setText(infoString);

            // Remove detail window if item is removed
            if (missionProxy.selection.getSelected().isEmpty() && itemDetailFragment != null) {
                removeItemDetail();
            }
        }
    }

    private void enableLidar(boolean enable) {
        if (dpApp.getDrone().isConnected()) {

            double missionLength = missionProxy.getMissionLength();
            LengthUnit convertedMissionLength = unitSystem.getLengthUnitProvider().boxBaseValueToTarget(missionLength);
            ParameterManager parameterManager = dpApp.getDrone().getParameterManager();
            Parameter lidarSeriaPort = parameterManager.getParameter("SERIAL2_PROTOCOL");
            lidarSeriaPort.setValue(enable?9:0); // 9: lidar type
            parameterManager.sendParameter(lidarSeriaPort);
            if(enable)
            {
                this.enableLidarBtn.setText("雷达开启");
                Toast.makeText(getApplicationContext(), "雷达开启", Toast.LENGTH_SHORT).show();
                EventBus.getDefault().post(AttributeEvent.LIDAR_ENABLED);

            }
            else
            {
                this.enableLidarBtn.setText("雷达关闭");
                Toast.makeText(getApplicationContext(), "雷达关闭", Toast.LENGTH_SHORT).show();
                EventBus.getDefault().post(AttributeEvent.LIDAR_DISABLED);
            }

        }

        // for test with autoWP
        // List<LatLong> pointsTest = new ArrayList<LatLong>();
        // pointsTest.add(new LatLong(85.009446, -179.960814));
        // pointsTest.add(new LatLong(85.00922,  -179.961866));
        // pointsTest.add(new LatLong(85.009171, -179.95805));
        // pointsTest.add(new LatLong(85.00935,  -179.958035));
        // pointsTest.add(new LatLong(85.00945,  -179.960919));
//
        // EditorToolsImpl toolImpl = getDrawToolsImpl();
        // toolImpl.onPathFinished(pointsTest);
    }

    @Override
    public void onMapClick(LatLong point) {
        EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.onMapClick(point);
    }

    public EditorTools getTool() {
        return editorToolsFragment.getTool();
    }

    public EditorToolsImpl getToolImpl() {
        return editorToolsFragment.getToolImpl();
    }

    public EditorToolsImpl getDrawToolsImpl() {
        return editorToolsFragment.getDrawToolsImpl();
    }

    @Override
    public void editorToolChanged(EditorTools tools) {
        setupTool();
    }

    @Override
    public void enableGestureDetection(boolean enable) {
        if (gestureMapFragment == null)
            return;

        if (enable)
            gestureMapFragment.enableGestureDetection();
        else
            gestureMapFragment.disableGestureDetection();
    }

    @Override
    public void skipMarkerClickEvents(boolean skip) {
        if (gestureMapFragment == null)
            return;

        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        if (planningMapFragment != null)
            planningMapFragment.skipMarkerClickEvents(skip);
    }

    private void setupTool() {
        final EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.setup();
        editorListFragment.enableDeleteMode(toolImpl.getEditorTools() == EditorTools.TRASH);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        updateLocationButtonsMargin(itemDetailFragment != null);
    }

    @Override
    protected void addToolbarFragment() {

        final FragmentManager fm = getSupportFragmentManager();
        Fragment actionBarTelem = fm.findFragmentById(R.id.editor_status_details);
        if (actionBarTelem == null) {
            actionBarTelem = new ActionBarTelemFragment();
            fm.beginTransaction().add(R.id.editor_status_details, actionBarTelem).commit();
        }
    }

    private void showItemDetail(MissionDetailFragment itemDetail) {
        if (itemDetailFragment == null) {
            addItemDetail(itemDetail);
        } else {
            switchItemDetail(itemDetail);
        }

        editorToolsFragment.setToolAndUpdateView(EditorTools.NONE);
    }

    private void addItemDetail(MissionDetailFragment itemDetail) {
        itemDetailFragment = itemDetail;
        if (itemDetailFragment == null)
            return;

        fragmentManager.beginTransaction()
                .replace(R.id.editor_map_details, itemDetailFragment, ITEM_DETAIL_TAG)
                .commit();
        updateLocationButtonsMargin(true);
    }

    public void switchItemDetail(MissionDetailFragment itemDetail) {
        removeItemDetail();
        addItemDetail(itemDetail);
    }

    private void removeItemDetail() {
        if (itemDetailFragment != null) {
            fragmentManager.beginTransaction().remove(itemDetailFragment).commit();
            itemDetailFragment = null;

            updateLocationButtonsMargin(false);
        }
    }

    @Override
    public void onPathFinished(List<LatLong> path) {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        List<LatLong> points = planningMapFragment.projectPathIntoMap(path);
        EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.onPathFinished(points);
    }

    @Override
    public void onDetailDialogDismissed(List<MissionItemProxy> itemList) {
        if (missionProxy != null) missionProxy.selection.removeItemsFromSelection(itemList);
    }

    @Override
    public void onWaypointTypeChanged(MissionItemType newType, List<Pair<MissionItemProxy, List<MissionItemProxy>>> oldNewItemsList) {
        missionProxy.replaceAll(oldNewItemsList);
    }

    private MissionDetailFragment selectMissionDetailType(List<MissionItemProxy> proxies) {
        if (proxies == null || proxies.isEmpty())
            return null;

        MissionItemType referenceType = null;
        for (MissionItemProxy proxy : proxies) {
            final MissionItemType proxyType = proxy.getMissionItem().getType();
            if (referenceType == null) {
                referenceType = proxyType;
            } else if (referenceType != proxyType
                    || MissionDetailFragment.typeWithNoMultiEditSupport.contains(referenceType)) {
                //Return a generic mission detail.
                return new MissionDetailFragment();
            }
        }

        return MissionDetailFragment.newInstance(referenceType);
    }

    @Override
    public void onItemClick(MissionItemProxy item, boolean zoomToFit) {
        if (missionProxy == null) return;

        EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.onListItemClick(item);

        if (zoomToFit) {
            zoomToFitSelected();
        }
    }

    @Override
    public void zoomToFitSelected() {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        List<MissionItemProxy> selected = missionProxy.selection.getSelected();
        if (selected.isEmpty()) {
            planningMapFragment.zoomToFit();
        } else {
            planningMapFragment.zoomToFit(MissionProxy.getVisibleCoords(selected));
        }
    }

    @Override
    public void onListVisibilityChanged() {
    }

    @Override
    protected boolean enableMissionMenus() {
        return true;
    }

    @Override
    public void onSelectionUpdate(List<MissionItemProxy> selected) {
        EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.onSelectionUpdate(selected);

        final boolean isEmpty = selected.isEmpty();

        if (isEmpty) {
            itemDetailToggle.setVisibility(View.GONE);
            removeItemDetail();
        } else {
            itemDetailToggle.setVisibility(View.VISIBLE);
            if (getTool() == EditorTools.SELECTOR)
                removeItemDetail();
            else {
                showItemDetail(selectMissionDetailType(selected));
            }
        }

        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        if (planningMapFragment != null)
            planningMapFragment.postUpdate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出该程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
