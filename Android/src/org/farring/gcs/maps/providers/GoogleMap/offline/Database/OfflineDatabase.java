package org.farring.gcs.maps.providers.GoogleMap.offline.Database;

import android.content.Context;
import android.widget.Toast;

import com.litesuits.orm.LiteOrm;

import org.farring.gcs.utils.file.DirectoryPath;

public class OfflineDatabase {

    private static LiteOrm liteOrm = null;
    private static String databaseName = DirectoryPath.getOfflineMapDBPath() + "offlineGoogleMap.db";

    // 获取数据库对象【单例模式】
    public synchronized static LiteOrm getLiteOrm(Context context) {
        if (liteOrm == null) {
            liteOrm = LiteOrm.newSingleInstance(context, databaseName);
        }

        return liteOrm;
    }

    public synchronized static void deleteDataBase(Context context) {
        if (liteOrm != null) {
            liteOrm.deleteAll(TileBean.class);
            if (liteOrm.deleteDatabase())
                Toast.makeText(context, "删除离线数据成功！", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "删除离线数据失败！", Toast.LENGTH_LONG).show();

            // 清空引用，垃圾回收！
            liteOrm = null;
        }
    }
}
