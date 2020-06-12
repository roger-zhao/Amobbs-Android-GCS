package org.farring.gcs.maps.providers.AMap

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import com.amap.api.maps.AMap
import org.farring.gcs.R
import org.farring.gcs.maps.providers.DPMapProvider
import org.farring.gcs.maps.providers.MapProviderPreferences

/**
 * This is the google map provider preferences. It stores and handles all preferences related to google map.
 */
class AMapPrefFragment : MapProviderPreferences() {

    companion object PrefManager {
        val MAP_TYPE_SATELLITE = "satellite"
        val MAP_TYPE_NORMAL = "normal"

        val PREF_MAP_TYPE = "pref_map_type"
        val DEFAULT_MAP_TYPE = MAP_TYPE_SATELLITE

        fun getMapType(context: Context?): Int {
            var mapType = AMap.MAP_TYPE_SATELLITE
            context?.let {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                val selectedType = sharedPref.getString(PREF_MAP_TYPE, DEFAULT_MAP_TYPE)
                when (selectedType) {
                    MAP_TYPE_NORMAL -> mapType = AMap.MAP_TYPE_NORMAL
                    MAP_TYPE_SATELLITE -> mapType = AMap.MAP_TYPE_SATELLITE
                    else -> mapType = AMap.MAP_TYPE_SATELLITE
                }
            }

            return mapType
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences_gaode_maps)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        setupGoogleTileProviderPreferences(sharedPref)
    }

    private fun setupGoogleTileProviderPreferences(sharedPref: SharedPreferences) {
        val mapTypeKey = PREF_MAP_TYPE
        val mapTypePref = findPreference(mapTypeKey)
        mapTypePref?.let {
            mapTypePref.summary = sharedPref.getString(mapTypeKey, DEFAULT_MAP_TYPE)
            mapTypePref.setOnPreferenceChangeListener { preference, newValue ->
                mapTypePref.summary = newValue.toString()
                true
            }
        }
    }

    override fun getMapProvider(): DPMapProvider? = DPMapProvider.高德地图
}
