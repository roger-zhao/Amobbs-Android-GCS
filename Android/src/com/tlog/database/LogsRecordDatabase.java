package com.tlog.database;

import android.content.Context;

import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_vfr_hud;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.utils.MathUtils;
import com.evenbus.ActionEvent;
import com.litesuits.orm.LiteOrm;
import com.orhanobut.logger.Logger;
import com.tlog.bmob.MD5FileUtil;
import com.tlog.helper.TLogParser;
import com.tlog.helper.TLogParser.Event;

import org.greenrobot.eventbus.EventBus;
import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.fragments.account.Model.MyUser;
import org.farring.gcs.utils.file.DirectoryPath;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import cn.bmob.v3.BmobUser;

public class LogsRecordDatabase {

    private static LiteOrm liteOrm = null;
    private static String databaseName = DirectoryPath.getTlogStringPath() + "fishDroneGCSRecord.db";

    // 获取数据库对象【单例模式】
    public synchronized static LiteOrm getLiteOrm(Context context) {
        if (liteOrm == null) {
            liteOrm = LiteOrm.newSingleInstance(context, databaseName);
        }

        return liteOrm;
    }

    /**
     * 异步存入数据库！
     *
     * @param file
     */
    public static void saveTlogToDBAsync(final File file) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    saveTlogToDB(file);
                } catch (Exception e) {
                    Logger.i(e.toString());
                }
            }
        });
    }

    /**
     * 【对外提供接口】保存日志相关信息到数据库,开启其它线程异步！
     *
     * @param file 文件
     * @return 是否保存成功?
     */
    public static void saveTlogToDB(File file) throws Exception {
        final Context context = FishDroneGCSApp.getContext();
        // 判断用户是否登陆
        MyUser currentUser = BmobUser.getCurrentUser(context, MyUser.class);
        if (currentUser == null || file == null)
            return;

        // 读取MAVLINK_MSG_ID_GLOBAL_POSITION_INT信息存放到“eventList”中
        List<Event> eventList = TLogParser.getAllEvents(file);

        // 新建数据库对象
        LogRecordBean logRecordBean = new LogRecordBean();

        // 记录开始时间
        logRecordBean.setLogStartTime(eventList.get(0).getTimestamp());
        // 记录结束时间
        logRecordBean.setLogEndTime(eventList.get(eventList.size() - 1).getTimestamp());

        // 记录临时长度
        double tempMaxHeight = 0;
        List<LatLong> flightPoints = new ArrayList<>();

        for (Event event : eventList) {
            switch (event.getMavLinkMessage().msgid) {
                case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                    msg_global_position_int msg = (msg_global_position_int) event.getMavLinkMessage();
                    // 设置纬度
                    if (msg.lat != 0 && logRecordBean.getLatitude() == 0)
                        logRecordBean.setLatitude(msg.lat / 1E7);

                    // 设置经度
                    if (msg.lon != 0 && logRecordBean.getLongitude() == 0)
                        logRecordBean.setLongitude(msg.lon / 1E7);

                    // 将所有经纬点加入列表中，用于计算距离。
                    flightPoints.add(new LatLong(msg.lat / 1E7, msg.lon / 1E7));
                    break;

                case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                    msg_vfr_hud msg_vfr_hud = (msg_vfr_hud) event.getMavLinkMessage();
                    // 计算记录中的最大高度，累加高度
                    if (tempMaxHeight < (msg_vfr_hud.alt))
                        tempMaxHeight = (msg_vfr_hud.alt);
                    break;
            }
        }

        // 日志数据有效性判断,不再记录无起飞或者只上电不飞行的日志~！根据里程或者高度值进行判断
        if (tempMaxHeight <= 0 && getFlightLength(flightPoints) <= 0)
            return;

        // 设置日期
        logRecordBean.setDate(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(eventList.get(1).getTimestamp())));
        // 文件绝对路径
        logRecordBean.setFilePath(file.getPath());
        // 总距离
        logRecordBean.setTotalDistance(getFlightLength(flightPoints));
        // 最大高度
        logRecordBean.setMaxHeight(tempMaxHeight);
        // 用户名
        logRecordBean.setFlightUserName(currentUser.getUsername());
        if (logRecordBean.getLatitude() == 0 || logRecordBean.getLongitude() == 0) {
            logRecordBean.setLocation("无位置信息");
        } else {
            // 反地理编码计算出飞行位置
            // reGeocoder(logRecordBean);
        }
        // MD5码
        logRecordBean.setFileMD5(MD5FileUtil.md5(file));

        Logger.i(logRecordBean.toString() + "数据库大小：" + liteOrm.queryCount(LogRecordBean.class));

        // 保存数据
        liteOrm.save(logRecordBean);

        EventBus.getDefault().post(ActionEvent.ACTION_TLOG_SAVE_SUCCESS);
    }

    // 获取飞行长度长度
    public static double getFlightLength(List<LatLong> points) {
        double length = 0;
        if (points.size() > 1) {
            for (int i = 1; i < points.size(); i = i + 10) {
                length += MathUtils.getDistance2D(points.get(i - 1), points.get(i));
            }
        }
        return length;
    }
}
