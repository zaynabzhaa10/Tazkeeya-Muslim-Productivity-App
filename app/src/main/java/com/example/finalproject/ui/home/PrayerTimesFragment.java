package com.example.finalproject.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.example.finalproject.alarm.AdzanBroadcastReceiver;
import com.example.finalproject.network.response.PrayerTimesResponse;
import com.example.finalproject.databinding.FragmentPrayerTimesBinding;
import com.example.finalproject.network.api.ApiService;
import com.example.finalproject.network.client.RetrofitClient;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.provider.Settings;
import android.app.AlertDialog;
import android.net.Uri;
import java.util.TimeZone;
import android.location.Geocoder;
import android.location.Geocoder;
import java.util.List;
import android.location.Address;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrayerTimesFragment extends Fragment {

    private FragmentPrayerTimesBinding binding;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ExecutorService executorService;
    private Handler mainHandler;

    private Handler clockHandler;
    private Runnable clockRunnable;
    private PrayerTimesResponse.PrayerTimesData currentPrayerTimesData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPrayerTimesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        clockHandler = new Handler(Looper.getMainLooper());
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateDigitalClockAndCountdown();
                clockHandler.postDelayed(this, 1000);
            }
        };

        binding.btnRefreshPrayer.setOnClickListener(v -> checkLocationPermissionAndFetchPrayerTimes());

        clockHandler.post(clockRunnable);
        checkLocationPermissionAndFetchPrayerTimes();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.post(clockRunnable);
        }
        if (currentPrayerTimesData != null) {
            updateDigitalClockAndCountdown();
        } else {
            checkLocationPermissionAndFetchPrayerTimes();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }

    private void checkLocationPermissionAndFetchPrayerTimes() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdatesAndFetchPrayerTimes();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdatesAndFetchPrayerTimes() {
        binding.progressBarPrayer.setVisibility(View.VISIBLE);
        binding.prayerTimesContainer.setVisibility(View.GONE);
        binding.btnRefreshPrayer.setVisibility(View.GONE);
        binding.tvLocation.setText("Mendapatkan lokasi...");
        binding.tvCountdownToNextPrayer.setText("Memuat waktu sholat...");

        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            showError("Layanan lokasi tidak tersedia.");
            binding.progressBarPrayer.setVisibility(View.GONE);
            binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
            binding.tvLocation.setText("Gagal mendapatkan lokasi: Layanan tidak tersedia.");
            return;
        }

        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            showError("Lokasi dinonaktifkan. Silakan aktifkan di pengaturan perangkat.");
            binding.tvLocation.setText("Lokasi dinonaktifkan.");
            binding.progressBarPrayer.setVisibility(View.GONE);
            binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
            return;
        }

        Location lastKnownLocation = null;
        try {
            if (isNetworkEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKnownLocation == null && isGpsEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException e) {
            Log.e("PrayerTimesFragment", "Izin lokasi tidak diberikan", e);
            showError("Aplikasi tidak memiliki izin lokasi.");
            binding.tvLocation.setText("Gagal mendapatkan lokasi: Izin ditolak.");
            binding.progressBarPrayer.setVisibility(View.GONE);
            binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
            return;
        }

        if (lastKnownLocation != null) {
            Log.d("PrayerTimesFragment", "Lokasi terakhir diketahui: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
            getAndDisplayLocationName(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()); // **[M] Diubah: Panggil metode baru untuk mendapatkan nama lokasi**
            fetchPrayerTimes(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        } else {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    locationManager.removeUpdates(this);
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("PrayerTimesFragment", "Lokasi ditemukan: " + latitude + ", " + longitude);
                    getAndDisplayLocationName(latitude, longitude); // **[M] Diubah: Panggil metode baru untuk mendapatkan nama lokasi**
                    fetchPrayerTimes(latitude, longitude);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(@NonNull String provider) {}
                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    mainHandler.post(() -> {
                        showError("Layanan lokasi dinonaktifkan. Silakan aktifkan di pengaturan.");
                        binding.tvLocation.setText("Layanan lokasi dinonaktifkan.");
                        binding.progressBarPrayer.setVisibility(View.GONE);
                        binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
                    });
                }
            };

            try {
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 10, 10, locationListener); // **[M] Diubah: Interval 10 detik**
                } else if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 10, 10, locationListener); // **[M] Diubah: Interval 10 detik**
                }

                mainHandler.postDelayed(() -> {
                    if (binding.progressBarPrayer.getVisibility() == View.VISIBLE) {
                        if (locationManager != null && locationListener != null) {
                            locationManager.removeUpdates(locationListener);
                        }
                        showError("Gagal mendapatkan lokasi: Waktu habis.");
                        binding.tvLocation.setText("Gagal mendapatkan lokasi.");
                        binding.progressBarPrayer.setVisibility(View.GONE);
                        binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
                    }
                }, 20000);
            } catch (SecurityException e) {
                Log.e("PrayerTimesFragment", "Izin lokasi tidak diberikan", e);
                showError("Aplikasi tidak memiliki izin lokasi.");
                binding.tvLocation.setText("Gagal mendapatkan lokasi: Izin ditolak.");
                binding.progressBarPrayer.setVisibility(View.GONE);
                binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void getAndDisplayLocationName(double latitude, double longitude) {
        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String cityName = address.getLocality();
                    String subAdminArea = address.getSubAdminArea();
                    String countryName = address.getCountryName();

                    String locationText;
                    if (cityName != null && !cityName.isEmpty()) {
                        locationText = cityName;
                        if (subAdminArea != null && !subAdminArea.isEmpty() && !subAdminArea.equalsIgnoreCase(cityName)) {
                            locationText += ", " + subAdminArea;
                        }
                    } else if (subAdminArea != null && !subAdminArea.isEmpty()) {
                        locationText = subAdminArea;
                    } else {
                        locationText = "Lokasi tidak diketahui";
                    }

                    if (countryName != null && !countryName.isEmpty()) {
                        locationText += ", " + countryName;
                    }
                    final String finalLocationText = "\uD83D\uDCCD " + locationText;

                    mainHandler.post(() -> {
                        binding.tvLocation.setText(finalLocationText);
                    });
                } else {
                    mainHandler.post(() -> {
                        binding.tvLocation.setText("\uD83D\uDCCD Lokasi tidak ditemukan.");
                    });
                }
            } catch (IOException e) {
                Log.e("PrayerTimesFragment", "Error getting location name: " + e.getMessage());
                mainHandler.post(() -> {
                    binding.tvLocation.setText("\uD83D\uDCCD Gagal mendapatkan nama lokasi (cek koneksi internet).");
                });
            } catch (IllegalArgumentException e) {
                Log.e("PrayerTimesFragment", "Error getting location name (invalid latitude/longitude): " + e.getMessage());
                mainHandler.post(() -> {
                    binding.tvLocation.setText("\uD83D\uDCCD Koordinat lokasi tidak valid.");
                });
            }
        });
    }

    private void fetchPrayerTimes(double latitude, double longitude) {
        ApiService apiService = RetrofitClient.getAladhanClient().create(ApiService.class);
        Call<PrayerTimesResponse> call = apiService.getPrayerTimesByCity("Makassar", "Indonesia", 5);

        executorService.execute(() -> {
            call.enqueue(new Callback<PrayerTimesResponse>() {
                @Override
                public void onResponse(@NonNull Call<PrayerTimesResponse> call, @NonNull Response<PrayerTimesResponse> response) {
                    mainHandler.post(() -> {
                        binding.progressBarPrayer.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            currentPrayerTimesData = response.body().getData();
                            displayPrayerTimes(currentPrayerTimesData);
                            binding.prayerTimesContainer.setVisibility(View.VISIBLE);
                            schedulePrayerAlarms(currentPrayerTimesData);
                            updateDigitalClockAndCountdown();
                        } else {
                            showError("Gagal mengambil waktu sholat. " + response.message());
                            binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<PrayerTimesResponse> call, @NonNull Throwable t) {
                    mainHandler.post(() -> {
                        binding.progressBarPrayer.setVisibility(View.GONE);
                        showError("Kesalahan jaringan: " + t.getMessage());
                        binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
                    });
                }
            });
        });
    }

    private void displayPrayerTimes(PrayerTimesResponse.PrayerTimesData data) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        String readableDate = data.getDate().getReadable();
        String fullHijriDate = data.getDate().getHijri().getDate();
        String[] hijriParts = fullHijriDate.split("-");

        String hijriDay = hijriParts[0];
        String hijriMonth = data.getDate().getHijri().getMonth().getEn();
        String hijriYear = hijriParts[2];



        String hijriDate = hijriDay + " " + hijriMonth + " " + hijriYear + " H";
        binding.tvDate.setText(readableDate + "\n" + hijriDate);

        // Tampilkan waktu sholat
        binding.tvFajr.setText(data.getTimings().getFajr());
        binding.tvDhuhr.setText(data.getTimings().getDhuhr());
        binding.tvAsr.setText(data.getTimings().getAsr());
        binding.tvMaghrib.setText(data.getTimings().getMaghrib());
        binding.tvIsha.setText(data.getTimings().getIsha());
        binding.tvSunrise.setText(data.getTimings().getSunrise());
    }

    private void updateDigitalClockAndCountdown() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        binding.tvDigitalClock.setText(timeFormat.format(now.getTime()));

        if (currentPrayerTimesData != null) {
            String nextPrayerName = "Tidak Ada";
            long millisUntilNextPrayer = Long.MAX_VALUE;
            boolean foundNextPrayer = false;

            SimpleDateFormat prayerTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            TimeZone apiTimeZone = TimeZone.getTimeZone(currentPrayerTimesData.getMeta().getTimezone());
            prayerTimeFormat.setTimeZone(apiTimeZone);

            String apiReadableDate = currentPrayerTimesData.getDate().getReadable();
            SimpleDateFormat apiDateFormatForParsing = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            apiDateFormatForParsing.setTimeZone(apiTimeZone);

            Date parsedApiDate;
            Calendar apiDateCalendar = Calendar.getInstance(apiTimeZone);
            try {
                parsedApiDate = apiDateFormatForParsing.parse(apiReadableDate);
                apiDateCalendar.setTime(parsedApiDate);
            } catch (ParseException e) {
                Log.e("PrayerTimesFragment", "Error parsing readable API date: " + apiReadableDate, e);
                binding.tvCountdownToNextPrayer.setText("Error menghitung waktu sholat.");
                return;
            }


            String[] prayerNames = {"Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"};
            String[] prayerTimes = {
                    currentPrayerTimesData.getTimings().getFajr(),
                    currentPrayerTimesData.getTimings().getDhuhr(),
                    currentPrayerTimesData.getTimings().getAsr(),
                    currentPrayerTimesData.getTimings().getMaghrib(),
                    currentPrayerTimesData.getTimings().getIsha()
            };

            for (int i = 0; i < prayerNames.length; i++) {
                try {
                    Date prayerTime = prayerTimeFormat.parse(prayerTimes[i]);
                    Calendar prayerCalendar = Calendar.getInstance(apiTimeZone);
                    prayerCalendar.setTime(prayerTime);

                    prayerCalendar.set(Calendar.YEAR, apiDateCalendar.get(Calendar.YEAR));
                    prayerCalendar.set(Calendar.MONTH, apiDateCalendar.get(Calendar.MONTH));
                    prayerCalendar.set(Calendar.DAY_OF_MONTH, apiDateCalendar.get(Calendar.DAY_OF_MONTH));

                    long timeInMillis = prayerCalendar.getTimeInMillis();

                    if (timeInMillis > now.getTimeInMillis() + 1000) {
                        if (timeInMillis - now.getTimeInMillis() < millisUntilNextPrayer) {
                            millisUntilNextPrayer = timeInMillis - now.getTimeInMillis();
                            nextPrayerName = prayerNames[i];
                            foundNextPrayer = true;
                        }
                    }

                } catch (ParseException e) {
                    Log.e("PrayerTimesFragment", "Error parsing prayer time for countdown: " + prayerTimes[i], e);
                }
            }

            if (!foundNextPrayer) {
                try {
                    Date fajrTomorrow = prayerTimeFormat.parse(currentPrayerTimesData.getTimings().getFajr());
                    Calendar fajrCalendarTomorrow = Calendar.getInstance(apiTimeZone);
                    fajrCalendarTomorrow.setTime(fajrTomorrow);

                    fajrCalendarTomorrow.set(Calendar.YEAR, apiDateCalendar.get(Calendar.YEAR));
                    fajrCalendarTomorrow.set(Calendar.MONTH, apiDateCalendar.get(Calendar.MONTH));
                    fajrCalendarTomorrow.set(Calendar.DAY_OF_MONTH, apiDateCalendar.get(Calendar.DAY_OF_MONTH));
                    fajrCalendarTomorrow.add(Calendar.DATE, 1);

                    millisUntilNextPrayer = fajrCalendarTomorrow.getTimeInMillis() - now.getTimeInMillis();
                    nextPrayerName = prayerNames[0];
                } catch (ParseException e) {
                    Log.e("PrayerTimesFragment", "Error parsing Fajr time for tomorrow's countdown.", e);
                }
            }


            long totalSeconds = millisUntilNextPrayer / 1000;
            long days = totalSeconds / (24 * 3600);
            long hours = (totalSeconds % (24 * 3600)) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            String countdownText = "";
            if (days > 0) {
                countdownText += String.format(Locale.getDefault(), "%d hari ", days);
            }
            if (hours > 0 || days > 0) {
                countdownText += String.format(Locale.getDefault(), "%02d jam ", hours);
            }
            countdownText += String.format(Locale.getDefault(), "%02d menit %02d detik lagi menuju waktu sholat %s", minutes, seconds, nextPrayerName);

            binding.tvCountdownToNextPrayer.setText(countdownText);

        } else {
            binding.tvCountdownToNextPrayer.setText("Memuat waktu sholat...");
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void schedulePrayerAlarms(PrayerTimesResponse.PrayerTimesData data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.w("PrayerTimesFragment", "Izin 'SCHEDULE_EXACT_ALARM' belum diberikan. Meminta pengguna untuk memberikan izin.");
                new AlertDialog.Builder(requireContext())
                        .setTitle("Izin Diperlukan: Alarm Adzan")
                        .setMessage("Aplikasi Tazkeeya memerlukan izin untuk menjadwalkan alarm tepat waktu agar notifikasi adzan berfungsi secara akurat. Silakan izinkan di pengaturan aplikasi.")
                        .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                            startActivity(intent);
                        })
                        .setNegativeButton("Batal", (dialog, which) -> {
                            Toast.makeText(requireContext(), "Izin alarm tidak diberikan. Alarm adzan mungkin tidak akurat.", Toast.LENGTH_LONG).show();
                        })
                        .setCancelable(false)
                        .show();
                return;
            }
        }

        Log.d("PrayerTimesFragment", "Mencoba menjadwalkan alarm sholat...");

        SimpleDateFormat fullDateTimeParser = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US);
        TimeZone apiTimeZone = TimeZone.getTimeZone(data.getMeta().getTimezone());
        fullDateTimeParser.setTimeZone(apiTimeZone); // Penting: Set TimeZone untuk parser ini

        String apiReadableDate = data.getDate().getReadable();

        String[] prayerNames = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] prayerTimes = {
                data.getTimings().getFajr(),
                data.getTimings().getDhuhr(),
                data.getTimings().getAsr(),
                data.getTimings().getMaghrib(),
                data.getTimings().getIsha()
        };

        for (int i = 0; i < prayerNames.length; i++) {
            String prayerName = prayerNames[i];
            String time = prayerTimes[i];

            try {
                Date prayerDateTime = fullDateTimeParser.parse(apiReadableDate + " " + time);
                if (prayerDateTime == null) {
                    Log.e("PrayerTimesFragment", "Gagal parse waktu sholat untuk " + prayerName + ": " + time + " (hasil null)");
                    continue;
                }

                Calendar calendar = Calendar.getInstance(apiTimeZone);
                calendar.setTime(prayerDateTime);
                long triggerTime = calendar.getTimeInMillis();

                if (triggerTime <= System.currentTimeMillis() + (5 * 60 * 1000)) {
                    calendar.add(Calendar.DATE, 1);
                    triggerTime = calendar.getTimeInMillis();
                    Log.d("PrayerTimesFragment", "Waktu sholat " + prayerName + " (" + time + ") sudah lewat hari ini. Dijadwalkan untuk besok: " + fullDateTimeParser.format(new Date(triggerTime)));
                } else {
                    Log.d("PrayerTimesFragment", "Waktu sholat " + prayerName + " (" + time + ") dijadwalkan hari ini: " + fullDateTimeParser.format(new Date(triggerTime)));
                }

                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(requireContext(), AdzanBroadcastReceiver.class);
                intent.setAction("com.example.finalproject.ACTION_ADZAN_ALARM");
                intent.putExtra("PRAYER_NAME", prayerName);
                int requestCode = 100 + i;

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
                Log.d("PrayerTimesFragment", "Alarm " + prayerName + " berhasil dijadwalkan pada " + fullDateTimeParser.format(new Date(triggerTime)));

            } catch (ParseException e) {
                Log.e("PrayerTimesFragment", "Error parsing time for " + prayerName + ": '" + apiReadableDate + " " + time + "' with format 'dd MMM yyyy HH:mm'", e);
                Toast.makeText(requireContext(), "Gagal menjadwalkan alarm " + prayerName + ": format waktu tidak valid.", Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                Log.e("PrayerTimesFragment", "Izin tidak cukup untuk menjadwalkan alarm. Pastikan izin 'SCHEDULE_EXACT_ALARM' diberikan.", e);
                Toast.makeText(requireContext(), "Gagal menjadwalkan alarm: Izin tidak diberikan.", Toast.LENGTH_LONG).show();
            }
        }
        Toast.makeText(requireContext(), "Alarm sholat telah dijadwalkan!", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        if (binding != null) {
            binding.progressBarPrayer.setVisibility(View.GONE);
            binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
            binding.tvLocation.setText("Gagal mendapatkan lokasi/data.");
            binding.tvFajr.setText("Subuh: --:--");
            binding.tvDhuhr.setText("Dzuhur: --:--");
            binding.tvAsr.setText("Ashar: --:--");
            binding.tvMaghrib.setText("Maghrib: --:--");
            binding.tvIsha.setText("Isya: --:--");
            binding.tvSunrise.setText("Terbit Matahari: --:--");
        }
        if (getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e("PrayerTimesFragment", "Error: " + message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndFetchPrayerTimes();
            } else {
                showError("Izin lokasi ditolak. Waktu sholat mungkin tidak akurat.");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
                Log.d("PrayerTimesFragment", "Location updates dihapus.");
            } catch (SecurityException e) {
                Log.e("PrayerTimesFragment", "Tidak dapat menghapus update lokasi: " + e.getMessage());
            }
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}