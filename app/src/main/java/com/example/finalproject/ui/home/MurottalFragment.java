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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MurottalFragment extends Fragment implements SurahAdapter.OnItemClickListener {

    private FragmentMurottalBinding binding;
    private SurahAdapter surahAdapter;
    private ExecutorService executorService;
    private Handler mainHandler;

    private int currentPlayingSurahNumber = -1;

    private BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (MurottalPlaybackService.ACTION_PLAY.equals(action)) {
                    String audioUrl = intent.getStringExtra(MurottalPlaybackService.EXTRA_AUDIO_URL);
                    String surahName = getSurahNameFromUrl(audioUrl);
                    binding.tvCurrentPlaying.setText("Memutar: " + surahName);
                    binding.btnPlayPauseMurottal.setImageResource(R.drawable.ic_pause);
                } else if (MurottalPlaybackService.ACTION_PAUSE.equals(action)) {
                    String currentText = binding.tvCurrentPlaying.getText().toString();
                    if (currentText.startsWith("Memutar: ")) {
                        binding.tvCurrentPlaying.setText("Dijeda: " + currentText.replace("Memutar: ", ""));
                    } else if (currentText.startsWith("Dijeda: ")) {
                        binding.tvCurrentPlaying.setText("Memutar: " + currentText.replace("Dijeda: ", ""));
                    } else {
                        binding.tvCurrentPlaying.setText("Murottal dijeda");
                    }
                    binding.btnPlayPauseMurottal.setImageResource(R.drawable.ic_play_arrow);
                } else if (MurottalPlaybackService.ACTION_STOP.equals(action)) {
                    binding.tvCurrentPlaying.setText("Tidak ada murottal yang diputar");
                    binding.btnPlayPauseMurottal.setImageResource(R.drawable.ic_play_arrow);
                    currentPlayingSurahNumber = -1;
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

        binding.btnPlayPauseMurottal.setOnClickListener(v -> {
            if (currentPlayingSurahNumber != -1) {
                Intent serviceIntent = new Intent(requireContext(), MurottalPlaybackService.class);
                if (binding.btnPlayPauseMurottal.getDrawable().getConstantState().equals(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause).getConstantState())) {
                    serviceIntent.setAction(MurottalPlaybackService.ACTION_PAUSE);
                } else {
                    if (currentPlayingSurahNumber > 0 && currentPlayingSurahNumber <= 114) {
                        String audioUrlToPlay = null;
                        if (surahAdapter != null && surahAdapter.getSurahList() != null) {
                            for (SurahResponse.Surah s : surahAdapter.getSurahList()) {
                                if (s.getNomor() == currentPlayingSurahNumber && s.getAudioFull() != null) {
                                    audioUrlToPlay = s.getAudioFull().get("05");
                                    break;
                                }
                            }
                        }
                        if (audioUrlToPlay != null && !audioUrlToPlay.isEmpty()) {
                            serviceIntent.setAction(MurottalPlaybackService.ACTION_PLAY);
                            serviceIntent.putExtra(MurottalPlaybackService.EXTRA_AUDIO_URL, audioUrlToPlay);
                        } else {
                            Toast.makeText(requireContext(), "Tidak ada audio untuk dilanjutkan. Pilih surah baru.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        Toast.makeText(requireContext(), "Pilih surah untuk diputar", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                ContextCompat.startForegroundService(requireContext(), serviceIntent);
            } else {
                Toast.makeText(requireContext(), "Pilih surah untuk diputar", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnPreviousMurottal.setOnClickListener(v -> playPreviousSurah());
        binding.btnNextMurottal.setOnClickListener(v -> playNextSurah());
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
                                Log.d("MurottalFragment", "Jumlah surah diterima: " + surahs.size());
                                for (SurahResponse.Surah surah : surahs) {
                                    // --- KOREKSI PENTING DI SINI: TIDAK PERLU SET audioUrlAlafasy di dalam loop ini ---
                                    // Karena kita akan mengambilnya langsung dari audioFull saat item diklik/diputar.
                                    // Logging untuk debugging tetap bisa dipertahankan.
                                    Map<String, String> audioFullMap = surah.getAudioFull();
                                    if (audioFullMap != null) {
                                        Log.d("MurottalFragment", "Surah " + surah.getNomor() + " AudioFull Map: " + audioFullMap.toString());
                                        String alafasyUrl = audioFullMap.get("05"); // Langsung coba ambil
                                        if (alafasyUrl != null && !alafasyUrl.isEmpty()) {
                                            // Jangan set surah.setAudioUrlAlafasy(alafasyUrl); lagi
                                            // Cukup log saja jika URL ditemukan.
                                            Log.d("MurottalFragment", "Surah " + surah.getNomor() + " Alafasy URL DITEMUKAN (dari Map): " + alafasyUrl);
                                        } else {
                                            Log.w("MurottalFragment", "Audio Alafasy (key '05') adalah NULL atau KOSONG di audioFull untuk surah " + surah.getNomor());
                                        }
                                    } else {
                                        Log.w("MurottalFragment", "AudioFull map adalah NULL untuk surah " + surah.getNomor());
                                    }
                                    // --- AKHIR KOREKSI ---
                                }
                                surahAdapter.setSurahList(surahs); // Set list ke adapter
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
        // --- KOREKSI PENTING DI SINI: LANGSUNG AMBIL DARI AUDIOFULL ---
        Map<String, String> audioFullMap = surah.getAudioFull();
        if (audioFullMap != null) {
            String audioUrl = audioFullMap.get("05"); // Langsung ambil dari map yang diparsing Gson
            if (audioUrl != null && !audioUrl.isEmpty()) {
                Intent serviceIntent = new Intent(requireContext(), MurottalPlaybackService.class);
                serviceIntent.setAction(MurottalPlaybackService.ACTION_PLAY);
                serviceIntent.putExtra(MurottalPlaybackService.EXTRA_AUDIO_URL, audioUrl);
                ContextCompat.startForegroundService(requireContext(), serviceIntent);

                currentPlayingSurahNumber = surah.getNomor();
                binding.tvCurrentPlaying.setText("Q.S " + surah.getNamaLatin());
                binding.btnPlayPauseMurottal.setImageResource(R.drawable.ic_pause);
            } else {
                Toast.makeText(requireContext(), "URL Audio Alafasy tidak ditemukan atau kosong untuk surah ini.", Toast.LENGTH_SHORT).show();
                Log.w("MurottalFragment", "Audio URL Alafasy kosong (dari map) untuk surah " + surah.getNomor() + ". Map keys: " + audioFullMap.keySet());
            }
        } else {
            Toast.makeText(requireContext(), "Audio murottal untuk surah ini tidak tersedia (Map kosong).", Toast.LENGTH_SHORT).show();
            Log.w("MurottalFragment", "AudioFull map adalah NULL untuk surah " + surah.getNomor());
        }
        // --- AKHIR KOREKSI ---
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