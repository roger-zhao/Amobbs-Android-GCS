package org.farring.gcs.utils.file;

import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileStream {

    public static FileOutputStream getParameterFileStream(String filename) throws FileNotFoundException {
        File myDir = new File(DirectoryPath.getParametersPath());
        myDir.mkdirs();
        File file = new File(myDir, filename);
        if (file.exists())
            file.delete();
        FileOutputStream out = new FileOutputStream(file);
        return out;
    }

    public static String getParameterFilename(String prefix) {
        return prefix + "-" + getTimeStamp() + FileList.PARAM_FILENAME_EXT;
    }

    static public FileOutputStream getWaypointFileStream(String filename) throws FileNotFoundException {
        File myDir = new File(DirectoryPath.getWaypointsPath());
        myDir.mkdirs();
        File file = new File(myDir, filename);
        if (file.exists())
            file.delete();
        return new FileOutputStream(file);
    }

    public static String getWaypointFilename(String prefix) {
        return prefix + "-" + getTimeStamp() + FileList.WAYPOINT_FILENAME_EXT;
    }

    /**
     * Timestamp for logs in the Mission Planner Format
     */
    static public String getTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss", Locale.US);
        return sdf.format(new Date());
    }

    public static boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // 获取文件名，并转URI传输
    public static Uri getOutputMediaFileUri() {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
        File mediaFile = new File(DirectoryPath.getUserInfoPath() + "IMG_" + timeStamp + ".jpg");
        return Uri.fromFile(mediaFile);
    }
}
