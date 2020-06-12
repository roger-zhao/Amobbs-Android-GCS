package org.farring.gcs.fragments.widget.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.farring.gcs.R

class FullWidgetUVCLinkVideo : BaseUVCVideoWidget() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_widget_uvc_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textureView?.setOnClickListener {
            when (aspectRatio) {
                ASPECT_RATIO_4_3 -> {
                    aspectRatio = ASPECT_RATIO_16_9
                    Toast.makeText(context, "屏幕宽高比 16:9", Toast.LENGTH_SHORT).show()
                }
                ASPECT_RATIO_16_9 -> {
                    aspectRatio = ASPECT_RATIO_4_3
                    Toast.makeText(context, "屏幕宽高比 4:3", Toast.LENGTH_SHORT).show()
                }
            }
            adjustAspectRatio(textureView as TextureView)
        }
    }
}