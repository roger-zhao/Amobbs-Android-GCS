package org.farring.gcs.maps.providers.GoogleMap.offline.Tiles;

import android.util.Log;

import com.google.android.gms.maps.model.UrlTileProvider;

import org.farring.gcs.maps.providers.GoogleMap.offline.Utils.OfflineMapUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class OnlineTileProvider extends UrlTileProvider {

    private final static String TAG = OnlineTileProvider.class.getSimpleName();

    private final int maxZoomLevel;

    public OnlineTileProvider(int maxZoomLevel) {
        super(OfflineMapUtils.TILE_WIDTH, OfflineMapUtils.TILE_HEIGHT);
        this.maxZoomLevel = maxZoomLevel;
    }

    @Override
    public URL getTileUrl(int x, int y, int zoom) {
        if (zoom <= maxZoomLevel) {
            final String tileUrl = OfflineMapUtils.getMapTileURL(zoom, x, y);
            try {
                return new URL(tileUrl);
            } catch (MalformedURLException e) {
                Log.e(TAG, "Error while building url for mapbox map tile.", e);
            }
        }
        return null;
    }
}
