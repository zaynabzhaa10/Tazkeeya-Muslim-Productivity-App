package com.example.finalproject.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.finalproject.data.dao.QuranDao;
import com.example.finalproject.data.database.AppDatabase;
import com.example.finalproject.data.entity.QuranReadLog;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuranRepository {

    private QuranDao quranDao;
    private LiveData<List<QuranReadLog>> allReadLogs;
    private ExecutorService executorService;

    public QuranRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        quranDao = db.quranDao();
        allReadLogs = quranDao.getAllReadLogs();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<QuranReadLog>> getAllReadLogs() {
        return allReadLogs;
    }

    public LiveData<Integer> getTotalAyahsReadOnDate(String date) {
        return quranDao.getTotalAyahsReadOnDate(date);
    }

    public void insert(QuranReadLog log) {
        executorService.execute(() -> quranDao.insert(log));
    }

    public void delete(QuranReadLog log) {
        executorService.execute(() -> quranDao.delete(log));
    }

    public void deleteAllLogs() {
        executorService.execute(() -> quranDao.deleteAllLogs());
    }

    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
