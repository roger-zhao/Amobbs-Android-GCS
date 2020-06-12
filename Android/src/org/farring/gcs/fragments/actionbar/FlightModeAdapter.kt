package org.farring.gcs.fragments.actionbar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.dronekit.core.drone.autopilot.Drone
import com.dronekit.core.drone.variables.ApmModes
import com.dronekit.core.drone.variables.State
import com.dronekit.core.drone.variables.Type
import org.farring.gcs.R

class FlightModeAdapter(context: Context, val drone: Drone) : SelectionListAdapter<ApmModes>(context) {

    private var selectedMode: ApmModes
    private val flightModes: List<ApmModes>

    init {
        val state: State = drone.state
        selectedMode = state.mode

        val type: Type = drone.type
        flightModes = ApmModes.getModeList(type.type)
    }

    override fun getCount() = flightModes.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vehicleMode = flightModes[position]

        val containerView = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_selection, parent, false)

        val holder = (containerView.tag as ViewHolder?) ?: ViewHolder(containerView.findViewById(R.id.item_selectable_option) as TextView,
                containerView.findViewById(R.id.item_selectable_check) as RadioButton)

        val clickListener = View.OnClickListener {
            if (drone.isConnected) {
                selectedMode = vehicleMode

                holder.checkView.isChecked = true

                drone.state.changeFlightMode(vehicleMode, null)

                listener?.onSelection()
            }
        }

        holder.checkView.isChecked = vehicleMode === selectedMode
        holder.checkView.setOnClickListener(clickListener)

        holder.labelView.text = vehicleMode.label
        holder.labelView.setOnClickListener(clickListener)

        containerView.setOnClickListener(clickListener)

        containerView.tag = holder
        return containerView
    }

    override fun getSelection() = flightModes.indexOf(selectedMode)

    class ViewHolder(val labelView: TextView, val checkView: RadioButton)
}