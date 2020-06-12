package org.farring.gcs.fragments.widget.diagnostics

import android.support.annotation.StringRes
import com.dronekit.core.drone.property.EkfStatus
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter
import lecho.lib.hellocharts.formatter.SimpleColumnChartValueFormatter
import lecho.lib.hellocharts.model.Axis
import lecho.lib.hellocharts.model.Column
import lecho.lib.hellocharts.model.Viewport
import org.farring.gcs.R
import org.farring.gcs.utils.SpannableUtils
import java.util.*

/**
 * Created by Fredia Huya-Kouadio on 9/15/15.
 */
class EkfStatusViewer : GraphDiagnosticViewer() {

    companion object {
        private val DECIMAL_DIGITS_NUMBER = 1

        @StringRes val LABEL_ID: Int = R.string.title_ekf_status_viewer
    }

    override fun getViewPort(refViewPort: Viewport?): Viewport {
        val viewPort = refViewPort ?: Viewport()
        viewPort.bottom = 0f
        viewPort.top = 1f
        viewPort.left = -0.5f
        viewPort.right = 4.5f

        return viewPort
    }

    override fun getYAxis() = Axis.generateAxisFromRange(0f, 1f, 0.1f)
            .setHasLines(true)
            .setFormatter(SimpleAxisValueFormatter(DECIMAL_DIGITS_NUMBER))

    override fun getXAxis() = Axis.generateAxisFromCollection(listOf(0f, 1f, 2f, 3f, 4f), listOf("vel", "h. pos", "v. pos", "mag", "terrain"))

    override fun getFormatter() = SimpleColumnChartValueFormatter(DECIMAL_DIGITS_NUMBER)

    override fun getColumns(): ArrayList<Column> {
        //Create a column for each variance
        val varianceCols = ArrayList<Column>()

        //Velocity column
        varianceCols.add(generateDisabledColumn())

        //Horizontal position column
        varianceCols.add(generateDisabledColumn())

        //Vertical position column
        varianceCols.add(generateDisabledColumn())

        //Compass variance
        varianceCols.add(generateDisabledColumn())

        //Terrain variance
        varianceCols.add(generateDisabledColumn())
        return varianceCols
    }

    override fun updateEkfView(ekfStatus: EkfStatus) {
        val variances = listOf(ekfStatus.velocityVariance,
                ekfStatus.horizontalPositionVariance,
                ekfStatus.verticalPositionVariance,
                ekfStatus.compassVariance,
                ekfStatus.terrainAltitudeVariance)

        var maxVariance = 0f
        val cols = chartData.columns
        val colsCount = cols.size - 1
        for (i in 0..colsCount) {
            val variance = variances.get(i)
            maxVariance = Math.max(maxVariance, variance)
            val varianceColor = if (variance < BaseWidgetDiagnostic.GOOD_VARIANCE_THRESHOLD) goodStatusColor
            else if (variance < BaseWidgetDiagnostic.WARNING_VARIANCE_THRESHOLD) warningStatusColor
            else dangerStatusColor

            val col = cols.get(i)
            for (value in col.values) {
                value.setTarget(variance)
                value.color = varianceColor
            }
        }

        graph?.startDataAnimation()

        val parentFragment = parentFragment
        if (parentFragment is FullWidgetDiagnostics) {
            val label = getText(LABEL_ID)
            val widgetTitle =
                    if (maxVariance < BaseWidgetDiagnostic.GOOD_VARIANCE_THRESHOLD) SpannableUtils.color(goodStatusColor, label)
                    else if (maxVariance < BaseWidgetDiagnostic.WARNING_VARIANCE_THRESHOLD) SpannableUtils.color(warningStatusColor, label)
                    else SpannableUtils.color(dangerStatusColor, label)

            parentFragment.setAdapterViewTitle(0, widgetTitle)
        }
    }

    override fun disableGraph() {
        super.disableGraph()

        val parentFragment = parentFragment
        if (parentFragment is FullWidgetDiagnostics) {
            parentFragment.setAdapterViewTitle(0, getText(LABEL_ID))
        }
    }
}