package com.example.finalproject.network.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class AyahResponse {
    @SerializedName("code")
    private int code;
    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private SurahDetail data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public SurahDetail getData() { return data; }
    public void setData(SurahDetail data) { this.data = data; }

    public static class SurahDetail {
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
        @SerializedName("arti") // Arti surah
        private String arti;
        @SerializedName("deskripsi")
        private String deskripsi;
        @SerializedName("audioFull")
        private Map<String, String> audioFull;
        @SerializedName("ayat")
        private List<Ayah> ayat;

        // Getters dan Setters
        public int getNomor() { return nomor; }
        public String getNama() { return nama; }
        public void setNama(String nama) { this.nama = nama; }
        public String getNamaLatin() { return namaLatin; }
        public void setNamaLatin(String namaLatin) { this.namaLatin = namaLatin; }
        public int getJumlahAyat() { return jumlahAyat; }
        public void setJumlahAyat(int jumlahAyat) { this.jumlahAyat = jumlahAyat; }
        public String getTempatTurun() { return tempatTurun; }
        public void setTempatTurun(String tempatTurun) { this.tempatTurun = tempatTurun; }
        public String getArti() { return arti; }
        public void setArti(String arti) { this.arti = arti; }
        public String getDeskripsi() { return deskripsi; }
        public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
        public Map<String, String> getAudioFull() { return audioFull; }
        public void setAudioFull(Map<String, String> audioFull) { this.audioFull = audioFull; }
        public List<Ayah> getAyat() { return ayat; }
        public void setAyat(List<Ayah> ayat) { this.ayat = ayat; }
    }

    public static class Ayah {
        @SerializedName("id")
        private int id;
        @SerializedName("surah")
        private int surah;
        @SerializedName("nomorAyat")
        private int nomor;
        @SerializedName("teksArab")
        private String ar;
        @SerializedName("teksLatin")
        private String tr;
        @SerializedName("teksIndonesia")
        private String idn;
        @SerializedName("audio")
        private Map<String, String> audio;

        // Getters dan Setters
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getSurah() {
            return surah;
        }
        public void setSurah(int surah) {
            this.surah = surah;
        }

        public int getNomor() {
            return nomor;
        }

        public void setNomor(int nomor) {
            this.nomor = nomor;
        }

        public String getAr() {
            return ar;
        }

        public void setAr(String ar) {
            this.ar = ar;
        }

        public String getTr() {
            return tr;
        }

        public void setTr(String tr) {
            this.tr = tr;
        }

        public String getIdn() {
            return idn;
        }

        public void setIdn(String idn) {
            this.idn = idn;
        }

        public Map<String, String> getAudio() {
            return audio;
        }

        public void setAudio(Map<String, String> audio) {
            this.audio = audio;
        }
    }
}