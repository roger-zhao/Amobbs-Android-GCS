package org.farring.gcs.maps.providers.GoogleMap.offline.Tiles;

import android.content.Context;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.litesuits.orm.db.assit.QueryBuilder;

import org.farring.gcs.maps.providers.GoogleMap.offline.Database.OfflineDatabase;
import org.farring.gcs.maps.providers.GoogleMap.offline.Database.TileBean;
import org.farring.gcs.maps.providers.GoogleMap.offline.Utils.OfflineMapUtils;

import java.util.List;

// 接口类，为类TileOverlay提供栅格图像
public class OfflineTileProvider implements TileProvider {

    public final static int TILEURI_SUB_INDEX = 54;
    private final Context context;
    private final int maxZoomLevel;

    public OfflineTileProvider(Context context, int maxZoomLevel) {
        this.context = context;
        this.maxZoomLevel = maxZoomLevel;
    }

    // 返回指定坐标和缩放级别下的Tile对象
    @Override
    public Tile getTile(int x, int y, int zoom) {
        if (zoom > maxZoomLevel) {
            // 指定tile坐标后不存在栅格的特殊tile
            return TileProvider.NO_TILE;
        }

        // 根据指定坐标和缩放级别获取Map的URL
        final String tileUri = OfflineMapUtils.getMapTileURL(zoom, x, y);

        byte[] data = null;
        // 搜索数据库
        List<TileBean> list = OfflineDatabase.getLiteOrm(context).query(new QueryBuilder<>(TileBean.class)
                .whereEquals(TileBean.COL_URL, tileUri.substring(TILEURI_SUB_INDEX)));
        for (TileBean tileBean : list) {
            data = tileBean.data;
        }

        if (data == null || data.length == 0)
            return TileProvider.NO_TILE;

        // 包含图片瓦块信息类
        return new Tile(OfflineMapUtils.TILE_WIDTH, OfflineMapUtils.TILE_HEIGHT, data);
    }
}
