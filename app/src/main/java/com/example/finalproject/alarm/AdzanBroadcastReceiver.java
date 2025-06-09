package com.example.finalproject.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.finalproject.R; // Pastikan ini di-import dengan benar

public class AdzanBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "AdzanReceiver";
    private static MediaPlayer mediaPlayer; // Jadikan static agar bisa dihentikan jika double trigger

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Adzan alarm received! Action: " + intent.getAction());
        String prayerName = intent.getStringExtra("PRAYER_NAME");
        Toast.makeText(context, "Waktu Sholat " + prayerName + " Telah Tiba!", Toast.LENGTH_LONG).show();

        // Pastikan media player hanya satu instance atau direlease dengan benar
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        playAdzanSound(context);

        // TODO: Tampilkan notifikasi yang lebih kaya dengan opsi henti/snooze
    }

    private void playAdzanSound(Context context) {
        try {
            // Ganti 'adzan_mishary' dengan nama file MP3 adzanmu di res/raw
            Uri adzanUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.adzan_mishary); // Pastikan nama file cocok

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, adzanUri);
            mediaPlayer.prepareAsync(); // Mempersiapkan MediaPlayer secara asinkron
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting adzan playback.");
                mp.start(); // Mulai memutar audio setelah siap
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Adzan playback completed, releasing MediaPlayer.");
                mp.release(); // Melepaskan sumber daya MediaPlayer setelah selesai
                mediaPlayer = null; // Set ke null
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Error playing adzan: " + what + ", " + extra);
                Toast.makeText(context, "Gagal memutar suara adzan.", Toast.LENGTH_SHORT).show();
                mp.release(); // Melepaskan sumber daya jika terjadi error
                mediaPlayer = null; // Set ke null
                return true; // true = error ditangani
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to play adzan sound", e);
            Toast.makeText(context, "Terjadi kesalahan saat memutar suara adzan: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }
}