package org.farring.gcs.fragments.widget.diagnostics

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.astuetz.PagerSlidingTabStrip
import org.farring.gcs.R
import org.farring.gcs.fragments.widget.TowerWidget
import org.farring.gcs.fragments.widget.TowerWidgets

/**
 * Created by Fredia Huya-Kouadio on 8/30/15.
 */
class FullWidgetDiagnostics : TowerWidget() {

    private val viewAdapter by lazy(LazyThreadSafetyMode.NONE) {
        DiagnosticViewAdapter(context, childFragmentManager)
    }

    private var tabPageIndicator: PagerSlidingTabStrip ? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_full_widget_diagnostics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById(R.id.diagnostics_view_pager) as ViewPager?
        viewPager?.adapter = viewAdapter
        viewPager?.offscreenPageLimit = 2

        tabPageIndicator = view.findViewById(R.id.pager_title_strip) as PagerSlidingTabStrip ?
        tabPageIndicator?.setViewPager(viewPager)
    }

    override fun getWidgetType() = TowerWidgets.VEHICLE_DIAGNOSTICS

    fun setAdapterViewTitle(position: Int, title: CharSequence) {
        viewAdapter.setViewTitles(position, title)
        tabPageIndicator?.notifyDataSetChanged()
    }
}