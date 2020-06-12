package com.tlog.database;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.NotNull;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.annotation.Unique;
import com.litesuits.orm.db.enums.AssignType;

import java.io.Serializable;

@Table("tLogsData")
public class LogRecordBean implements Serializable {

    public final static String COL_DATE = "_date";
    public final static String COL_LONGITUDE = "_longitude";
    public final static String COL_LATITUDE = "_latitude";
    public final static String COL_MAXHEIGHT = "_maxHeight";
    public final static String COL_LOCATION = "_location";
    public final static String COL_TOTALDISTANCE = "_totalDistance";
    public final static String COL_FILEPATH = "_filePath";
    public final static String COL_USERNAME = "_userName";
    public final static String COL_FILEMD5 = "_fileMD5";
    public final static String COL_LOGSTARTTIME = "_logStartTime";
    public final static String COL_LOGENDTIME = "_logEndTime";

    // 飞行时间
    @Column(COL_DATE)
    @NotNull
    private String date;

    // 经度
    @Column(COL_LONGITUDE)
    private double longitude;

    // 纬度
    @Column(COL_LATITUDE)
    private double latitude;

    // 飞行最大高度
    @Column(COL_MAXHEIGHT)
    private double maxHeight;

    // 位置描述
    @Column(COL_LOCATION)
    private String location;

    // 飞行总距离(里程)
    @Column(COL_TOTALDISTANCE)
    private double totalDistance;

    // 飞行开始时间
    @Column(COL_LOGSTARTTIME)
    private long logStartTime;

    // 飞行结束时间
    @Column(COL_LOGENDTIME)
    private long logEndTime;

    // tlog存放路径（绝对路径）
    @NotNull
    @Column(COL_FILEPATH)
    @Unique
    private String filePath;

    // tlog文件的MD5码，用于校验【主键，唯一值】
    @NotNull
    @PrimaryKey(AssignType.BY_MYSELF)
    @Column(COL_FILEMD5)
    @Unique
    private String fileMD5;

    // 用户名字
    @NotNull
    @Column(COL_USERNAME)
    private String flightUserName;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public long getLogEndTime() {
        return logEndTime;
    }

    public void setLogEndTime(long logEndTime) {
        this.logEndTime = logEndTime;
    }

    public long getLogStartTime() {
        return logStartTime;
    }

    public void setLogStartTime(long logStartTime) {
        this.logStartTime = logStartTime;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFlightUserName() {
        return flightUserName;
    }

    public void setFlightUserName(String flightUserName) {
        this.flightUserName = flightUserName;
    }

    public String getFileMD5() {
        return fileMD5;
    }

    public void setFileMD5(String fileMD5) {
        this.fileMD5 = fileMD5;
    }

    @Override
    public String toString() {
        return "LogRecordBean{" +
                "date='" + date + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", maxHeight=" + maxHeight +
                ", location='" + location + '\'' +
                ", totalDistance=" + totalDistance +
                ", logStartTime=" + logStartTime +
                ", logEndTime=" + logEndTime +
                ", filePath='" + filePath + '\'' +
                ", fileMD5='" + fileMD5 + '\'' +
                ", flightUserName='" + flightUserName + '\'' +
                '}';
    }
}
