package com.dronekit.core.gcs.location;

import android.os.Handler;
import android.os.Looper;

import com.amap.api.location.AMapLocation;
import com.dronekit.core.gcs.location.Location.LocationFinder;
import com.dronekit.core.gcs.location.Location.LocationReceiver;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.farring.gcs.utils.DroneMapHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feeds Location Data from Android's FusedLocation LocationProvider
 */
public class FusedLocation implements LocationFinder {

    // 通常精度为, GPS：<20米，WiFi：30-180米，基站：150-800米.
    private static final float LOCATION_ACCURACY_THRESHOLD = 20.0f;
    private static final float JUMP_FACTOR = 4.0f;
    private final Map<String, LocationReceiver> receivers = new ConcurrentHashMap<>();
    private float mTotalSpeed;
    private long mSpeedReadings;
    private Handler mainHandler;
    private AMapLocation mLastLocation;

    public FusedLocation() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void enableLocationUpdates() {
        EventBus.getDefault().register(this);
        mSpeedReadings = 0;
        mTotalSpeed = 0f;
        mLastLocation = null;
    }

    @Override
    public void disableLocationUpdates() {
        EventBus.getDefault().unregister(this);
    }

    private void notifyLocationUpdate(Location location) {
        if (receivers.isEmpty())
            return;

        for (LocationReceiver receiver : receivers.values()) {
            receiver.onLocationUpdate(location);
        }
    }

    @Override
    public void addLocationListener(String tag, LocationReceiver receiver) {
        receivers.put(tag, receiver);
    }

    @Override
    public void removeLocationListener(String tag) {
        receivers.remove(tag);
    }

    private void notifyLocationUnavailable() {
        if (receivers.isEmpty())
            return;

        for (LocationReceiver listener : receivers.values()) {
            listener.onLocationUnavailable();
        }
    }

    private boolean isLocationAccurate(float accuracy, float currentSpeed) {
        if (accuracy >= LOCATION_ACCURACY_THRESHOLD) {
            Logger.i("High accuracy: " + accuracy);
            return false;
        }

        mTotalSpeed += currentSpeed;
        float avg = (mTotalSpeed / ++mSpeedReadings);

        // If moving:
        if (currentSpeed > 0) {
            // if average indicates some movement
            if (avg >= 1.0) {
                // Reject unreasonable updates.
                if (currentSpeed >= (avg * JUMP_FACTOR)) {
                    Logger.i("High current speed: " + currentSpeed);
                    return false;
                }
            }
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null)
            return;

        // 地图纠偏！！！【官方说法】定位SDK在大陆、香港、澳门返回gcj02坐标；台湾、海外返回原始wgs84坐标。
        if (aMapLocation.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
            float distanceToLast = -1.0f;
            long timeSinceLast = -1L;
            final long androidLocationTime = aMapLocation.getTime();
            if (mLastLocation != null) {
                distanceToLast = aMapLocation.distanceTo(mLastLocation);
                timeSinceLast = (androidLocationTime - mLastLocation.getTime()) / 1000;
            }

            final float currentSpeed = distanceToLast > 0f && timeSinceLast > 0
                    ? (distanceToLast / timeSinceLast)
                    : 0f;
            final boolean isLocationAccurate = isLocationAccurate(aMapLocation.getAccuracy(), currentSpeed);

            final Location location = new Location(
                    new LatLongAlt(DroneMapHelper.AMapLocationToCoord(aMapLocation), aMapLocation.getAltitude()),
                    aMapLocation.getBearing(),
                    aMapLocation.hasSpeed() ? aMapLocation.getSpeed() : currentSpeed,
                    isLocationAccurate, aMapLocation.getAccuracy());

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyLocationUpdate(location);
                }
            });
            this.mLastLocation = aMapLocation;
        } else {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyLocationUnavailable();
                }
            });
        }
    }
}
