package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finalproject.network.response.AyahResponse;
import com.example.finalproject.databinding.ActivityDetailSurahBinding;
import com.example.finalproject.network.api.ApiService;
import com.example.finalproject.network.client.RetrofitClient;
import com.example.finalproject.playback.MurottalPlaybackService;
import com.example.finalproject.ui.adapter.AyahAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailSurahActivity extends AppCompatActivity implements AyahAdapter.OnPlayClickListener {

    private ActivityDetailSurahBinding binding;
    private AyahAdapter ayahAdapter;
    private List<AyahResponse.Ayah> originalAyahList;
    private int surahNumber;
    private String surahNameLatin;
    private String surahNameArabic;
    private String surahMeaning;
    private String surahRevelationType;
    private int numberOfAyahs;
    private String surahDescription; // Ini yang akan di-toggle

    private ExecutorService executorService;
    private Handler mainHandler;

    private boolean isDescriptionExpanded = false; // Status untuk melacak deskripsi diperluas atau tidak

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailSurahBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(""); // Kosongkan judul default Toolbar
        }

        ImageView icBack = binding.icBack;
        TextView tvToolbarTitle = binding.tvDetailSurahNameLatin;

        surahNumber = getIntent().getIntExtra("surah_number", -1);
        surahNameLatin = getIntent().getStringExtra("surah_name");

        if (surahNumber == -1 || surahNameLatin == null) {
            Toast.makeText(this, "Data surah tidak valid.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvToolbarTitle.setText(surahNameLatin); // Set judul di TextView kustom

        icBack.setOnClickListener(v -> onBackPressed());

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupRecyclerView();
        fetchAyahsAndSurahDetails(surahNumber);

        binding.btnRefreshAyahs.setOnClickListener(v -> fetchAyahsAndSurahDetails(surahNumber));

        binding.searchViewAyah.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAyahs(newText);
                return true;
            }
        });

    }

    private void setupRecyclerView() {
        ayahAdapter = new AyahAdapter();
        ayahAdapter.setOnPlayClickListener(this);
        binding.rvAyahs.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAyahs.setAdapter(ayahAdapter);
    }

    private void fetchAyahsAndSurahDetails(int surahNumber) {
        binding.progressBarAyahs.setVisibility(View.VISIBLE);
        binding.rvAyahs.setVisibility(View.GONE);
        binding.btnRefreshAyahs.setVisibility(View.GONE);

        ApiService apiService = RetrofitClient.getEquranClient().create(ApiService.class);
        Call<AyahResponse> call = apiService.getAyahsOfSurah(surahNumber);

        executorService.execute(() -> {
            call.enqueue(new Callback<AyahResponse>() {
                @Override
                public void onResponse(@NonNull Call<AyahResponse> call, @NonNull Response<AyahResponse> response) {
                    mainHandler.post(() -> {
                        binding.progressBarAyahs.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            if (response.body() != null && response.body().getData() != null) {
                                AyahResponse.SurahDetail surahDetail = response.body().getData();
                                originalAyahList = surahDetail.getAyat();

                                surahNameArabic = surahDetail.getNama();
                                surahMeaning = surahDetail.getArti();
                                surahRevelationType = surahDetail.getTempatTurun();
                                numberOfAyahs = surahDetail.getJumlahAyat();
                                surahDescription = surahDetail.getDeskripsi(); // Simpan deskripsi lengkap

                                // Set TextViews
                                binding.tvDetailSurahNameLatin.setText(surahNameLatin);
                                binding.tvDetailSurahArabic.setText(surahNameArabic);
                                binding.tvDetailSurahInfo.setText(numberOfAyahs + " Ayat | " + surahRevelationType + " | " + surahMeaning);


                                if (originalAyahList != null && !originalAyahList.isEmpty()) {
                                    ayahAdapter.setAyahList(originalAyahList);
                                    binding.rvAyahs.setVisibility(View.VISIBLE);
                                    Log.d("DetailSurahActivity", "Jumlah ayat diterima: " + originalAyahList.size());
                                } else {
                                    String errorDetail = "API Berhasil, tapi daftar ayat kosong untuk surah " + surahNumber;
                                    showError(errorDetail);
                                    Log.e("DetailSurahActivity", "Ayat list is empty or null for surah " + surahNumber);
                                }
                            } else {
                                String errorDetail = "Response body or data is null.";
                                try {
                                    if (response.errorBody() != null) {
                                        errorDetail += " ErrorBody: " + response.errorBody().string();
                                    }
                                } catch (IOException e) {
                                    errorDetail += " Error reading errorBody: " + e.getMessage();
                                }
                                showError("API Berhasil (tapi body/data kosong): " + response.code() + " " + errorDetail);
                                Log.e("DetailSurahActivity", "API success but body/data missing. Code: " + response.code() + ", Detail: " + errorDetail);
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
                            showError("Gagal mengambil ayat-ayat. Code: " + response.code() + " " + errorDetail);
                            Log.e("DetailSurahActivity", "Failed to fetch ayahs. Code: " + response.code() + ", Detail: " + errorDetail);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<AyahResponse> call, @NonNull Throwable t) {
                    mainHandler.post(() -> {
                        binding.progressBarAyahs.setVisibility(View.GONE);
                        showError("Kesalahan jaringan: " + (t.getMessage() != null ? t.getMessage() : "Unknown network error"));
                        binding.btnRefreshAyahs.setVisibility(View.VISIBLE);
                        Log.e("DetailSurahActivity", "API call onFailure: " + t.getMessage(), t);
                    });
                }
            });
        });
    }

    private void filterAyahs(String query) {
        List<AyahResponse.Ayah> filteredList = new ArrayList<>();
        if (originalAyahList != null) {
            if (query.isEmpty()) {
                filteredList.addAll(originalAyahList);
            } else {
                String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
                for (AyahResponse.Ayah ayah : originalAyahList) {
                    if (String.valueOf(ayah.getNomor()).contains(lowerCaseQuery) ||
                            (ayah.getAr() != null && ayah.getAr().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (ayah.getTr() != null && ayah.getTr().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (ayah.getIdn() != null && ayah.getIdn().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery))) {
                        filteredList.add(ayah);
                    }
                }
            }
        }
        ayahAdapter.setAyahList(filteredList);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("DetailSurahActivity", "Error: " + message);
    }

    @Override
    public void onPlayClick(String audioUrl) {
        Intent serviceIntent = new Intent(this, MurottalPlaybackService.class);
        serviceIntent.setAction(MurottalPlaybackService.ACTION_PLAY);
        serviceIntent.putExtra(MurottalPlaybackService.EXTRA_AUDIO_URL, audioUrl);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}