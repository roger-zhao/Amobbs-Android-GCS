package org.farring.gcs.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * Contains application related functions.
 */
public class Utils {

    public static final String PACKAGE_NAME = "org.jelsoon.android";

    public static final int MIN_DISTANCE = 0; //meter
    public static final int MAX_DISTANCE = 1000; // meters

    //Private constructor to prevent instantiation.
    private Utils() {
    }

    /**
     * Used to update the user interface language.
     *
     * @param context Application context
     */
    public static void updateUILanguage(Context context) {
        DroidPlannerPrefs prefs = DroidPlannerPrefs.getInstance(context);
        if (prefs.isEnglishDefaultLanguage()) {
            Configuration config = new Configuration();
            config.locale = Locale.ENGLISH;

            final Resources res = context.getResources();
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
    }

    public static boolean runningOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static void showDialog(DialogFragment dialog, FragmentManager fragmentManager, String tag, boolean allowStateLoss) {
        if (allowStateLoss) {
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(dialog, tag);
            transaction.commitAllowingStateLoss();
        } else {
            dialog.show(fragmentManager, tag);
        }
    }

    /**
     * Ensures that the device has the correct version of the Google Play Services.
     *
     * @return true if the Google Play Services binary is valid
     */
    public static boolean isGooglePlayServicesValid(Context context) {
        // Check for the google play services is available
        final int playStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        return playStatus == ConnectionResult.SUCCESS;
    }


    //  获取versionName方法(获取版本号)
    public static String getVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "unknow";
        }
    }

    /**
     * 判断网络连接是否有效（此时可传输数据）。
     *
     * @return boolean 不管wifi，还是mobile net，只有当前在连接状态（可有效传输数据）才返回true,反之false。
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null)
            return false;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * 返回当前屏幕是否为竖屏。
     *
     * @param context
     * @return 当且仅当当前屏幕为竖屏时返回true, 否则返回false。
     */
    public static boolean isScreenOriatationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static float dpToPx(Context context, int dp) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());

        return px;
    }
}
