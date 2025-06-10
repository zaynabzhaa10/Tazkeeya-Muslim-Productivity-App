package com.example.finalproject.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;

import java.io.IOException;

public class MurottalPlaybackService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private MediaPlayer mediaPlayer;
    private String audioUrl;
    private static final String CHANNEL_ID = "MurottalPlaybackChannel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY = "com.example.finalproject.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.finalproject.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.example.finalproject.ACTION_STOP";
    public static final String ACTION_COMPLETED = "com.example.finalproject.ACTION_COMPLETED";

    public static final String EXTRA_AUDIO_URL = "extra_audio_url";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MurottalService", "Service onCreate");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                String newAudioUrl = intent.getStringExtra(EXTRA_AUDIO_URL);

                switch (action) {
                    case ACTION_PLAY:
                        if (newAudioUrl != null && !newAudioUrl.equals(audioUrl)) {
                            stopAudio();
                            audioUrl = newAudioUrl;
                            playAudio(audioUrl);
                            startForeground(NOTIFICATION_ID, createNotification("Memutar Murottal"));
                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {

                            mediaPlayer.start();
                            Toast.makeText(this, "Murottal dilanjutkan", Toast.LENGTH_SHORT).show();
                            startForeground(NOTIFICATION_ID, createNotification("Memutar Murottal"));
                        } else if (mediaPlayer == null && newAudioUrl != null) {
                            audioUrl = newAudioUrl;
                            playAudio(audioUrl);
                            startForeground(NOTIFICATION_ID, createNotification("Memutar Murottal"));
                        } else if (mediaPlayer != null && mediaPlayer.isPlaying()){
                        }
                        break;
                    case ACTION_PAUSE:
                        pauseAudio();
                        stopForeground(false);
                        updateNotification("Murottal Dijeda");
                        break;
                    case ACTION_STOP:
                        stopAudio();
                        stopForeground(true);
                        stopSelf();
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    private void playAudio(String url) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnCompletionListener(this);
        } else {
            mediaPlayer.reset();
        }
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            Log.d("MurottalService", "Mempersiapkan audio: " + url);
        } catch (IOException e) {
            Log.e("MurottalService", "Error setting data source", e);
            Toast.makeText(this, "Gagal memuat audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            stopAudio();
            sendBroadcast(new Intent(ACTION_STOP));
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Toast.makeText(this, "Murottal dijeda", Toast.LENGTH_SHORT).show();
            sendBroadcast(new Intent(ACTION_PAUSE));
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            audioUrl = null;
            Toast.makeText(this, "Murottal dihentikan", Toast.LENGTH_SHORT).show();
            sendBroadcast(new Intent(ACTION_STOP));
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        Toast.makeText(this, "Murottal diputar", Toast.LENGTH_SHORT).show();
        updateNotification("Memutar Murottal");
        Intent intent = new Intent(ACTION_PLAY);
        intent.putExtra(EXTRA_AUDIO_URL, audioUrl);
        sendBroadcast(intent);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("MurottalService", "MediaPlayer error: " + what + ", " + extra);
        Toast.makeText(this, "Kesalahan saat memutar audio.", Toast.LENGTH_SHORT).show();
        stopAudio();
        sendBroadcast(new Intent(ACTION_STOP));
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("MurottalService", "Playback completed");
        stopAudio();
        sendBroadcast(new Intent(ACTION_COMPLETED));
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MurottalService", "Service onDestroy");
        stopAudio();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Murottal Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel untuk notifikasi pemutar murottal");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MurottalPlaybackService.class);
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            playPauseIntent.setAction(ACTION_PAUSE);
        } else {
            playPauseIntent.setAction(ACTION_PLAY);
            playPauseIntent.putExtra(EXTRA_AUDIO_URL, audioUrl);
        }
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MurottalPlaybackService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tazkeeya Murottal")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_headphone)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .addAction(mediaPlayer != null && mediaPlayer.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow,
                        mediaPlayer != null && mediaPlayer.isPlaying() ? "Jeda" : "Putar",
                        playPausePendingIntent)
                .addAction(R.drawable.ic_stop, "Hentikan", stopPendingIntent)
                .setStyle(new MediaStyle());

        return builder.build();
    }

    private void updateNotification(String contentText) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(contentText));
        }
    }

    private class MediaStyle extends NotificationCompat.Style {
    }
}
