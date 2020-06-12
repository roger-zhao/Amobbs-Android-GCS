package org.farring.gcs.utils.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    public static final String CAMERA_FILENAME_EXT = ".xml";
    public static final String TLOG_FILENAME_EXT = ".tlog";
    private static final String TLOG_PREFIX = "fishDroneRecord";

    public static File[] getCameraInfoFileList() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.contains(CAMERA_FILENAME_EXT);
            }
        };
        return getFileList(DirectoryPath.getCameraInfoPath(), filter);
    }

    public static File[] getTLogFileList() {
        final FilenameFilter tlogFilter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(TLOG_FILENAME_EXT);
            }
        };

        return getFileList(DirectoryPath.getTLogPath().getPath(), tlogFilter);
    }

    public static ArrayList<String> getTLogFileListName() {
        ArrayList<String> tlogsNameArray = new ArrayList<>();

        File[] files = getFileList(DirectoryPath.getTLogPath().getPath(), new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(TLOG_FILENAME_EXT);
            }
        });

        for (File file : files)
            tlogsNameArray.add(file.getName());

        return tlogsNameArray;
    }

    static public File[] getFileList(String path, FilenameFilter filter) {
        File mPath = new File(path);
        if (!mPath.exists())
            return new File[0];

        return mPath.listFiles(filter);
    }

    /**
     * Timestamp for logs in the Mission Planner Format
     */
    static public String getTimeStamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    static public String getTimeStamp() {
        return getTimeStamp(System.currentTimeMillis());
    }

    public static File getTLogFile() {
        return new File(DirectoryPath.getTLogPath(), TLOG_PREFIX + "_[" + getTimeStamp() + "]" + FileUtils.TLOG_FILENAME_EXT);
    }

    /**
     * 根据byte数组，生成文件
     */
    public static void transFileByByteArray(byte[] bfile, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {//判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(filePath + "\\" + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
