package com.example.finalproject.network.api;

import com.example.finalproject.network.response.AyahResponse;
import com.example.finalproject.network.response.PrayerTimesResponse;
import com.example.finalproject.network.response.SurahResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Endpoint untuk waktu sholat (Aladhan API)
    @GET("timingsByCity")
    Call<PrayerTimesResponse> getPrayerTimesByCity(
            @Query("city") String city,
            @Query("country") String country,
            @Query("method") int method
    );

    @GET("timings/{timestamp}")
    Call<PrayerTimesResponse> getPrayerTimesByCoordinates(
            @Path("timestamp") long timestamp, // Unix timestamp
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("method") int method
    );

    // Endpoint untuk daftar surah (Equran.id API)
    @GET("surat")
    Call<SurahResponse> getAllSurahs();

    // Endpoint untuk detail surah dan ayat-ayatnya (Equran.id API)
    // API ini buat murottal per ayat di properti 'audio'
    @GET("surat/{nomor}")
    Call<AyahResponse> getAyahsOfSurah(@Path("nomor") int surahNumber);

}