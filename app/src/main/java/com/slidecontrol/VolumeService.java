package com.slidecontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

public class VolumeService extends Service {

    public static boolean isRunning = false;
    private static final String CHANNEL_ID = "SlideControlChannel";
    private static final int NOTIF_ID = 1;

    private SharedPreferences prefs;
    private MediaSessionHelper mediaSession;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("SlideControl", MODE_PRIVATE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        // WakeLock: prevents CPU from sleeping, keeps service alive
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SlideControl::VolumeServiceWakeLock"
            );
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(); // No timeout - held until service stops
            }
        }

        // Build persistent notification with tap-to-open action
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notifIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SlideControl Active")
            .setContentText("Vol UP = Next  |  Vol DOWN = Prev")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)        // Can't be swiped away
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();

        startForeground(NOTIF_ID, notification);

        // Start intercepting volume buttons
        mediaSession = new MediaSessionHelper(this, (command) -> {
            if (command.equals("next")) sendCommand("next");
            else if (command.equals("prev")) sendCommand("prev");
        });
        mediaSession.activate();

        // START_STICKY = Android will restart this service if it gets killed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (mediaSession != null) {
            mediaSession.deactivate();
        }

        // Release WakeLock safely
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Auto-restart: send broadcast to restart service after being killed
        Intent restartIntent = new Intent(getApplicationContext(), VolumeService.class);
        restartIntent.setPackage(getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendCommand(String cmd) {
        String ip   = prefs.getString("ip", "192.168.1.10");
        String port = prefs.getString("port", "5000");
        String url  = "http://" + ip + ":" + port + "/" + cmd;

        new Thread(() -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Slide Control",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls presentation slides via volume buttons");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
