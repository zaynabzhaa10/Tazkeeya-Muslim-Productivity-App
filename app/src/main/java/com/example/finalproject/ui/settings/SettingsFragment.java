package com.example.finalproject.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R; // Pastikan import R benar
import com.example.finalproject.databinding.FragmentSettingsBinding; // Sesuaikan

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPrefs = requireActivity().getSharedPreferences("TazkeeyaPrefs", Context.MODE_PRIVATE);
        int currentTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        switch (currentTheme) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                binding.radioLight.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                binding.radioDark.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                binding.radioSystem.setChecked(true);
                break;
        }

        binding.themeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int themeMode;
                if (checkedId == R.id.radioLight) {
                    themeMode = AppCompatDelegate.MODE_NIGHT_NO;
                } else if (checkedId == R.id.radioDark) {
                    themeMode = AppCompatDelegate.MODE_NIGHT_YES;
                } else if (checkedId == R.id.radioSystem) {
                    themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                } else {
                    themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // Default
                }

                sharedPrefs.edit().putInt("theme_mode", themeMode).apply();
                AppCompatDelegate.setDefaultNightMode(themeMode);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}