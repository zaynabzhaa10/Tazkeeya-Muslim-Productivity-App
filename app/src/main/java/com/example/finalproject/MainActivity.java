package com.example.finalproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.finalproject.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPrefs;
    // private static final String PREF_IS_FIRST_LAUNCH_COMPLETE = "is_first_launch_complete"; // <<< DIHAPUS
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = getSharedPreferences("TazkeeyaPrefs", Context.MODE_PRIVATE);

        // Ambil preferensi tema dan terapkan (ini tetap harus ada)
        int savedTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        // --- Minta Izin Notifikasi di Sini ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
        // --- Akhir Permintaan Izin ---

        // MainActivity akan selalu menampilkan landing page
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Listener untuk tombol "Ayo Mulai" di landing page
        // Pastikan ID tombol Anda di activity_main.xml adalah btnStart (sesuai kode ini)
        binding.btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Langsung pindah ke HomeContainerActivity
                navigateToHomeContainer();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Izin notifikasi ditolak. Anda mungkin tidak menerima notifikasi adzan.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void navigateToHomeContainer() {
        Intent intent = new Intent(MainActivity.this, HomeContainerActivity.class);
        startActivity(intent);
        // Tidak perlu finish() MainActivity di sini jika Anda ingin bisa kembali ke landing page
        // Namun, jika landing page hanya untuk sambutan dan tidak ingin bisa diakses dari back stack,
        // maka tambahkan finish();
        // Untuk skenario "selalu ke situ dulu", biasanya finish() dihilangkan
        // Tapi jika Anda ingin "selalu ke situ dulu, lalu ke Home, dan tidak bisa back ke landing",
        // maka finish() harus ada. Saya akan tambahkan finish() di sini untuk skenario itu.
        finish();
    }

    // Metode navigateToSettingsFromLanding() tidak lagi dibutuhkan karena alur navigasi berubah
    // private void navigateToSettingsFromLanding() { ... }
}