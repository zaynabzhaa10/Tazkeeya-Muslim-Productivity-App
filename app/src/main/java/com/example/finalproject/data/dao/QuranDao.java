package com.example.finalproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.finalproject.data.entity.QuranReadLog;

import java.util.List;

@Dao
public interface QuranDao {
    @Insert
    void insert(QuranReadLog log);

    @Delete
    void delete(QuranReadLog log);

    @Query("DELETE FROM quran_read_log")
    void deleteAllLogs();

    @Query("SELECT * FROM quran_read_log ORDER BY read_date DESC, id DESC")
    LiveData<List<QuranReadLog>> getAllReadLogs();

    @Query("SELECT SUM(ayahs_count) FROM quran_read_log WHERE read_date = :date")
    LiveData<Integer> getTotalAyahsReadOnDate(String date);
}