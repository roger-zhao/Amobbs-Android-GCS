package org.farring.gcs.utils;

import android.content.res.Resources;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.model.LatLng;
import com.dronekit.core.helpers.coordinates.LatLong;

import org.farring.gcs.FishDroneGCSApp;
import org.farring.gcs.maps.providers.GoogleMap.GoogleMapPrefConstants;
import org.farring.gcs.maps.providers.GoogleMap.GoogleMapPrefConstants.TileProvider;
import org.farring.gcs.maps.providers.GoogleMap.offline.UI.GoogleMapPrefFragment;

public class DroneMapHelper {

    // LatLng：存储经纬度坐标值的类，单位：角度
    final static double pi = 3.14159265358979324;
    final static double a = 6378245.0;
    final static double ee = 0.00669342162296594323;

    public static int scaleDpToPixels(double value, Resources res) {
        final float scale = res.getDisplayMetrics().density;
        return (int) Math.round(value * scale);
    }

    // ***************************** Google地图提供 *********************************
    // 地图是否为原生地图（使用纠偏技术）
    private static boolean isGoogleOriginalMap() {
        final @TileProvider String tileProvider = GoogleMapPrefFragment.PrefManager.getMapTileProvider(FishDroneGCSApp.getContext());
        switch (tileProvider) {
            case GoogleMapPrefConstants.MAPBOX_TILE_PROVIDER:
                return false;

            default:
            case GoogleMapPrefConstants.GOOGLE_TILE_PROVIDER:
                return true;
        }
    }

    public static com.google.android.gms.maps.model.LatLng CoordToLatLang(LatLong coord) {
        if (isGoogleOriginalMap()) {
            return new com.google.android.gms.maps.model.LatLng(coord.getLatitude(), coord.getLongitude());
        } else {
            double d[] = new double[2];
            transform(coord.getLatitude(), coord.getLongitude(), d);
            return new com.google.android.gms.maps.model.LatLng(d[0], d[1]);
        }
    }

    public static LatLong LatLngToCoord(com.google.android.gms.maps.model.LatLng point) {
        if (isGoogleOriginalMap()) {
            return new LatLong((float) point.latitude, (float) point.longitude);
        } else {
            double d[] = new double[2];
            untransform(point.latitude, point.longitude, d);
            return new LatLong(d[0], d[1]);
        }
    }

    // ************************* 高德地图提供 ***********************
    public static LatLng CoordToGaodeLatLang(LatLong coord) {
        double d[] = new double[2];
        transform(coord.getLatitude(), coord.getLongitude(), d);
        return new LatLng(d[0], d[1]);
    }

    public static LatLong GaodeLatLngToCoord(LatLng point) {
        double d[] = new double[2];
        untransform(point.latitude, point.longitude, d);
        return new LatLong(d[0], d[1]);
    }

    // ************************* 高德定位提供 ***********************
    public static LatLong AMapLocationToCoord(AMapLocation location) {
        double d[] = new double[2];
        untransform(location.getLatitude(), location.getLongitude(), d);
        return new LatLong(d[0], d[1]);
    }

    // ******************** 转换算法 ****************************
    // World Geodetic System ==> Mars Geodetic System 标准坐标系 ---> 火星坐标系  WGS84转GCj02
    public static void transform(double wgLat, double wgLon, double[] latlng) {
        if (outOfChina(wgLat, wgLon)) {
            latlng[0] = wgLat;
            latlng[1] = wgLon;
            return;
        }

        double dLat = transformLat(wgLon - 105.0, wgLat - 35.0);
        double dLon = transformLon(wgLon - 105.0, wgLat - 35.0);
        double radLat = wgLat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        latlng[0] = wgLat + dLat;
        latlng[1] = wgLon + dLon;
    }

    /**
     * GCJ02 转换为 WGS84
     *
     * @param wgLat
     * @param wgLon
     * @returns {*[]}
     */
    public static void untransform(double wgLat, double wgLon, double[] latlng) {
        double d[] = new double[2];
        transform(wgLat, wgLon, d);
        latlng[0] = wgLat + (wgLat - d[0]);
        latlng[1] = wgLon + (wgLon - d[1]);
    }

    /**
     * 判断是否在国内，不在国内则不做偏移
     * 定位SDK在大陆、香港、澳门返回gcj02坐标；台湾、海外返回原始wgs84坐标。
     *
     * @param lat
     * @param lon
     * @returns {boolean}
     */
    private static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        return lat < 0.8293 || lat > 55.8271;
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }
}

/*
【总结】
    1.WGS84坐标系：即地球坐标系，国际上通用的坐标系。

    2.GCJ02坐标系：即火星坐标系，WGS84坐标系经加密后的坐标系。

    3.BD09坐标系：即百度坐标系，GCJ02坐标系经加密后的坐标系。

    4.搜狗坐标系、图吧坐标系等，估计也是在GCJ02基础上加密而成的。
 */