package org.farring.gcs.maps.providers.GoogleMap.offline.UI

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.maps.GoogleMap
import org.farring.gcs.R
import org.farring.gcs.maps.providers.DPMapProvider
import org.farring.gcs.maps.providers.GoogleMap.GoogleMapPrefConstants
import org.farring.gcs.maps.providers.GoogleMap.GoogleMapPrefConstants.MAPBOX_TILE_PROVIDER
import org.farring.gcs.maps.providers.GoogleMap.GoogleMapPrefConstants.TileProvider
import org.farring.gcs.maps.providers.GoogleMap.offline.Database.OfflineDatabase
import org.farring.gcs.maps.providers.MapProviderPreferences

/**
 * This is the google map provider preferences. It stores and handles all preferences related to google map.
 */
class GoogleMapPrefFragment : MapProviderPreferences() {

    companion object PrefManager {

        val DEFAULT_TILE_PROVIDER = MAPBOX_TILE_PROVIDER

        val MAP_TYPE_SATELLITE = "satellite"
        val MAP_TYPE_HYBRID = "hybrid"
        val MAP_TYPE_NORMAL = "normal"
        val MAP_TYPE_TERRAIN = "terrain"

        val PREF_TILE_PROVIDERS = "pref_google_map_tile_providers"

        val PREF_GOOGLE_TILE_PROVIDER_SETTINGS = "pref_google_tile_provider_settings"

        val PREF_MAP_TYPE = "pref_map_type"
        val DEFAULT_MAP_TYPE = MAP_TYPE_SATELLITE

        val PREF_MAPBOX_TILE_PROVIDER_SETTINGS = "pref_mapbox_tile_provider_settings"

        val PREF_MAPBOX_MAP_DOWNLOAD = "pref_mapbox_map_download"
        val PREF_MAPBOX_MAP_DELETE_DB = "pref_mapbox_map_deleteDataBase"

        fun getMapType(context: Context?): Int {
            var mapType = GoogleMap.MAP_TYPE_SATELLITE
            context?.let {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                val selectedType = sharedPref.getString(PREF_MAP_TYPE, DEFAULT_MAP_TYPE)
                when (selectedType) {
                    MAP_TYPE_HYBRID -> mapType = GoogleMap.MAP_TYPE_HYBRID
                    MAP_TYPE_NORMAL -> mapType = GoogleMap.MAP_TYPE_NORMAL
                    MAP_TYPE_TERRAIN -> mapType = GoogleMap.MAP_TYPE_TERRAIN
                    MAP_TYPE_SATELLITE -> mapType = GoogleMap.MAP_TYPE_SATELLITE
                    else -> mapType = GoogleMap.MAP_TYPE_SATELLITE
                }
            }

            return mapType
        }

        @TileProvider fun getMapTileProvider(context: Context?): String {
            var tileProvider = DEFAULT_TILE_PROVIDER
            context?.let {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                tileProvider = sharedPref.getString(PREF_TILE_PROVIDERS, tileProvider)
            }

            return tileProvider
        }

        fun setMapTileProvider(context: Context?, @TileProvider tileProvider: String?) {
            context?.let {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                sharedPref.edit().putString(PREF_TILE_PROVIDERS, tileProvider).apply()
            }
        }
    }

    private var tileProvidersPref: ListPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences_google_maps)
        setupPreferences()
    }

    private fun setupPreferences() {
        val context = activity.applicationContext
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)

        setupTileProvidersPreferences(sharedPref)
        setupGoogleTileProviderPreferences(sharedPref)
        setupMapboxTileProviderPreferences()
    }

    private fun enableTileProvider(tileProviderPref: ListPreference?, provider: String, persistPreference: Boolean) {
        if (persistPreference) {
            tileProviderPref?.value = provider
            setMapTileProvider(context, provider)
        }

        tileProviderPref?.summary = provider
        toggleTileProviderPrefs(provider)
    }

    private fun setupTileProvidersPreferences(sharedPref: SharedPreferences) {
        val tileProvidersKey = PREF_TILE_PROVIDERS
        tileProvidersPref = findPreference(tileProvidersKey) as ListPreference?

        if (tileProvidersPref != null) {
            val tileProvider = sharedPref.getString(tileProvidersKey, DEFAULT_TILE_PROVIDER)
            tileProvidersPref?.summary = tileProvider
            tileProvidersPref?.setOnPreferenceChangeListener { preference, newValue ->
                val updatedTileProvider = newValue.toString()
                var acceptChange = true
                if (acceptChange) {
                    enableTileProvider(tileProvidersPref, updatedTileProvider, false)
                    true
                } else {
                    false
                }
            }

            toggleTileProviderPrefs(tileProvider)
        }
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

    private fun setupMapboxTileProviderPreferences() {
        // Setup mapbox map download button
        val downloadMapPref = findPreference(PREF_MAPBOX_MAP_DOWNLOAD)
        downloadMapPref?.setOnPreferenceClickListener {
            // startActivity(Intent(context, DownloadMapActivity::class.java))
            true
        }

        //Setup mapbox map download button
        val deleteDBPref = findPreference(PREF_MAPBOX_MAP_DELETE_DB)
        deleteDBPref?.setOnPreferenceClickListener {
            MaterialDialog.Builder(activity)
                    .iconRes(R.drawable.ic_launcher).limitIconToDefaultSize() // limits the displayed icon size to 48dp
                    .title("删除离线地图数据库")
                    .positiveText(getString(android.R.string.yes))
                    .negativeText(getString(android.R.string.no))
                    .onPositive { dialog, which ->
                        OfflineDatabase.deleteDataBase(activity)
                    }.show()
            true
        }
    }

    private fun toggleTileProviderPrefs(tileProvider: String) {
        when (tileProvider) {
            GoogleMapPrefConstants.GOOGLE_TILE_PROVIDER -> {
                enableGoogleTileProviderPrefs(true)
                enableMapboxTileProviderPrefs(false)
            }

            MAPBOX_TILE_PROVIDER -> {
                enableGoogleTileProviderPrefs(false)
                enableMapboxTileProviderPrefs(true)
            }
        }
    }

    private fun enableGoogleTileProviderPrefs(enable: Boolean) {
        enableTileProviderPrefs(PREF_GOOGLE_TILE_PROVIDER_SETTINGS, enable)
    }

    private fun enableMapboxTileProviderPrefs(enable: Boolean) {
        enableTileProviderPrefs(PREF_MAPBOX_TILE_PROVIDER_SETTINGS, enable)
    }

    private fun enableTileProviderPrefs(prefKey: String, enable: Boolean) {
        val prefCategory = findPreference(prefKey) as PreferenceCategory?
        prefCategory?.isEnabled = enable
    }

    override fun getMapProvider(): DPMapProvider? = DPMapProvider.谷歌地图
}
