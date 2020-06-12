package org.farring.gcs.fragments.widget.diagnostics

import com.dronekit.core.drone.property.EkfStatus
import com.dronekit.core.drone.property.Vibration
import com.dronekit.core.drone.variables.State
import com.evenbus.AttributeEvent
import org.greenrobot.eventbus.Subscribe
import org.farring.gcs.fragments.widget.TowerWidget
import org.farring.gcs.fragments.widget.TowerWidgets

/**
 * Created by Fredia Huya-Kouadio on 8/30/15.
 */
abstract class BaseWidgetDiagnostic : TowerWidget() {

    companion object {
        val INVALID_HIGHEST_VARIANCE: Float = -1f

        /**
         * Any variance value less than this threshold is considered good.
         */
        val GOOD_VARIANCE_THRESHOLD: Float = 0.5f

        /**
         * Variance values between the good threshold and the warning threshold are considered as warning.
         * Variance values above the warning variance threshold are considered bad.
         */
        val WARNING_VARIANCE_THRESHOLD: Float = 0.8f

        /**
         * Vibration values less or equal to this value are considered good.
         */
        val GOOD_VIBRATION_THRESHOLD: Int = 30

        /**
         * Vibration values between the good threshold and the warning threshold are in the warning zone.
         * Vibration values above the warning threshold are in the danger zone.
         */
        val WARNING_VIBRATION_THRESHOLD: Int = 60
    }

    @Subscribe fun onReceiveAttributeEvent(attributeEvent: AttributeEvent) {
        if (attributeEvent == AttributeEvent.STATE_EKF_REPORT) updateEkfStatus()
        else if (attributeEvent == AttributeEvent.STATE_CONNECTED || attributeEvent == AttributeEvent.STATE_DISCONNECTED || attributeEvent == AttributeEvent.HEARTBEAT_RESTORED || attributeEvent == AttributeEvent.HEARTBEAT_TIMEOUT) {
            updateEkfStatus()
            updateVibrationStatus()
        } else if (attributeEvent == AttributeEvent.STATE_VEHICLE_VIBRATION) updateVibrationStatus()
    }

    private fun updateEkfStatus() {
        if (!isAdded)
            return

        val state: State? = drone?.state
        val ekfStatus = state?.ekfStatus
        if (state == null || !drone.isConnectionAlive || ekfStatus == null || !drone.isConnected) {
            disableEkfView()
        } else {
            updateEkfView(ekfStatus)
        }
    }

    private fun updateVibrationStatus() {
        if (!isAdded)
            return

        val state: State? = drone?.state
        val vibration = drone?.vibration
        if (state == null || !drone.isConnectionAlive || vibration == null || !drone.isConnected) {
            disableVibrationView()
        } else {
            updateVibrationView(vibration)
        }
    }

    protected open fun disableEkfView() {
    }

    protected open fun updateEkfView(ekfStatus: EkfStatus) {
    }

    protected open fun disableVibrationView() {
    }

    protected open fun updateVibrationView(vibration: Vibration) {
    }

    override fun getWidgetType() = TowerWidgets.VEHICLE_DIAGNOSTICS

    override fun onStart() {
        super.onStart()
        updateEkfStatus()
        updateVibrationStatus()
    }

    override fun onStop() {
        super.onStop()
        updateEkfStatus()
        updateVibrationStatus()
    }
}