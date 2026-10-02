package com.vitalii.rotationcontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

/**
 * Holds a 1x1, fully transparent, non-touchable overlay window whose
 * LayoutParams.screenOrientation carries the rotation the user picked.
 * Android's window manager honors the orientation request of the top
 * overlay window, which is how all rotation-control apps work.
 *
 * The view is never visible and never receives touches. This service
 * opens no sockets, reads no data, and has no other components.
 */
public class RotationService extends Service {

    public static final String KEY_RUNNING = "running";
    public static final String ACTION_STOP = "com.vitalii.rotationcontrol.STOP";
    private static final String CHANNEL = "rotation";
    private static final int NOTIF_ID = 1;

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        createChannel();
        Notification notif = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, notif);
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int mode = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .getInt(MainActivity.KEY_MODE, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        if (overlay == null) {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            overlay = new View(this);
            overlay.setAlpha(0f);

            params = new WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            params.screenOrientation = mode;
            wm.addView(overlay, params);
        } else {
            params.screenOrientation = mode;
            wm.updateViewLayout(overlay, params);
        }

        getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .edit().putBoolean(KEY_RUNNING, true).apply();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
        getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .edit().putBoolean(KEY_RUNNING, false).apply();
        super.onDestroy();
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Rotation control", NotificationManager.IMPORTANCE_LOW));
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(this, RotationService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Rotation control is on")
                .setContentText("Screen orientation is being forced. Tap to open, or turn it off.")
                .setContentIntent(openPi)
                .addAction(0, "Turn off", stopPi)
                .setOngoing(true)
                .build();
    }
}
