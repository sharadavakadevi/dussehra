package com.example.dussehraphotoorganizer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.service.PhotoWorker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {

    private static final String WORK_NAME_AUTO_ORGANIZE = "work_auto_organize_camera";

    private MaterialSwitch switchAutoLocationOrganize;
    private MaterialSwitch switchAutoOrganize;
    private MaterialSwitch switchIncludeVideos;
    private MaterialButton btnConfigureLocations;
    private LocalHistoryRepository historyRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(v -> finish());

        historyRepository = new LocalHistoryRepository(this);

        switchAutoLocationOrganize = findViewById(R.id.switchAutoLocationOrganize);
        switchAutoOrganize = findViewById(R.id.switchAutoOrganize);
        switchIncludeVideos = findViewById(R.id.switchIncludeVideos);
        btnConfigureLocations = findViewById(R.id.btnConfigureLocations);

        switchAutoLocationOrganize.setChecked(historyRepository.isAutoLocationOrganizeEnabled());
        switchAutoOrganize.setChecked(historyRepository.isAutoOrganizeEnabled());
        switchIncludeVideos.setChecked(historyRepository.isIncludeVideos());

        switchAutoLocationOrganize.setOnCheckedChangeListener((buttonView, isChecked) -> {
            historyRepository.setAutoLocationOrganizeEnabled(isChecked);
            if (isChecked || historyRepository.isAutoOrganizeEnabled()) {
                scheduleBackgroundWorker();
            } else {
                cancelBackgroundWorker();
            }
            Toast.makeText(this, isChecked ? "Location auto-organizer enabled." : "Location auto-organizer disabled.", Toast.LENGTH_SHORT).show();
        });

        switchAutoOrganize.setOnCheckedChangeListener((buttonView, isChecked) -> {
            historyRepository.setAutoOrganizeEnabled(isChecked);
            if (isChecked || historyRepository.isAutoLocationOrganizeEnabled()) {
                scheduleBackgroundWorker();
            } else {
                cancelBackgroundWorker();
            }
            Toast.makeText(this, isChecked ? "Festival auto-organizer enabled." : "Festival auto-organizer disabled.", Toast.LENGTH_SHORT).show();
        });

        switchIncludeVideos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            historyRepository.setIncludeVideos(isChecked);
            Toast.makeText(this, isChecked ? "Video organizing enabled." : "Photos only mode enabled.", Toast.LENGTH_SHORT).show();
        });

        btnConfigureLocations.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ManageLocationsActivity.class);
            startActivity(intent);
        });

        // Ensure background worker is scheduled if either auto-organizer is enabled
        if (historyRepository.isAutoLocationOrganizeEnabled() || historyRepository.isAutoOrganizeEnabled()) {
            scheduleBackgroundWorker();
        }
    }

    private void scheduleBackgroundWorker() {
        // Enqueue 15-minute periodic work (minimum allowed interval by Android WorkManager)
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(PhotoWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WORK_NAME_AUTO_ORGANIZE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    private void cancelBackgroundWorker() {
        if (!historyRepository.isAutoLocationOrganizeEnabled() && !historyRepository.isAutoOrganizeEnabled()) {
            WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME_AUTO_ORGANIZE);
        }
    }
}
