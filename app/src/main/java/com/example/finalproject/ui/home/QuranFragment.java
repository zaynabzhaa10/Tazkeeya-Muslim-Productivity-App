package com.example.finalproject.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finalproject.DetailSurahActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.entity.QuranReadLog;
import com.example.finalproject.network.response.SurahResponse;
import com.example.finalproject.databinding.FragmentQuranBinding;
import com.example.finalproject.network.api.ApiService;
import com.example.finalproject.network.client.RetrofitClient;
import com.example.finalproject.ui.adapter.SurahAdapter;
import com.example.finalproject.ui.quran.QuranReadViewModel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuranFragment extends Fragment implements SurahAdapter.OnItemClickListener {

    private FragmentQuranBinding binding;
    private SurahAdapter surahAdapter;
    private List<SurahResponse.Surah> originalSurahList; // Untuk menyimpan daftar surah asli
    private SharedPreferences quranPrefs;
    private ExecutorService executorService;
    private Handler mainHandler;

    private QuranReadViewModel quranReadViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQuranBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        quranPrefs = requireActivity().getSharedPreferences("TazkeeyaPrefs", Context.MODE_PRIVATE); // Menggunakan "TazkeeyaPrefs" konsisten

        quranReadViewModel = new ViewModelProvider(requireActivity()).get(QuranReadViewModel.class);

        setupRecyclerView();
        fetchSurahs();

        binding.btnRefreshQuran.setOnClickListener(v -> fetchSurahs());

        binding.fabAddLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_quranFragment_to_addReadLogFragment);
            }
        });

        binding.btnViewReadLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.readLogListFragment);
            }
        });

        // --- LISTENER UNTUK SEARCHVIEW SURAH ---
        binding.searchViewSurah.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // Tidak melakukan apa-apa saat submit
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSurahs(newText);
                return true;
            }
        });
        // --- AKHIR LISTENER ---
    }

    private void setupRecyclerView() {
        surahAdapter = new SurahAdapter(this);
        binding.rvSurahs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSurahs.setAdapter(surahAdapter);
    }

    private void fetchSurahs() {
        binding.progressBarQuran.setVisibility(View.VISIBLE);
        binding.rvSurahs.setVisibility(View.GONE);
        binding.btnRefreshQuran.setVisibility(View.GONE);

        ApiService apiService = RetrofitClient.getEquranClient().create(ApiService.class);
        Call<SurahResponse> call = apiService.getAllSurahs();

        executorService.execute(() -> {
            call.enqueue(new Callback<SurahResponse>() {
                @Override
                public void onResponse(@NonNull Call<SurahResponse> call, @NonNull Response<SurahResponse> response) {
                    mainHandler.post(() -> {
                        binding.progressBarQuran.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            if (response.body() != null && response.body().getData() != null) {
                                List<SurahResponse.Surah> surahs = response.body().getData();
                                Log.d("QuranFragment", "Jumlah surah diterima: " + surahs.size());
                                for (SurahResponse.Surah surah : surahs) {
                                    Map<String, String> audioFullMap = surah.getAudioFull();
                                    if (audioFullMap != null) {
                                        Log.d("QuranFragment", "Surah " + surah.getNomor() + " AudioFull Map: " + audioFullMap.toString());

                                        String alafasyUrl = audioFullMap.get("05"); // Langsung coba ambil nilai dari Map
                                        if (alafasyUrl != null && !alafasyUrl.isEmpty()) {
                                            // --- KOREKSI PENTING DI SINI: TIDAK PERLU SET audioUrlAlafasy ---
                                            // Karena properti audioUrlAlafasy sudah dihapus dari model Surah
                                            // URL akan diambil langsung dari audioFull.get("05") di AyahAdapter atau MurottalFragment.
                                            Log.d("QuranFragment", "Surah " + surah.getNomor() + " Alafasy URL DITEMUKAN (untuk cek): " + alafasyUrl);
                                        } else {
                                            Log.w("QuranFragment", "Audio Alafasy (key '05') adalah NULL atau KOSONG di audioFull untuk surah " + surah.getNomor());
                                        }
                                    } else {
                                        Log.w("QuranFragment", "AudioFull map adalah NULL untuk surah " + surah.getNomor());
                                    }
                                }
                                originalSurahList = surahs;
                                surahAdapter.setSurahList(originalSurahList);
                                binding.rvSurahs.setVisibility(View.VISIBLE);
                            } else {
                                String errorDetail = "Response body is null or data is null.";
                                try {
                                    if (response.errorBody() != null) {
                                        errorDetail += " ErrorBody: " + response.errorBody().string();
                                    }
                                } catch (IOException e) {
                                    errorDetail += " Error reading errorBody: " + e.getMessage();
                                }
                                showError("API Berhasil (tapi data kosong): " + response.code() + " " + errorDetail);
                                Log.e("QuranFragment", "API success but data missing. Code: " + response.code() + ", Detail: " + errorDetail);
                            }
                        } else {
                            String errorDetail = response.message();
                            try {
                                if (response.errorBody() != null) {
                                    errorDetail += " | ErrorBody: " + response.errorBody().string();
                                }
                            } catch (IOException e) {
                                errorDetail += " | Error reading errorBody: " + e.getMessage();
                            }
                            showError("Gagal mengambil daftar surah. Code: " + response.code() + " " + errorDetail);
                            Log.e("QuranFragment", "Failed to fetch surahs. Code: " + response.code() + ", Detail: " + errorDetail);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<SurahResponse> call, @NonNull Throwable t) {
                    mainHandler.post(() -> {
                        binding.progressBarQuran.setVisibility(View.GONE);
                        showError("Kesalahan jaringan: " + (t.getMessage() != null ? t.getMessage() : "Unknown network error"));
                        binding.btnRefreshQuran.setVisibility(View.VISIBLE);
                        Log.e("QuranFragment", "API call onFailure: " + t.getMessage(), t);
                    });
                }
            });
        });
    }

    // --- METODE BARU: FILTER SURAH ---
    private void filterSurahs(String query) {
        List<SurahResponse.Surah> filteredList = new ArrayList<>();
        if (originalSurahList != null) {
            if (query.isEmpty()) {
                filteredList.addAll(originalSurahList);
            } else {
                String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
                for (SurahResponse.Surah surah : originalSurahList) {
                    // Filter berdasarkan nomor surah atau nama surah (Latin/Arab)
                    if (String.valueOf(surah.getNomor()).contains(lowerCaseQuery) ||
                            surah.getNamaLatin().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                            surah.getNama().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                        filteredList.add(surah);
                    }
                }
            }
        }
        surahAdapter.setSurahList(filteredList); // Update RecyclerView dengan daftar yang difilter
    }
    // --- AKHIR METODE BARU ---

    private void showError(String message) {
        binding.progressBarQuran.setVisibility(View.GONE);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        binding.btnRefreshQuran.setVisibility(View.VISIBLE);
        Log.e("QuranFragment", "Error: " + message);
    }

    @Override
    public void onItemClick(SurahResponse.Surah surah) {
        Intent intent = new Intent(requireContext(), DetailSurahActivity.class);
        intent.putExtra("surah_number", surah.getNomor());
        intent.putExtra("surah_name", surah.getNamaLatin());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}