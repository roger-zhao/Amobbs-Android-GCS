package org.farring.gcs.fragments.widget.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.farring.gcs.R

class MiniWidgetUVCLinkVideo : BaseUVCVideoWidget() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_mini_widget_uvc_video, container, false)
    }
}