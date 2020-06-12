package org.farring.gcs.maps.providers;

import org.farring.gcs.maps.DPMap;
import org.farring.gcs.maps.providers.AMap.AMapFragment;
import org.farring.gcs.maps.providers.AMap.AMapPrefFragment;
import org.farring.gcs.maps.providers.GoogleMap.GoogleMapFragment;
import org.farring.gcs.maps.providers.GoogleMap.offline.UI.GoogleMapPrefFragment;

/**
 * Contains a listing of the various map providers supported, and implemented in DroidPlanner.
 */
public enum DPMapProvider {
    /**
     * Provide access to google map v2. Requires the google play services.
     */
    高德地图 {
        @Override
        public DPMap getMapFragment() {
            return new AMapFragment();
        }

        @Override
        public MapProviderPreferences getMapProviderPreferences() {
            return new AMapPrefFragment();
        }

    },

    /**
     * Provide access to google map v2. Requires the google play services.
     */
    谷歌地图 {
        @Override
        public DPMap getMapFragment() {
            return new GoogleMapFragment();
        }

        @Override
        public MapProviderPreferences getMapProviderPreferences() {
            return new GoogleMapPrefFragment();
        }
    };

    /**
     * By default, Google Map is the map provider.
     */
    public static final DPMapProvider DEFAULT_MAP_PROVIDER = 高德地图;

    /**
     * Returns the map type corresponding to the given map name.
     *
     * @param mapName name of the map type
     * @return {@link DPMapProvider} object.
     */
    public static DPMapProvider getMapProvider(String mapName) {
        if (mapName == null) {
            return null;
        }

        try {
            return DPMapProvider.valueOf(mapName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the fragment implementing the map.
     */
    public abstract DPMap getMapFragment();

    /**
     * @return the set of preferences supported by the map.
     */
    public abstract MapProviderPreferences getMapProviderPreferences();
}
