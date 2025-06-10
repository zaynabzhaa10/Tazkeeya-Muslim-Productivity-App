package com.example.finalproject.network.client;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofitAladhan = null;
    private static Retrofit retrofitEquran = null;

    private static final String ALADHAN_BASE_URL = "https://api.aladhan.com/v1/";
    private static final String EQURAN_BASE_URL = "https://equran.id/api/v2/";

    private static OkHttpClient createOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    public static Retrofit getAladhanClient() {
        if (retrofitAladhan == null) {
            retrofitAladhan = new Retrofit.Builder()
                    .baseUrl(ALADHAN_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(createOkHttpClient()) // Gunakan OkHttpClient yang sudah dibuat
                    .build();
        }
        return retrofitAladhan;
    }

    public static Retrofit getEquranClient() {
        if (retrofitEquran == null) {
            retrofitEquran = new Retrofit.Builder()
                    .baseUrl(EQURAN_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(createOkHttpClient()) // Gunakan OkHttpClient yang sudah dibuat
                    .build();
        }
        return retrofitEquran;
    }
}
