package com.dronekit.utils;

import android.util.Log;

import com.dronekit.core.model.Logger;
import com.tencent.bugly.crashreport.BuglyLog;

/**
 * Android specific implementation for the {org.droidplanner.services.android.core.model.Logger} interface.
 */
public class AndroidLogger implements Logger {

    private static Logger sLogger = new AndroidLogger();

    // Only one instance is allowed.
    private AndroidLogger() {
    }

    public static Logger getLogger() {
        return sLogger;
    }

    @Override
    public void logVerbose(String logTag, String verbose) {
        if (verbose != null) {
            // BuglyLog.v(logTag, verbose);
        }
    }

    @Override
    public void logDebug(String logTag, String debug) {
        if (debug != null) {
            // BuglyLog.d(logTag, debug);
        }
    }

    @Override
    public void logInfo(String logTag, String info) {
        if (info != null) {
            // BuglyLog.i(logTag, info);
        }
    }

    @Override
    public void logWarning(String logTag, String warning) {
        if (warning != null) {
            // BuglyLog.w(logTag, warning);
        }
    }

    @Override
    public void logWarning(String logTag, Exception exception) {
        if (exception != null) {
            Log.w(logTag, exception);
        }
    }

    @Override
    public void logWarning(String logTag, String warning, Exception exception) {
        if (warning != null && exception != null) {
            Log.w(logTag, warning, exception);
        }
    }

    @Override
    public void logErr(String logTag, String err) {
        if (err != null) {
            // BuglyLog.e(logTag, err);
        }
    }

    @Override
    public void logErr(String logTag, Exception exception) {
        if (exception != null) {
            // BuglyLog.e(logTag, exception.getMessage(), exception);
        }
    }

    @Override
    public void logErr(String logTag, String err, Exception exception) {
        if (err != null && exception != null) {
            // BuglyLog.e(logTag, err, exception);
        }
    }
}
