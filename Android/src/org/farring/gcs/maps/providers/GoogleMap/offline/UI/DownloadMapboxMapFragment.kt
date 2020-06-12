package org.farring.gcs.maps.providers.GoogleMap.offline.UI

import android.widget.Toast
import org.farring.gcs.fragments.DroneMap
import org.farring.gcs.utils.prefs.AutoPanMode

/**
 * Created by Fredia Huya-Kouadio on 6/17/15.
 */
class DownloadMapboxMapFragment : DroneMap() {
    override fun isMissionDraggable() = false

    override fun setAutoPanMode(target: AutoPanMode?): Boolean {
        return when (target) {
            AutoPanMode.DISABLED -> true
            else -> {
                Toast.makeText(activity, "地图中心点自动跟随并不适用于该页面", Toast.LENGTH_LONG).show()
                false
            }
        }
    }
}