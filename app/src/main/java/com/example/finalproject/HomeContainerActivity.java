package com.example.finalproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.finalproject.databinding.ActivityHomeContainerBinding;

public class HomeContainerActivity extends AppCompatActivity {

    private ActivityHomeContainerBinding binding;
    private NavController navController;
    private SharedPreferences sharedPrefs;
    // private static final String PREF_IS_SETUP_COMPLETE = "is_setup_complete"; // <<< DIHAPUS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = getSharedPreferences("TazkeeyaPrefs", Context.MODE_PRIVATE);

        // Terapkan tema (ini tetap harus ada)
        int savedTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        binding = ActivityHomeContainerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(binding.bottomNavView, navController);

        // --- KOREKSI DI SINI: Hapus logika navigasi awal ke SettingsFragment ---
        // boolean navigateToSettingsOnStart = getIntent().getBooleanExtra("navigate_to_settings_on_start", false);
        // if (navigateToSettingsOnStart) {
        //     navController.navigate(R.id.settingsFragment);
        // }

        // Hapus listener onDestinationChangedListener karena tidak lagi mengelola is_setup_complete
        // navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
        //     if (destination.getId() != R.id.settingsFragment && !sharedPrefs.getBoolean(PREF_IS_SETUP_COMPLETE, false)) {
        //         sharedPrefs.edit().putBoolean(PREF_IS_SETUP_COMPLETE, true).apply();
        //     }
        // });
        // --- AKHIR KOREKSI ---
    }

    @Override
    public void onBackPressed() {
        // Jika tidak ada fragment di back stack navigasi, biarkan default (keluar aplikasi).
        if (!navController.popBackStack()) {
            super.onBackPressed();
        }
    }
}