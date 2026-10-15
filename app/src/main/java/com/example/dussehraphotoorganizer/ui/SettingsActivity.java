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
    private MaterialSwitch switchAutoWifiSync;
    private MaterialSwitch switchIncludeVideos;
    private MaterialButton btnConfigureLocations;
    private MaterialButton btnManagePairing;
    private LocalHistoryRepository historyRepository;
    private com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository syncLedgerRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(v -> finish());

        historyRepository = new LocalHistoryRepository(this);
        syncLedgerRepository = new com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository(this);

        switchAutoLocationOrganize = findViewById(R.id.switchAutoLocationOrganize);
        switchAutoOrganize = findViewById(R.id.switchAutoOrganize);
        switchAutoWifiSync = findViewById(R.id.switchAutoWifiSync);
        switchIncludeVideos = findViewById(R.id.switchIncludeVideos);
        btnConfigureLocations = findViewById(R.id.btnConfigureLocations);
        btnManagePairing = findViewById(R.id.btnManagePairing);
        MaterialButton btnCleanDuplicates = findViewById(R.id.btnCleanDuplicates);

        if (btnCleanDuplicates != null) {
            btnCleanDuplicates.setOnClickListener(v -> {
                btnCleanDuplicates.setEnabled(false);
                btnCleanDuplicates.setText("Cleaning duplicates...");
                new com.example.dussehraphotoorganizer.util.DuplicateCleanupHelper(this).cleanDuplicates((removedCount, freedBytes, msg) -> {
                    runOnUiThread(() -> {
                        btnCleanDuplicates.setEnabled(true);
                        btnCleanDuplicates.setText(R.string.btn_clean_duplicates);
                        new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                                .setTitle("Duplicate Cleanup")
                                .setMessage(msg)
                                .setPositiveButton("OK", null)
                                .show();
                    });
                });
            });
        }

        switchAutoLocationOrganize.setChecked(historyRepository.isAutoLocationOrganizeEnabled());
        switchAutoOrganize.setChecked(historyRepository.isAutoOrganizeEnabled());
        switchAutoWifiSync.setChecked(syncLedgerRepository.isAutoWifiSyncEnabled());
        switchIncludeVideos.setChecked(historyRepository.isIncludeVideos());

        switchAutoWifiSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            syncLedgerRepository.setAutoWifiSyncEnabled(isChecked);
            if (isChecked) {
                com.example.dussehraphotoorganizer.sync.service.SyncTriggerManager.startSyncService(this);
            }
            Toast.makeText(this, isChecked ? "Wi-Fi auto-sync enabled." : "Wi-Fi auto-sync disabled.", Toast.LENGTH_SHORT).show();
        });

        btnManagePairing.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, PairDevicesActivity.class);
            startActivity(intent);
        });

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
