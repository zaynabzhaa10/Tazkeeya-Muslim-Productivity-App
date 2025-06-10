package com.example.finalproject.ui.readlog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.finalproject.data.entity.QuranReadLog;
import com.example.finalproject.databinding.FragmentAddReadLogBinding;
import com.example.finalproject.ui.quran.QuranReadViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddReadLogFragment extends Fragment {

    private FragmentAddReadLogBinding binding;
    private QuranReadViewModel quranReadViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddReadLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        quranReadViewModel = new ViewModelProvider(requireActivity()).get(QuranReadViewModel.class);

        String[] surahNames = new String[114];
        for (int i = 0; i < 114; i++) {
            surahNames[i] = "Surah " + (i + 1);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, surahNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSurah.setAdapter(adapter);


        binding.btnSaveLog.setOnClickListener(v -> saveReadLog());
    }

    private void saveReadLog() {
        String surahName = binding.spinnerSurah.getSelectedItem().toString();
        int surahNumber = binding.spinnerSurah.getSelectedItemPosition() + 1;

        String startAyatStr = binding.etStartAyah.getText().toString();
        String endAyatStr = binding.etEndAyah.getText().toString();

        if (TextUtils.isEmpty(startAyatStr) || TextUtils.isEmpty(endAyatStr)) {
            Toast.makeText(requireContext(), "Nomor ayat awal dan akhir tidak boleh kosong.", Toast.LENGTH_SHORT).show();
            return;
        }

        int startAyah = Integer.parseInt(startAyatStr);
        int endAyah = Integer.parseInt(endAyatStr);

        if (startAyah <= 0 || endAyah <= 0 || startAyah > endAyah) {
            Toast.makeText(requireContext(), "Nomor ayat tidak valid.", Toast.LENGTH_SHORT).show();
            return;
        }

        int ayahsCount = (endAyah - startAyah) + 1;
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        QuranReadLog newLog = new QuranReadLog(currentDate, surahNumber, surahName, startAyah, endAyah, ayahsCount);
        quranReadViewModel.insert(newLog);

        Toast.makeText(requireContext(), "Catatan bacaan tersimpan! Jumlah ayat: " + ayahsCount, Toast.LENGTH_SHORT).show();

        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            Toast.makeText(requireContext(), "Catatan tersimpan.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}