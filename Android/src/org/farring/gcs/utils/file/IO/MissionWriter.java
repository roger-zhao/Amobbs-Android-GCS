package org.farring.gcs.utils.file.IO;

import com.MAVLink.common.msg_mission_item;

import org.farring.gcs.utils.file.FileList;
import org.farring.gcs.utils.file.FileStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Write a mission to file.
 * <p/>
 * QGC WPL <VERSION> <INDEX> <CURRENT WP> <COORD FRAME> <COMMAND> <PARAM1>
 * <PARAM2> <PARAM3> <PARAM4> <PARAM5/X/LONGITUDE> <PARAM6/Y/LATITUDE>
 * <PARAM7/Z/ALTITUDE> <AUTOCONTINUE>
 * <p/>
 * See http://qgroundcontrol.org/mavlink/waypoint_protocol for details
 */
public class MissionWriter {
    private static final String TAG = MissionWriter.class.getSimpleName();

    public static boolean write(List<msg_mission_item> msgMissionItems) {
        return write(msgMissionItems, FileStream.getWaypointFilename("Mission"));
    }

    public static boolean write(List<msg_mission_item> msgMissionItems, String filename) {
        try {
            // 检查外部存储是否可用
            if (!FileStream.isExternalStorageAvailable())
                return false;

            if (!filename.endsWith(FileList.WAYPOINT_FILENAME_EXT)) {
                filename += FileList.WAYPOINT_FILENAME_EXT;
            }

            // 文件输出流
            final FileOutputStream out = FileStream.getWaypointFileStream(filename);
            // 写入头部
            writeHeader(out);
            // 写入任务
            writeMissionItems(out, msgMissionItems);
            // 关闭传输流
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void writeHeader(FileOutputStream out) throws IOException {
        out.write(String.format(Locale.ENGLISH, "QGC WPL 110\n").getBytes());
    }

    private static void writeMissionItems(FileOutputStream out, List<msg_mission_item> msgMissionItems) throws IOException {
        // for all msgs...
        for (int i = 0, msgMissionItemsSize = msgMissionItems.size(); i < msgMissionItemsSize; i++) {
            final msg_mission_item msg = msgMissionItems.get(i);

            // write msg (TAB delimited)
            out.write(String.format(Locale.ENGLISH,
                    "%d\t%d\t%d\t%d\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%d\n", i, i == 0 ? 1 : 0, // set CURRENT_WP = 1 for 'home' - msg[0], 0
                    // for all others
                    msg.frame, msg.command, msg.param1, msg.param2, msg.param3, msg.param4, msg.x, msg.y, msg.z, msg.autocontinue).getBytes());
        }
    }
}

