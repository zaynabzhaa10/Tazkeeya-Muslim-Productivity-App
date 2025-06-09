package com.example.finalproject.ui.readlog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.data.entity.QuranReadLog;
import com.example.finalproject.databinding.FragmentReadLogListBinding;
import com.example.finalproject.ui.quran.QuranReadViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReadLogListFragment extends Fragment {

    private FragmentReadLogListBinding binding;
    private QuranReadViewModel quranReadViewModel;
    private ReadLogAdapter readLogAdapter;
    private SharedPreferences quranPrefs;

    private static final String PREF_DAILY_TARGET = "daily_quran_target";
    private int dailyQuranTarget;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReadLogListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        quranReadViewModel = new ViewModelProvider(requireActivity()).get(QuranReadViewModel.class);
        quranPrefs = requireActivity().getSharedPreferences("QuranReadingPrefs", Context.MODE_PRIVATE);

        dailyQuranTarget = quranPrefs.getInt(PREF_DAILY_TARGET, 50);

        setupRecyclerView();
        observeReadLogs();
        observeDailyTarget();

        binding.btnEditTarget.setOnClickListener(v -> showEditTargetDialog());

        // --- LISTENER UNTUK TOMBOL HAPUS SEMUA RIWAYAT ---
        binding.btnDeleteAllLogs.setOnClickListener(v -> showConfirmDeleteAllDialog());
        // --- AKHIR LISTENER ---
    }

    private void setupRecyclerView() {
        // --- PASS ViewModel ke Adapter agar bisa melakukan operasi delete ---
        readLogAdapter = new ReadLogAdapter(quranReadViewModel);
        binding.rvReadLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReadLogs.setAdapter(readLogAdapter);
    }

    private void observeReadLogs() {
        quranReadViewModel.getAllReadLogs().observe(getViewLifecycleOwner(), new Observer<List<QuranReadLog>>() {
            @Override
            public void onChanged(List<QuranReadLog> quranReadLogs) {
                readLogAdapter.setReadLogs(quranReadLogs);
                binding.tvNoLogs.setVisibility(quranReadLogs != null && quranReadLogs.isEmpty() ? View.VISIBLE : View.GONE);
                // Sembunyikan/tampilkan tombol "Hapus Semua" jika tidak ada riwayat
                binding.btnDeleteAllLogs.setVisibility(quranReadLogs != null && quranReadLogs.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void observeDailyTarget() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        quranReadViewModel.getTotalAyahsReadOnDate(currentDate).observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer totalAyahsToday) {
                int currentRead = (totalAyahsToday != null) ? totalAyahsToday : 0;
                binding.tvDailyTargetStatus.setText("Target Harian: " + currentRead + " / " + dailyQuranTarget + " Ayat");
                updateAppreciationMessage(currentRead);
            }
        });
    }

    private void showEditTargetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ubah Target Bacaan Harian");
        builder.setMessage("Masukkan jumlah ayat targetmu hari ini:");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(dailyQuranTarget));
        builder.setView(input);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String inputText = input.getText().toString();
            if (TextUtils.isEmpty(inputText)) {
                Toast.makeText(requireContext(), "Target tidak boleh kosong.", Toast.LENGTH_SHORT).show();
                return;
            }
            int newTarget = Integer.parseInt(inputText);
            if (newTarget <= 0) {
                Toast.makeText(requireContext(), "Target harus lebih dari 0.", Toast.LENGTH_SHORT).show();
                return;
            }

            dailyQuranTarget = newTarget;
            quranPrefs.edit().putInt(PREF_DAILY_TARGET, dailyQuranTarget).apply();
            Toast.makeText(requireContext(), "Target harian berhasil diubah!", Toast.LENGTH_SHORT).show();

            observeDailyTarget();
            dialog.dismiss();
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateAppreciationMessage(int totalAyahsToday) {
        String message;
        int target = dailyQuranTarget;

        if (totalAyahsToday >= target * 2) {
            message = "Maa syaa Allah! Kamu luar biasa melampaui target bacaan hari ini! Semoga Allah memberkahimu selalu.";
        } else if (totalAyahsToday >= target) {
            message = "Alhamdulillah! Kamu mencapai target bacaan Al-Qur'anmu hari ini! Pertahankan istiqamah ini.";
        } else if (totalAyahsToday > 0) {
            message = "Baarakallahu fiik, kamu sudah membaca " + totalAyahsToday + " ayat hari ini. Semoga Allah mudahkan besok bisa mencapai target " + target + " ayat yah!";
        } else {
            message = "Ayo mulai membaca Al-Qur'an hari ini! Setiap huruf adalah pahala.";
        }
        binding.tvAppreciation.setText(message);
    }

    // --- METODE BARU: Dialog Konfirmasi Hapus Semua Riwayat ---
    private void showConfirmDeleteAllDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus Semua Riwayat?")
                .setMessage("Apakah Anda yakin ingin menghapus semua catatan bacaan Al-Qur'an Anda? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    quranReadViewModel.deleteAllLogs();
                    Toast.makeText(requireContext(), "Semua riwayat bacaan telah dihapus.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.cancel())
                .show();
    }
    // --- AKHIR METODE BARU ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Adapter untuk RecyclerView Riwayat Bacaan (MODIFIKASI) ---
    private static class ReadLogAdapter extends RecyclerView.Adapter<ReadLogAdapter.ReadLogViewHolder> {

        private List<QuranReadLog> readLogs;
        // --- TAMBAHAN BARU: Referensi ke ViewModel untuk delete ---
        private QuranReadViewModel quranReadViewModel;

        public ReadLogAdapter(QuranReadViewModel quranReadViewModel) {
            this.quranReadViewModel = quranReadViewModel;
            this.readLogs = new ArrayList<>(); // Pastikan diinisialisasi
        }

        public void setReadLogs(List<QuranReadLog> readLogs) {
            this.readLogs = readLogs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ReadLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_read_log, parent, false);
            return new ReadLogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReadLogViewHolder holder, int position) {
            QuranReadLog log = readLogs.get(position);
            holder.tvLogDate.setText(log.getReadDate());
            holder.tvLogDetails.setText(log.getSurahName() + " (" + log.getStartAyah() + "-" + log.getEndAyah() + ")");
            holder.tvLogAyatCount.setText(log.getAyahsCount() + " Ayat");

            // --- LISTENER UNTUK TOMBOL DELETE PER ITEM ---
            holder.btnDeleteLogItem.setOnClickListener(v -> {
                // Konfirmasi penghapusan satu item
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Hapus Riwayat Ini?")
                        .setMessage("Apakah Anda yakin ingin menghapus catatan bacaan ini?")
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            quranReadViewModel.delete(log); // Panggil metode delete di ViewModel
                            Toast.makeText(v.getContext(), "Catatan dihapus.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .setNegativeButton("Batal", (dialog, which) -> dialog.cancel())
                        .show();
            });
            // --- AKHIR LISTENER ---
        }

        @Override
        public int getItemCount() {
            return readLogs != null ? readLogs.size() : 0;
        }

        static class ReadLogViewHolder extends RecyclerView.ViewHolder {
            TextView tvLogDate;
            TextView tvLogDetails;
            TextView tvLogAyatCount;
            ImageView btnDeleteLogItem;

            @SuppressLint("WrongViewCast")
            ReadLogViewHolder(@NonNull View itemView) {
                super(itemView);
                tvLogDate = itemView.findViewById(R.id.tvLogDate);
                tvLogDetails = itemView.findViewById(R.id.tvLogDetails);
                tvLogAyatCount = itemView.findViewById(R.id.tvLogAyatCount);
                btnDeleteLogItem = itemView.findViewById(R.id.btnDeleteLogItem); // Inisialisasi ImageButton
            }
        }
    }
}