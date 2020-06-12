package org.farring.gcs.maps.providers.GoogleMap.offline.Utils;

import java.util.Locale;
import java.util.Random;

/**
 * Created by Fredia Huya-Kouadio on 5/11/15.
 */
public class OfflineMapUtils {

    public static final int TILE_WIDTH = 512; // pixels
    public static final int TILE_HEIGHT = 512;// pixels

    //Private constructor to prevent instantiation.
    private OfflineMapUtils() {
    }

    // 捏造Tile的URL连接
    public static String getMapTileURL(int zoom, int x, int y) {
        return String.format(Locale.US, "http://mt%d.google.cn/vt/lyrs=s&hl=zh-CN&gl=cn&scale=2&x=%d&y=%d&z=%d",
                new Random().nextInt(3), x, y, zoom);
    }
}
