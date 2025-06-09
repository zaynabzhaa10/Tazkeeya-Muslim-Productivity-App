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
import android.view.View; // Pastikan ini diimpor
import android.view.ViewGroup;
import android.widget.Toast; // Pastikan ini diimpor

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.provider.Settings;
import android.app.AlertDialog;
import android.net.Uri;

public class PrayerTimesFragment extends Fragment {

    private FragmentPrayerTimesBinding binding;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ExecutorService executorService;
    private Handler mainHandler;

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

        binding.btnRefreshPrayer.setOnClickListener(v -> requestLocationAndFetchPrayerTimes());

        checkLocationPermissionAndFetchPrayerTimes();
    }

    private void checkLocationPermissionAndFetchPrayerTimes() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            requestLocationAndFetchPrayerTimes();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLocationAndFetchPrayerTimes() {
        binding.progressBarPrayer.setVisibility(View.VISIBLE);
        binding.prayerTimesContainer.setVisibility(View.GONE);
        binding.btnRefreshPrayer.setVisibility(View.GONE);
        binding.tvLocation.setText("Mendapatkan lokasi...");

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
            Log.e("PrayerTimesFragment", "Izin lokasi tidak diberikan untuk getLastKnownLocation", e);
            showError("Aplikasi tidak memiliki izin lokasi.");
            binding.tvLocation.setText("Gagal mendapatkan lokasi: Izin ditolak.");
            binding.progressBarPrayer.setVisibility(View.GONE);
            binding.btnRefreshPrayer.setVisibility(View.VISIBLE);
            return;
        }

        if (lastKnownLocation != null) {
            Log.d("PrayerTimesFragment", "Lokasi terakhir diketahui: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
            binding.tvLocation.setText("\uD83D\uDCCD Makassar, Indonesia");
            fetchPrayerTimes(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        } else {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    locationManager.removeUpdates(this);
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("PrayerTimesFragment", "Lokasi ditemukan: " + latitude + ", " + longitude);
                    binding.tvLocation.setText("\uD83D\uDCCD Makassar, Indonesia");
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
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 60 * 5, 10, locationListener);
                } else if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 60 * 5, 10, locationListener);
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

    private void fetchPrayerTimes(double latitude, double longitude) {
        ApiService apiService = RetrofitClient.getAladhanClient().create(ApiService.class);
        long timestamp = System.currentTimeMillis() / 1000L;
        Call<PrayerTimesResponse> call = apiService.getPrayerTimesByCoordinates(timestamp, latitude, longitude, 5);

        executorService.execute(() -> {
            call.enqueue(new Callback<PrayerTimesResponse>() {
                @Override
                public void onResponse(@NonNull Call<PrayerTimesResponse> call, @NonNull Response<PrayerTimesResponse> response) {
                    mainHandler.post(() -> {
                        binding.progressBarPrayer.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            displayPrayerTimes(response.body().getData());
                            binding.prayerTimesContainer.setVisibility(View.VISIBLE);
                            schedulePrayerAlarms(response.body().getData());
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
        // Tampilkan tanggal Gregorian dan Hijriah
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

    @SuppressLint("ScheduleExactAlarm")
    private void schedulePrayerAlarms(PrayerTimesResponse.PrayerTimesData data) {
        // --- TAMBAHAN PENTING UNTUK REQUEST IZIN ALARM TEAPAT WAKTU ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Hanya untuk Android 12 (API 31) dan di atasnya
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            // Periksa apakah aplikasi sudah memiliki izin untuk menjadwalkan alarm tepat waktu
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.w("PrayerTimesFragment", "Izin 'SCHEDULE_EXACT_ALARM' belum diberikan. Meminta pengguna untuk memberikan izin.");
                // Tampilkan dialog untuk mengarahkan pengguna ke pengaturan izin aplikasi
                new AlertDialog.Builder(requireContext())
                        .setTitle("Izin Diperlukan: Alarm Adzan")
                        .setMessage("Aplikasi Tazkeeya memerlukan izin untuk menjadwalkan alarm tepat waktu agar notifikasi adzan berfungsi secara akurat. Silakan izinkan di pengaturan aplikasi.")
                        .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM); // Intent khusus untuk izin ini
                            intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null)); // Arahkan ke pengaturan aplikasi ini
                            startActivity(intent);
                        })
                        .setNegativeButton("Batal", (dialog, which) -> {
                            Toast.makeText(requireContext(), "Izin alarm tidak diberikan. Alarm adzan mungkin tidak akurat.", Toast.LENGTH_LONG).show();
                        })
                        .setCancelable(false) // Pengguna harus memilih
                        .show();
                return; // Hentikan penjadwalan alarm jika izin belum diberikan
            }
        }
        // --- AKHIR TAMBAHAN PENTING ---

        Log.d("PrayerTimesFragment", "Mencoba menjadwalkan alarm sholat...");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        String todayDateString = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

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
                Date prayerDateTime = dateTimeFormat.parse(todayDateString + " " + time);
                if (prayerDateTime == null) {
                    Log.e("PrayerTimesFragment", "Gagal parse waktu sholat untuk " + prayerName + ": " + time);
                    continue;
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(prayerDateTime);
                long triggerTime = calendar.getTimeInMillis();

                if (triggerTime <= System.currentTimeMillis() + (5 * 60 * 1000)) {
                    calendar.add(Calendar.DATE, 1);
                    triggerTime = calendar.getTimeInMillis();
                    Log.d("PrayerTimesFragment", "Waktu sholat " + prayerName + " (" + time + ") sudah lewat hari ini. Dijadwalkan untuk besok: " + dateTimeFormat.format(new Date(triggerTime)));
                } else {
                    Log.d("PrayerTimesFragment", "Waktu sholat " + prayerName + " (" + time + ") dijadwalkan hari ini: " + dateTimeFormat.format(new Date(triggerTime)));
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
                Log.d("PrayerTimesFragment", "Alarm " + prayerName + " berhasil dijadwalkan pada " + dateTimeFormat.format(new Date(triggerTime)));

            } catch (ParseException e) {
                Log.e("PrayerTimesFragment", "Error parsing time for " + prayerName + ": " + time, e);
                Toast.makeText(requireContext(), "Gagal menjadwalkan alarm " + prayerName + ": format waktu tidak valid.", Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Log.e("PrayerTimesFragment", "Izin tidak cukup untuk menjadwalkan alarm. Pastikan izin 'SCHEDULE_EXACT_ALARM' diberikan.", e);
                Toast.makeText(requireContext(), "Gagal menjadwalkan alarm: Izin tidak diberikan.", Toast.LENGTH_LONG).show();
            }
        }
        Toast.makeText(requireContext(), "Alarm sholat telah dijadwalkan!", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) { // <<< METODE showError() YANG HILANG
        if (binding != null) { // Pastikan binding tidak null
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
        if (getContext() != null) { // Pastikan konteks tidak null
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e("PrayerTimesFragment", "Error: " + message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationAndFetchPrayerTimes();
            } else {
                showError("Izin lokasi ditolak. Waktu sholat mungkin tidak akurat.");
            }
        }
        // Izin notifikasi POST_NOTIFICATIONS ditangani di MainActivity
        // Izin SCHEDULE_EXACT_ALARM ditangani dengan dialog ke Settings
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
