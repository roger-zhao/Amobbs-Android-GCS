package org.farring.gcs.utils.file;

import android.os.Environment;

import java.io.File;

public class DirectoryPath {
    /**
     * Main path used to store public data files related to the program
     *
     * @return Path to Tower data directory in external storage
     */
    static public String getPublicDataPath() {
        String root = Environment.getExternalStorageDirectory().getPath();
        return (root + "/fishDroneGCS/");
    }

    /**
     * Storage folder for Parameters
     */
    static public String getParametersPath() {
        return getPublicDataPath() + "/Parameters/";
    }

    /**
     * Storage folder for mission files
     */
    static public String getWaypointsPath() {
        return getPublicDataPath() + "/Waypoints/";
    }

    /**
     * Storage folder for user map tiles
     */
    static public String getOfflineMapDBPath() {
        return getPublicDataPath() + "/OfflineMapData/";
    }

    static public String getUserInfoPath() {
        return getPublicDataPath() + "/User/";
    }

    static public String getTlogStringPath() {
        return getPublicDataPath() + "/tlogs/";
    }

    /**
     * Folder where telemetry log files are stored
     */
    static public File getTLogPath() {
        File f = new File(getPublicDataPath() + "/tlogs/");
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    /**
     * Storage folder for user camera description files
     */
    public static String getCameraInfoPath() {
        return getPublicDataPath() + "/CameraInfo/";
    }
}
