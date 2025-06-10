package com.example.finalproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.network.response.SurahResponse;

import java.util.ArrayList;
import java.util.List;

public class SurahAdapter extends RecyclerView.Adapter<SurahAdapter.SurahViewHolder> {

    private List<SurahResponse.Surah> surahList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SurahResponse.Surah surah);
    }

    public SurahAdapter(OnItemClickListener listener) {
        this.surahList = new ArrayList<>();
        this.listener = listener;
    }

    public void setSurahList(List<SurahResponse.Surah> surahList) {
        this.surahList = surahList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SurahViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surah, parent, false);
        return new SurahViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurahViewHolder holder, int position) {
        SurahResponse.Surah surah = surahList.get(position);
        holder.tvSurahNumber.setText(String.valueOf(surah.getNomor()) + ".");
        holder.tvSurahName.setText(surah.getNamaLatin() + " (" + surah.getArti() + ")");
        holder.tvAyahsCount.setText(surah.getJumlahAyat() + " Ayat, " + surah.getTempatTurun());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(surah);
            }
        });
    }

    @Override
    public int getItemCount() {
        return surahList.size();
    }
    public List<SurahResponse.Surah> getSurahList() {
        return surahList;
    }

    static class SurahViewHolder extends RecyclerView.ViewHolder {
        TextView tvSurahNumber;
        TextView tvSurahName;
        TextView tvAyahsCount;

        SurahViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSurahNumber = itemView.findViewById(R.id.tvSurahNumber);
            tvSurahName = itemView.findViewById(R.id.tvSurahName);
            tvAyahsCount = itemView.findViewById(R.id.tvAyahsCount);
        }
    }
}