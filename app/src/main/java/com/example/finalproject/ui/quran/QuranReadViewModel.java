package com.example.finalproject.ui.quran;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.finalproject.data.entity.QuranReadLog;
import com.example.finalproject.data.repository.QuranRepository;

import java.util.List;

public class QuranReadViewModel extends AndroidViewModel {

    private QuranRepository quranRepository;
    private LiveData<List<QuranReadLog>> allReadLogs;

    public QuranReadViewModel(@NonNull Application application) {
        super(application);
        quranRepository = new QuranRepository(application);
        allReadLogs = quranRepository.getAllReadLogs();
    }

    public LiveData<List<QuranReadLog>> getAllReadLogs() {
        return allReadLogs;
    }

    public LiveData<Integer> getTotalAyahsReadOnDate(String date) {
        return quranRepository.getTotalAyahsReadOnDate(date);
    }

    public void insert(QuranReadLog log) {
        quranRepository.insert(log);
    }

    public void delete(QuranReadLog log) {
        quranRepository.delete(log);
    }

    public void deleteAllLogs() {
        quranRepository.deleteAllLogs();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        quranRepository.shutdownExecutor();
    }
}
