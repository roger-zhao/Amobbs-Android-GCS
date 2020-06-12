package com.tlog.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.evenbus.ActionEvent;
import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.orhanobut.logger.Logger;
import com.tlog.bmob.CloudSyncTlogs;
import com.tlog.bmob.CloudSyncTlogs.CloudSyncListener;
import com.tlog.database.LogRecordBean;
import com.tlog.database.LogsRecordDatabase;
import com.tubb.smrv.SwipeMenuLayout;
import com.tubb.smrv.SwipeMenuRecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.farring.gcs.R;
import org.farring.gcs.activities.interfaces.AccountLoginListener;
import org.farring.gcs.fragments.account.Model.MyUser;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.utils.Utils;
import org.farring.gcs.utils.file.FileUtils;
import org.farring.gcs.view.DividerItemDecoration;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.BmobUser;
import me.grantland.widget.AutofitTextView;

public class FlightRecordFragment extends BaseFragment {

    public static final int choseOrderTextColor = Color.rgb(100, 149, 237);

    @BindView(R.id.date_order)
    AutofitTextView dateOrder;
    @BindView(R.id.distance_order)
    AutofitTextView distanceOrder;
    @BindView(R.id.height_order)
    AutofitTextView heightOrder;
    @BindView(R.id.time_order)
    AutofitTextView timeOrder;
    @BindView(R.id.user_historyLog_list)
    SwipeMenuRecyclerView recyclerView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    private AccountLoginListener loginListener;
    private Activity mActivity;
    private LogRecordAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountLoginListener)) {
            throw new IllegalStateException("Parent must implement " + AccountLoginListener.class.getName());
        }
        this.mActivity = activity;
        this.loginListener = (AccountLoginListener) mActivity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_history_record, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                // TODO:刷新地理位置，比对数据库：如果数据库存在数据，则跳过，否则开始读取数据！先查询数据库~！刷新地理数据，须在新线程中泡，异步！
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (File file : FileUtils.getTLogFileList()) {
                                LogsRecordDatabase.saveTlogToDB(file);
                            }
                        } catch (Exception e) {
                            Logger.i(e.toString());
                        }
                    }
                });

                // 遍历完毕，刷新数据库
                refreshLogRecords(LogRecordBean.COL_DATE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Use this setting to improve performance if you know that changes in content do not change the layout side of the RecyclerView
        recyclerView.setHasFixedSize(true);
        // Use a linear layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        // 设置Item增加、移除动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        // 添加分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL_LIST));

        // 设置数据源适配器
        mAdapter = new LogRecordAdapter();
        recyclerView.setAdapter(mAdapter);
        refreshLogRecords(LogRecordBean.COL_DATE);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mActivity.getMenuInflater().inflate(R.menu.menu_droneshare_account, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final MyUser currentUser = BmobUser.getCurrentUser(mActivity, MyUser.class);
        switch (item.getItemId()) {
            // 退出登陆
            case R.id.menu_dshare_logout:
                if (currentUser != null) {
                    // 清除缓存用户对象
                    BmobUser.logOut(mActivity);
                    // 接口回调，用户登出
                    loginListener.onLogout();
                    EventBus.getDefault().post(ActionEvent.ACTION_UPDATE_USER);
                }
                break;

            case R.id.menu_cloudSync_tlogs:
                if (!Utils.isNetworkAvailable(mActivity)) {
                    Toast.makeText(mActivity, "网络不通，无法云同步飞行日志", Toast.LENGTH_SHORT).show();
                    return false;
                }

                final MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                        .progress(true, 0)
                        .iconRes(R.drawable.ic_launcher)
                        .limitIconToDefaultSize() // limits the displayed icon size to 48dp
                        .content("正在同步飞行记录……")
                        .contentGravity(GravityEnum.CENTER)
                        .canceledOnTouchOutside(false)
                        .show();

                new CloudSyncTlogs(mActivity, currentUser.getUsername(), new CloudSyncListener() {
                    @Override
                    public void onFailure(String msg) {
                        Toast.makeText(mActivity, "云同步失败:" + msg, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onRunning(int remainUploadCounts, int remainDownloadCounts) {
                        dialog.setContent("正在同步飞行记录……\n仍需上传:" + remainUploadCounts + "个日志\n仍需下载:" + remainDownloadCounts + "个文件");
                        if (remainUploadCounts == 0 && remainDownloadCounts == 0) {
                            dialog.dismiss();
                            Toast.makeText(mActivity, "云同步成功!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).cloudSync();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_TLOG_SAVE_SUCCESS:
                // 遍历完毕，刷新数据库
                refreshLogRecords(LogRecordBean.COL_DATE);
                break;
        }
        super.onReceiveActionEvent(actionEvent);
    }

    private void refreshLogRecords(final String sortType) {
        MyUser currentUser = BmobUser.getCurrentUser(mActivity, MyUser.class);
        if (currentUser != null) {
            mAdapter.updateRecyclerViewData(LogsRecordDatabase.getLiteOrm(mActivity).query(new QueryBuilder<>(LogRecordBean.class)
                    .whereEquals(LogRecordBean.COL_USERNAME, currentUser.getUsername()).appendOrderDescBy(sortType)));
        }
    }

    @OnClick({R.id.date_order, R.id.distance_order, R.id.height_order, R.id.time_order})
    public void onClick(View view) {
        distanceOrder.setTextColor(Color.WHITE);
        dateOrder.setTextColor(Color.WHITE);
        timeOrder.setTextColor(Color.WHITE);
        heightOrder.setTextColor(Color.WHITE);

        switch (view.getId()) {
            case R.id.date_order:
                dateOrder.setTextColor(choseOrderTextColor);
                refreshLogRecords(LogRecordBean.COL_DATE);
                break;

            case R.id.distance_order:
                distanceOrder.setTextColor(choseOrderTextColor);
                refreshLogRecords(LogRecordBean.COL_TOTALDISTANCE);
                break;

            case R.id.height_order:
                heightOrder.setTextColor(choseOrderTextColor);
                refreshLogRecords(LogRecordBean.COL_MAXHEIGHT);
                break;

            case R.id.time_order:
                timeOrder.setTextColor(choseOrderTextColor);
                refreshLogRecords(LogRecordBean.COL_LOGSTARTTIME);
                break;
        }
    }

    public static class MyViewHolder extends ViewHolder {
        final TextView logDate;
        final TextView flightLocation;
        final TextView flightDistance;
        final TextView flightTime;
        final TextView flightHeight;
        final View logRecordListView;
        final View btDelete;
        final SwipeMenuLayout swipeMenuLayout;

        public MyViewHolder(View itemView) {
            super(itemView);
            logRecordListView = itemView.findViewById(R.id.smContentView);

            logDate = (TextView) itemView.findViewById(R.id.history_list_date);
            flightLocation = (TextView) itemView.findViewById(R.id.history_list_location);
            flightDistance = (TextView) itemView.findViewById(R.id.history_list_distance);
            flightTime = (TextView) itemView.findViewById(R.id.history_list_use_time);
            flightHeight = (TextView) itemView.findViewById(R.id.history_list_height);

            btDelete = itemView.findViewById(R.id.btDelete);
            swipeMenuLayout = (SwipeMenuLayout) itemView.findViewById(R.id.swipeMenuLayout);
        }
    }

    public class LogRecordAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private List<LogRecordBean> logRecordBeanList;

        public void updateRecyclerViewData(List<LogRecordBean> logRecordBeanList) {
            this.logRecordBeanList = logRecordBeanList;
            notifyDataSetChanged();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_nest, parent, false));
        }

        @Override
        public void onBindViewHolder(final MyViewHolder myViewHolder, final int position) {
            final LogRecordBean logRecord = logRecordBeanList.get(position);
            final LiteOrm liteOrm = LogsRecordDatabase.getLiteOrm(mActivity);

            // 点击
            myViewHolder.logRecordListView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 检测文件是否存在
                    if (new File(logRecord.getFilePath()).exists()) {
                        Intent intent = new Intent(mActivity, HistoryActivity.class);
                        intent.putExtra("data", logRecord);
                        startActivity(intent);
                    } else {
                        Toast.makeText(mActivity, "日志文件不存在，无法打开历史回放", Toast.LENGTH_SHORT).show();
                        // 删除本地数据库记录
                        liteOrm.delete(logRecord);
                        logRecordBeanList.remove(myViewHolder.getAdapterPosition());
                        mAdapter.notifyItemRemoved(myViewHolder.getAdapterPosition());
                    }
                }
            });

            // 删除实体
            myViewHolder.btDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 1.删除本地文件
                    File file = new File(logRecord.getFilePath()); // 输入要删除的文件位置
                    if (file.exists())
                        file.delete();

                    // 2.删除本地数据库中的记录
                    liteOrm.delete(logRecord);
                    logRecordBeanList.remove(myViewHolder.getAdapterPosition());
                    mAdapter.notifyItemRemoved(myViewHolder.getAdapterPosition());
                }
            });

            // 设置日期
            myViewHolder.logDate.setText(logRecord.getDate());
            // 设置距离
            myViewHolder.flightDistance.setText(
                    getLengthUnitProvider().boxBaseValueToTarget(logRecord.getTotalDistance()).toString());
            // 设置高度
            myViewHolder.flightHeight.setText(
                    getLengthUnitProvider().boxBaseValueToTarget(logRecord.getMaxHeight()).toString());

            // 计算时间差
            Long diffTime = logRecord.getLogEndTime() - logRecord.getLogStartTime();
            long diffSeconds = diffTime / 1000 % 60;
            long diffMinutes = diffTime / (60 * 1000) % 60;

            myViewHolder.flightTime.setText(diffMinutes + "分" + diffSeconds + "秒");

            // 设置位置信息
            myViewHolder.flightLocation.setText(logRecord.getLocation());

            /**
             * optional
             */
            myViewHolder.swipeMenuLayout.setOpenInterpolator(recyclerView.getOpenInterpolator());
            myViewHolder.swipeMenuLayout.setCloseInterpolator(recyclerView.getCloseInterpolator());
        }

        @Override
        public int getItemCount() {
            return logRecordBeanList.size();
        }
    }
}
