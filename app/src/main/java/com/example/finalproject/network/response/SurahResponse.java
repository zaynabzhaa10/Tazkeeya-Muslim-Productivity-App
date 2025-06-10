package com.example.finalproject.network.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class SurahResponse {
    @SerializedName("code")
    private int code;
    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private List<Surah> data;

    // Getters dan Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Surah> getData() {
        return data;
    }

    public void setData(List<Surah> data) {
        this.data = data;
    }

    public static class Surah {
        @SerializedName("nomor")
        private int nomor;
        @SerializedName("nama")
        private String nama;
        @SerializedName("namaLatin")
        private String namaLatin;
        @SerializedName("jumlahAyat")
        private int jumlahAyat;
        @SerializedName("tempatTurun")
        private String tempatTurun;
        @SerializedName("arti")
        private String arti;
        @SerializedName("deskripsi")
        private String deskripsi;
        @SerializedName("audioFull")
        private Map<String, String> audioFull;

        // Getter dan Setter
        public int getNomor() {
            return nomor;
        }

        public void setNomor(int nomor) {
            this.nomor = nomor;
        }

        public String getNama() {
            return nama;
        }

        public void setNama(String nama) {
            this.nama = nama;
        }

        public String getNamaLatin() {
            return namaLatin;
        }

        public void setNamaLatin(String namaLatin) {
            this.namaLatin = namaLatin;
        }

        public int getJumlahAyat() {
            return jumlahAyat;
        }

        public void setJumlahAyat(int jumlahAyat) {
            this.jumlahAyat = jumlahAyat;
        }

        public String getTempatTurun() {
            return tempatTurun;
        }

        public void setTempatTurun(String tempatTurun) {
            this.tempatTurun = tempatTurun;
        }

        public String getArti() {
            return arti;
        }

        public void setArti(String arti) {
            this.arti = arti;
        }

        public String getDeskripsi() {
            return deskripsi;
        }

        public void setDeskripsi(String deskripsi) {
            this.deskripsi = deskripsi;
        }

        public Map<String, String> getAudioFull() {
            return audioFull;
        }

        public void setAudioFull(Map<String, String> audioFull) {
            this.audioFull = audioFull;
        }

    }
}
