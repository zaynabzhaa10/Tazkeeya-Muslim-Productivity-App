package com.example.finalproject.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finalproject.R;
import com.example.finalproject.network.response.SurahResponse;
import com.example.finalproject.databinding.FragmentMurottalBinding;
import com.example.finalproject.network.api.ApiService;
import com.example.finalproject.network.client.RetrofitClient;
import com.example.finalproject.playback.MurottalPlaybackService;
import com.example.finalproject.ui.adapter.SurahAdapter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MurottalFragment extends Fragment implements SurahAdapter.OnItemClickListener {

    private FragmentMurottalBinding binding;
    private SurahAdapter surahAdapter;
    private List<SurahResponse.Surah> originalSurahList;
    private ExecutorService executorService;
    private Handler mainHandler;

    private int currentPlayingSurahNumber = -1;
    private String currentPlayingAudioUrl = null;

    private BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (MurottalPlaybackService.ACTION_PLAY.equals(action)) {
                    String audioUrl = intent.getStringExtra(MurottalPlaybackService.EXTRA_AUDIO_URL);
                    String surahName = getSurahNameFromUrl(audioUrl);
                    binding.tvCurrentPlaying.setText("Q.S " + surahName);
                    currentPlayingAudioUrl = audioUrl;
                } else if (MurottalPlaybackService.ACTION_PAUSE.equals(action)) {
                    String currentText = binding.tvCurrentPlaying.getText().toString();
                    if (currentText.startsWith("Memutar: ")) {
                        binding.tvCurrentPlaying.setText("Dijeda: " + currentText.replace("Memutar: ", ""));
                    } else {
                        binding.tvCurrentPlaying.setText("Murottal dijeda");
                    }
                } else if (MurottalPlaybackService.ACTION_STOP.equals(action)) {
                    binding.tvCurrentPlaying.setText("Tidak ada murottal yang diputar");
                    currentPlayingSurahNumber = -1;
                    currentPlayingAudioUrl = null;
                } else if (MurottalPlaybackService.ACTION_COMPLETED.equals(action)) {
                    playNextSurah();
                }
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMurottalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupRecyclerView();
        fetchSurahsForMurottal();

        binding.btnRefreshMurottal.setOnClickListener(v -> fetchSurahsForMurottal());

        binding.btnPlayMurottal.setOnClickListener(v -> {
            if (currentPlayingAudioUrl != null && !currentPlayingAudioUrl.isEmpty()) {
                Intent serviceIntent = new Intent(requireContext(), MurottalPlaybackService.class);
                serviceIntent.setAction(MurottalPlaybackService.ACTION_PLAY);
                serviceIntent.putExtra(MurottalPlaybackService.EXTRA_AUDIO_URL, currentPlayingAudioUrl);
                ContextCompat.startForegroundService(requireContext(), serviceIntent);
            } else {
                Toast.makeText(requireContext(), "Pilih surah untuk diputar dari daftar.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnPauseMurottal.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(requireContext(), MurottalPlaybackService.class);
            serviceIntent.setAction(MurottalPlaybackService.ACTION_PAUSE);
            ContextCompat.startForegroundService(requireContext(), serviceIntent);
        });

        binding.btnStopMurottal.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(requireContext(), MurottalPlaybackService.class);
            serviceIntent.setAction(MurottalPlaybackService.ACTION_STOP);
            ContextCompat.startForegroundService(requireContext(), serviceIntent);
        });

        binding.btnPreviousMurottal.setOnClickListener(v -> playPreviousSurah());
        binding.btnNextMurottal.setOnClickListener(v -> playNextSurah());

        binding.searchViewMurottalSurah.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSurahs(newText);
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        surahAdapter = new SurahAdapter(this);
        binding.rvMurottalSurahs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMurottalSurahs.setAdapter(surahAdapter);
    }

    private void fetchSurahsForMurottal() {
        binding.progressBarMurottal.setVisibility(View.VISIBLE);
        binding.rvMurottalSurahs.setVisibility(View.GONE);
        binding.btnRefreshMurottal.setVisibility(View.GONE);

        ApiService apiService = RetrofitClient.getEquranClient().create(ApiService.class);
        Call<SurahResponse> call = apiService.getAllSurahs();

        executorService.execute(() -> {
            call.enqueue(new Callback<SurahResponse>() {
                @Override
                public void onResponse(@NonNull Call<SurahResponse> call, @NonNull Response<SurahResponse> response) {
                    mainHandler.post(() -> {
                        binding.progressBarMurottal.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            if (response.body() != null && response.body().getData() != null) {
                                List<SurahResponse.Surah> surahs = response.body().getData();
                                originalSurahList = new ArrayList<>(surahs);
                                Log.d("MurottalFragment", "Jumlah surah diterima: " + surahs.size());
                                for (SurahResponse.Surah surah : surahs) {
                                    Map<String, String> audioFullMap = surah.getAudioFull();
                                    if (audioFullMap != null) {
                                        Log.d("MurottalFragment", "Surah " + surah.getNomor() + " AudioFull Map: " + audioFullMap.toString());
                                        String alafasyUrl = audioFullMap.get("05");
                                        if (alafasyUrl != null && !alafasyUrl.isEmpty()) {
                                            Log.d("MurottalFragment", "Surah " + surah.getNomor() + " Alafasy URL DITEMUKAN (dari Map): " + alafasyUrl);
                                        } else {
                                            Log.w("MurottalFragment", "Audio Alafasy (key '05') adalah NULL atau KOSONG di audioFull untuk surah " + surah.getNomor());
                                        }
                                    } else {
                                        Log.w("MurottalFragment", "AudioFull map adalah NULL untuk surah " + surah.getNomor());
                                    }
                                }
                                surahAdapter.setSurahList(surahs);
                                binding.rvMurottalSurahs.setVisibility(View.VISIBLE);
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
                                Log.e("MurottalFragment", "API success but data missing. Code: " + response.code() + ", Detail: " + errorDetail);
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
                            showError("Gagal mengambil daftar surah murottal. Code: " + response.code() + " " + errorDetail);
                            Log.e("MurottalFragment", "Failed to fetch surahs. Code: " + response.code() + ", Detail: " + errorDetail);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<SurahResponse> call, @NonNull Throwable t) {
                    mainHandler.post(() -> {
                        binding.progressBarMurottal.setVisibility(View.GONE);
                        showError("Kesalahan jaringan: " + (t.getMessage() != null ? t.getMessage() : "Unknown network error"));
                        binding.btnRefreshMurottal.setVisibility(View.VISIBLE);
                        Log.e("MurottalFragment", "API call onFailure: " + t.getMessage(), t);
                    });
                }
            });
        });
    }

    private void showError(String message) {
        binding.progressBarMurottal.setVisibility(View.GONE);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        binding.btnRefreshMurottal.setVisibility(View.VISIBLE);
        Log.e("MurottalFragment", "Error: " + message);
    }

    @Override
    public void onItemClick(SurahResponse.Surah surah) {
        Map<String, String> audioFullMap = surah.getAudioFull();
        if (audioFullMap != null) {
            String audioUrl = audioFullMap.get("05");
            if (audioUrl != null && !audioUrl.isEmpty()) {
                Intent serviceIntent = new Intent(requireContext(), MurottalPlaybackService.class);
                serviceIntent.setAction(MurottalPlaybackService.ACTION_PLAY);
                serviceIntent.putExtra(MurottalPlaybackService.EXTRA_AUDIO_URL, audioUrl);
                ContextCompat.startForegroundService(requireContext(), serviceIntent);

                currentPlayingSurahNumber = surah.getNomor();
                binding.tvCurrentPlaying.setText("Q.S " + surah.getNamaLatin());
            } else {
                Toast.makeText(requireContext(), "URL Audio Alafasy tidak ditemukan atau kosong untuk surah ini.", Toast.LENGTH_SHORT).show();
                Log.w("MurottalFragment", "Audio URL Alafasy kosong (dari map) untuk surah " + surah.getNomor() + ". Map keys: " + audioFullMap.keySet());
            }
        } else {
            Toast.makeText(requireContext(), "Audio murottal untuk surah ini tidak tersedia (Map kosong).", Toast.LENGTH_SHORT).show();
            Log.w("MurottalFragment", "AudioFull map adalah NULL untuk surah " + surah.getNomor());
        }
    }

    private void playNextSurah() {
        if (surahAdapter == null || surahAdapter.getItemCount() == 0) return;

        int nextSurahNumber = currentPlayingSurahNumber + 1;
        if (nextSurahNumber > 114) {
            nextSurahNumber = 1;
        }

        for (SurahResponse.Surah surah : surahAdapter.getSurahList()) {
            if (surah.getNomor() == nextSurahNumber) {
                onItemClick(surah);
                return;
            }
        }
        Toast.makeText(requireContext(), "Tidak dapat menemukan surah berikutnya.", Toast.LENGTH_SHORT).show();
    }

    private void playPreviousSurah() {
        if (surahAdapter == null || surahAdapter.getItemCount() == 0) return;

        int previousSurahNumber = currentPlayingSurahNumber - 1;
        if (previousSurahNumber < 1) {
            previousSurahNumber = 114;
        }

        for (SurahResponse.Surah surah : surahAdapter.getSurahList()) {
            if (surah.getNomor() == previousSurahNumber) {
                onItemClick(surah);
                return;
            }
        }
        Toast.makeText(requireContext(), "Tidak dapat menemukan surah sebelumnya.", Toast.LENGTH_SHORT).show();
    }

    private String getSurahNameFromUrl(String audioUrl) {
        if (surahAdapter == null || surahAdapter.getItemCount() == 0) return "Surah Tidak Dikenal";
        for (SurahResponse.Surah surah : surahAdapter.getSurahList()) {
            if (surah.getAudioFull() != null && surah.getAudioFull().containsKey("05") && surah.getAudioFull().get("05").equals(audioUrl)) {
                return surah.getNamaLatin();
            }
        }
        return "Surah Tidak Dikenal";
    }

    private void filterSurahs(String query) {
        List<SurahResponse.Surah> filteredList = new ArrayList<>();
        if (originalSurahList != null) {
            if (query.isEmpty()) {
                filteredList.addAll(originalSurahList);
            } else {
                String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
                for (SurahResponse.Surah surah : originalSurahList) {
                    if (String.valueOf(surah.getNomor()).contains(lowerCaseQuery) ||
                            surah.getNamaLatin().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                            surah.getNama().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                        filteredList.add(surah);
                    }
                }
            }
        }
        surahAdapter.setSurahList(filteredList);
        if (filteredList.isEmpty() && !query.isEmpty()) {
            binding.tvNoMurottalSurahFound.setVisibility(View.VISIBLE);
            binding.rvMurottalSurahs.setVisibility(View.GONE);
        } else {
            binding.tvNoMurottalSurahFound.setVisibility(View.GONE);
            binding.rvMurottalSurahs.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MurottalPlaybackService.ACTION_PLAY);
        filter.addAction(MurottalPlaybackService.ACTION_PAUSE);
        filter.addAction(MurottalPlaybackService.ACTION_STOP);
        filter.addAction(MurottalPlaybackService.ACTION_COMPLETED);
        ContextCompat.registerReceiver(requireContext(), playbackReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(playbackReceiver);
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