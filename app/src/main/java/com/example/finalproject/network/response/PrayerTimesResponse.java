package com.example.finalproject.network.response;

import com.google.gson.annotations.SerializedName;

public class PrayerTimesResponse {
    @SerializedName("data")
    private PrayerTimesData data;

    public PrayerTimesData getData() {
        return data;
    }

    public void setData(PrayerTimesData data) {
        this.data = data;
    }

    public static class PrayerTimesData {
        @SerializedName("timings")
        private Timings timings;
        @SerializedName("date")
        private PrayerDate date;
        @SerializedName("meta")
        private PrayerMeta meta;

        public Timings getTimings() {
            return timings;
        }

        public void setTimings(Timings timings) {
            this.timings = timings;
        }

        public PrayerDate getDate() {
            return date;
        }

        public void setDate(PrayerDate date) {
            this.date = date;
        }

        public PrayerMeta getMeta() {
            return meta;
        }

        public void setMeta(PrayerMeta meta) {
            this.meta = meta;
        }
    }

    public static class Timings {
        @SerializedName("Fajr")
        private String fajr;
        @SerializedName("Dhuhr")
        private String dhuhr;
        @SerializedName("Asr")
        private String asr;
        @SerializedName("Maghrib")
        private String maghrib;
        @SerializedName("Isha")
        private String isha;
        @SerializedName("Sunrise")
        private String sunrise;
        @SerializedName("Sunset")
        private String sunset;
        @SerializedName("Imsak")
        private String imsak;
        @SerializedName("Midnight")
        private String midnight;

        public String getFajr() { return fajr; }
        public String getDhuhr() { return dhuhr; }
        public String getAsr() { return asr; }
        public String getMaghrib() { return maghrib; }
        public String getIsha() { return isha; }
        public String getSunrise() { return sunrise; }
        public String getSunset() { return sunset; }
        public String getImsak() { return imsak; }
        public String getMidnight() { return midnight; }
    }

    public static class PrayerDate {
        @SerializedName("readable")
        private String readable;
        @SerializedName("gregorian")
        private GregorianDate gregorian;
        @SerializedName("hijri")
        private HijriDate hijri;

        public String getReadable() { return readable; }
        public GregorianDate getGregorian() { return gregorian; }
        public HijriDate getHijri() { return hijri; }
    }

    public static class GregorianDate {
        @SerializedName("date")
        private String date; // e.g., "07-06-2025"
        @SerializedName("weekday")
        private Weekday weekday;
        @SerializedName("month")
        private Month month;
        @SerializedName("year")
        private String year;

        public String getDate() { return date; }
        public Weekday getWeekday() { return weekday; }
        public Month getMonth() { return month; }
        public String getYear() { return year; }
    }

    public static class Weekday {
        @SerializedName("en")
        private String en;
        public String getEn() { return en; }
    }

    public static class Month {
        @SerializedName("en")
        private String en;
        public String getEn() { return en; }
    }

    public static class HijriDate {
        @SerializedName("date")
        private String date;
        @SerializedName("month")
        private Month month;
        @SerializedName("year")
        private String year;
        public String getDate() { return date; }
        public Month getMonth() { return month; }
        public String getYear() { return year; }
    }

    public static class PrayerMeta {
        @SerializedName("timezone")
        private String timezone;
        @SerializedName("latitude")
        private double latitude;
        @SerializedName("longitude")
        private double longitude;
        // ... tambahkan properti lain jika diperlukan
        public String getTimezone() { return timezone; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
}