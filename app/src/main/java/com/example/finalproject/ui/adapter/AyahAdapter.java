package com.example.finalproject.ui.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.network.response.AyahResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AyahAdapter extends RecyclerView.Adapter<AyahAdapter.AyahViewHolder> {

    private List<AyahResponse.Ayah> ayahList;
    private OnPlayClickListener listener;

    public interface OnPlayClickListener {
        void onPlayClick(String audioUrl);
    }

    public AyahAdapter() {
        this.ayahList = new ArrayList<>();
    }

    public void setAyahList(List<AyahResponse.Ayah> ayahList) {
        this.ayahList = ayahList;
        notifyDataSetChanged();
    }

    public void setOnPlayClickListener(OnPlayClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AyahViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ayah, parent, false);
        return new AyahViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AyahViewHolder holder, int position) {
        AyahResponse.Ayah ayah = ayahList.get(position);

        holder.tvAyahNumber.setText("Ayat " + ayah.getNomor());

        // Teks Arab
        if (ayah.getAr() != null && !ayah.getAr().isEmpty()) {
            holder.tvAyahText.setText(ayah.getAr());
            Log.d("AyahAdapter", "Ayat " + ayah.getNomor() + " (Arab): " + ayah.getAr().substring(0, Math.min(ayah.getAr().length(), 50)) + "...");
            holder.tvAyahText.setVisibility(View.VISIBLE);
        } else {
            holder.tvAyahText.setText("Teks Arab tidak tersedia."); // Ganti dengan string kosong jika tidak ingin menampilkan pesan ini
            Log.e("AyahAdapter", "Teks Arab kosong atau null untuk ayat " + ayah.getNomor());
            // Toast.makeText(holder.itemView.getContext(), "Teks Arab ayat " + ayah.getNomor() + " kosong!", Toast.LENGTH_SHORT).show(); // Komentar/hapus jika tidak ingin toast
            holder.tvAyahText.setVisibility(View.GONE); // Sembunyikan jika kosong
        }

        // --- TAMBAHAN BARU DI onBindViewHolder ---
        // Cara Baca Latin
        if (ayah.getTr() != null && !ayah.getTr().isEmpty()) {
            holder.tvAyahLatin.setText(ayah.getTr());
            holder.tvAyahLatin.setVisibility(View.VISIBLE);
        } else {
            holder.tvAyahLatin.setVisibility(View.GONE);
        }

        // Terjemahan Indonesia
        if (ayah.getIdn() != null && !ayah.getIdn().isEmpty()) {
            holder.tvAyahTranslation.setText(ayah.getIdn());
            holder.tvAyahTranslation.setVisibility(View.VISIBLE);
        } else {
            holder.tvAyahTranslation.setVisibility(View.GONE);
        }
        // --- AKHIR TAMBAHAN BARU ---

        // Penanganan tombol play audio
        holder.btnPlayAyah.setOnClickListener(v -> {
            Map<String, String> audioMap = ayah.getAudio();
            if (listener != null && audioMap != null && audioMap.containsKey("05")) {
                String audioUrl = audioMap.get("05");
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    listener.onPlayClick(audioUrl);
                    Log.d("AyahAdapter", "Memutar audio: " + audioUrl);
                } else {
                    Toast.makeText(holder.itemView.getContext(), "URL Audio Alafasy kosong.", Toast.LENGTH_SHORT).show();
                    Log.w("AyahAdapter", "URL Audio Alafasy kosong untuk ayat " + ayah.getNomor());
                }
            } else {
                Toast.makeText(holder.itemView.getContext(), "Audio untuk ayat ini tidak tersedia.", Toast.LENGTH_SHORT).show();
                Log.w("AyahAdapter", "Audio Alafasy tidak tersedia untuk ayat " + ayah.getNomor() + ". Map: " + audioMap);
            }
        });
    }

    @Override
    public int getItemCount() {
        return ayahList != null ? ayahList.size() : 0;
    }

    static class AyahViewHolder extends RecyclerView.ViewHolder {
        TextView tvAyahNumber;
        TextView tvAyahText;
        // --- TAMBAHAN BARU DI ViewHolder ---
        TextView tvAyahLatin;
        TextView tvAyahTranslation;
        // --- AKHIR TAMBAHAN BARU ---
        ImageView btnPlayAyah;

        @SuppressLint("WrongViewCast")
        AyahViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAyahNumber = itemView.findViewById(R.id.tvAyahNumber);
            tvAyahText = itemView.findViewById(R.id.tvAyahText);
            // --- INISIALISASI VIEW BARU ---
            tvAyahLatin = itemView.findViewById(R.id.tvAyahLatin);
            tvAyahTranslation = itemView.findViewById(R.id.tvAyahTranslation);
            // --- AKHIR INISIALISASI ---
            btnPlayAyah = itemView.findViewById(R.id.btnPlayAyah);
        }
    }
}