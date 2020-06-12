package org.farring.gcs.maps.providers.GoogleMap;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Fredia Huya-Kouadio on 6/16/15.
 */
public class GoogleMapPrefConstants {

    public static final String GOOGLE_TILE_PROVIDER = "谷歌原生";
    public static final String MAPBOX_TILE_PROVIDER = "谷歌中国";

    //Prevent instantiation
    private GoogleMapPrefConstants() {
    }

    @StringDef({GOOGLE_TILE_PROVIDER, MAPBOX_TILE_PROVIDER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TileProvider {
    }
}
