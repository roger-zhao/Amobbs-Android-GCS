package org.farring.gcs.fragments.widget.video

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.TextView
import com.evenbus.AttributeEvent
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import org.greenrobot.eventbus.Subscribe
import org.farring.gcs.R
import org.farring.gcs.dialogs.UVCDialog
import org.farring.gcs.fragments.widget.TowerWidget
import org.farring.gcs.fragments.widget.TowerWidgets
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

abstract class BaseUVCVideoWidget : TowerWidget() {

    @Subscribe fun onReceiveAttributeEvent(attributeEvent: AttributeEvent) {
        if (attributeEvent == AttributeEvent.STATE_CONNECTED) startVideoStreaming()
    }

    override fun getWidgetType() = TowerWidgets.UVC_VIDEO

    // for thread pool
    protected val CORE_POOL_SIZE = 1   // initial/minimum threads
    protected val MAX_POOL_SIZE = 4    // maximum threads
    protected val KEEP_ALIVE_TIME = 10 // time periods while keep the idle thread
    protected val EXECUTER: ThreadPoolExecutor = ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME.toLong(), TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

    // Aspect ratio
    protected val ASPECT_RATIO_4_3: Float = 3f / 4f
    protected val ASPECT_RATIO_16_9: Float = 9f / 16f
    protected var aspectRatio: Float = ASPECT_RATIO_4_3

    // for accessing USB and USB camera
    protected var mUSBMonitor: USBMonitor? = null
    protected var mUVCCamera: UVCCamera? = null
    protected var isPreview: Boolean = false
    protected var usbDevice: UsbDevice? = null
    protected var mPreviewSurface: Surface? = null


    protected val textureView by lazy(LazyThreadSafetyMode.NONE) {
        view?.findViewById(R.id.uvc_video_view) as TextureView?
    }

    protected val videoStatus by lazy(LazyThreadSafetyMode.NONE) {
        view?.findViewById(R.id.uvc_video_status) as TextView?
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textureView?.surfaceTextureListener = mSurfaceTextureListener

        mUSBMonitor = USBMonitor(activity, mOnDeviceConnectListener)
    }

    override fun onResume() {
        super.onResume()

        mUSBMonitor?.register()
        aspectRatio = appPrefs.uvcVideoAspectRatio
    }

    override fun onPause() {
        super.onPause()

        mUSBMonitor?.unregister()
        mUVCCamera?.close()
        appPrefs.uvcVideoAspectRatio = aspectRatio
    }

    override fun onDestroy() {
        super.onDestroy()

        mUVCCamera?.destroy()
        mUVCCamera = null
        isPreview = false
        mUSBMonitor?.destroy()
        mUSBMonitor = null
    }


    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            startVideoStreaming()
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {

            usbDevice = device
            mUVCCamera?.destroy()
            mUVCCamera = UVCCamera()

            videoStatus?.visibility = View.GONE

            EXECUTER.execute({
                mUVCCamera?.open(ctrlBlock)

                mPreviewSurface?.release()
                mPreviewSurface = null

                try {
                    mUVCCamera?.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG)
                } catch (e: IllegalArgumentException) {
                    try {
                        // fallback to YUV mode
                        mUVCCamera?.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE)
                    } catch (e1: IllegalArgumentException) {
                        mUVCCamera?.destroy()
                        mUVCCamera = null
                    }
                }

                if (mUVCCamera != null) {
                    val st = textureView?.surfaceTexture
                    if (st != null) {
                        mPreviewSurface = Surface(st)
                        mUVCCamera?.setPreviewDisplay(mPreviewSurface)
                        mUVCCamera?.startPreview()
                        isPreview = true
                    }

                }
            })
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            mUVCCamera?.close()
            mPreviewSurface?.release()
            mPreviewSurface = null
            isPreview = false

            videoStatus?.visibility = View.VISIBLE
        }

        override fun onDettach(device: UsbDevice) {
            mUVCCamera?.close()
            mPreviewSurface?.release()
            mPreviewSurface = null
            isPreview = false

            videoStatus?.visibility = View.VISIBLE
        }

        override fun onCancel() {
            videoStatus?.visibility = View.VISIBLE
        }
    }

    protected fun startVideoStreaming() {
        if (usbDevice != null) {
            mUSBMonitor?.requestPermission(usbDevice);
        } else {
            //UVC Device Filter
            val uvcFilter = DeviceFilter.getDeviceFilters(activity, R.xml.uvc_device_filter)
            val uvcDevices = mUSBMonitor?.getDeviceList(uvcFilter[0])
            if (uvcDevices == null || uvcDevices.isEmpty()) {
            } else {
                if (uvcDevices.size.compareTo(1) == 0) {
                    usbDevice = uvcDevices.get(0);
                    mUSBMonitor?.requestPermission(usbDevice)
                } else {
                    UVCDialog.showDialog(activity, mUSBMonitor)
                }
            }
        }
    }

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            adjustAspectRatio(textureView as TextureView)
            startVideoStreaming()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            mPreviewSurface?.release()
            mPreviewSurface = null
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        }
    }

    protected fun adjustAspectRatio(textureView: TextureView) {
        val viewWidth = textureView.width
        val viewHeight = textureView.height

        val newWidth: Int
        val newHeight: Int
        if (viewHeight > (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth
            newHeight = (viewWidth * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (viewHeight / aspectRatio).toInt();
            newHeight = viewHeight
        }
        val xoff = (viewWidth - newWidth) / 2f
        val yoff = (viewHeight - newHeight) / 2f

        val txform = Matrix();
        textureView.getTransform(txform);
        txform.setScale((newWidth.toFloat() / viewWidth), newHeight.toFloat() / viewHeight);

        txform.postTranslate(xoff, yoff);
        textureView.setTransform(txform);
    }
}