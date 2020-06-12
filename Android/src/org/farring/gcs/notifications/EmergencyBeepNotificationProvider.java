package org.farring.gcs.notifications;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.evenbus.ActionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.notifications.NotificationHandler.NotificationProvider;
import org.farring.gcs.utils.prefs.DroidPlannerPrefs;

public class EmergencyBeepNotificationProvider implements NotificationProvider {

    private final Context context;
    private final DroidPlannerPrefs appPrefs;
    private SoundPool mPool;
    private int beepBeep;

    public EmergencyBeepNotificationProvider(Context context) {
        this.context = context;
        appPrefs = DroidPlannerPrefs.getInstance(context);
    }

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        switch (actionEvent) {
            case ACTION_GROUND_COLLISION_IMMINENT:
                if (mPool != null) {
                    if (appPrefs.getImminentGroundCollisionWarning()) {
                        mPool.play(beepBeep, 1f, 1f, 1, 1, 1f);
                    } else {
                        mPool.stop(beepBeep);
                    }
                }
                break;
        }
    }

    @Override
    public void init() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        mPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        beepBeep = mPool.load(context, R.raw.beep_beep, 1);
    }

    @Override
    public void onTerminate() {
        EventBus.getDefault().unregister(this);
        if (mPool != null) {
            mPool.release();
            mPool = null;
        }
    }
}
