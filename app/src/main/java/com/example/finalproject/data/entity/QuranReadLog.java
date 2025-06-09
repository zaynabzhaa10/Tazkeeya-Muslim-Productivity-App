package com.example.finalproject.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quran_read_log")
public class QuranReadLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "read_date")
    public String readDate; // Format: YYYY-MM-DD
    @ColumnInfo(name = "surah_number")
    public int surahNumber;
    @ColumnInfo(name = "surah_name")
    public String surahName;
    @ColumnInfo(name = "start_ayah")
    public int startAyah;
    @ColumnInfo(name = "end_ayah")
    public int endAyah;
    @ColumnInfo(name = "ayahs_count")
    public int ayahsCount;

    // Constructor
    public QuranReadLog(String readDate, int surahNumber, String surahName, int startAyah, int endAyah, int ayahsCount) {
        this.readDate = readDate;
        this.surahNumber = surahNumber;
        this.surahName = surahName;
        this.startAyah = startAyah;
        this.endAyah = endAyah;
        this.ayahsCount = ayahsCount;
    }

    // Getters
    public int getId() { return id; }
    public String getReadDate() { return readDate; }
    public int getSurahNumber() { return surahNumber; }
    public String getSurahName() { return surahName; }
    public int getStartAyah() { return startAyah; }
    public int getEndAyah() { return endAyah; }
    public int getAyahsCount() { return ayahsCount; }

    // Setters (opsional, tergantung kebutuhan)
    public void setId(int id) { this.id = id; }
    public void setReadDate(String readDate) { this.readDate = readDate; }
    public void setSurahNumber(int surahNumber) { this.surahNumber = surahNumber; }
    public void setSurahName(String surahName) { this.surahName = surahName; }
    public void setStartAyah(int startAyah) { this.startAyah = startAyah; }
    public void setEndAyah(int endAyah) { this.endAyah = endAyah; }
    public void setAyahsCount(int ayahsCount) { this.ayahsCount = ayahsCount; }
}
