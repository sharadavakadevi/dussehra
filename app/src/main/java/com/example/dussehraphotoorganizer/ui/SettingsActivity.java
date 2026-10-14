package com.example.dussehraphotoorganizer.ui;

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
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {

    private static final String WORK_NAME_AUTO_ORGANIZE = "work_auto_organize_camera";

    private MaterialSwitch switchAutoOrganize;
    private MaterialSwitch switchIncludeVideos;
    private LocalHistoryRepository historyRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(v -> finish());

        historyRepository = new LocalHistoryRepository(this);

        switchAutoOrganize = findViewById(R.id.switchAutoOrganize);
        switchIncludeVideos = findViewById(R.id.switchIncludeVideos);

        switchAutoOrganize.setChecked(historyRepository.isAutoOrganizeEnabled());
        switchIncludeVideos.setChecked(historyRepository.isIncludeVideos());

        switchAutoOrganize.setOnCheckedChangeListener((buttonView, isChecked) -> {
            historyRepository.setAutoOrganizeEnabled(isChecked);
            if (isChecked) {
                scheduleBackgroundWorker();
                Toast.makeText(this, "Automatic background camera monitoring enabled.", Toast.LENGTH_SHORT).show();
            } else {
                cancelBackgroundWorker();
                Toast.makeText(this, "Automatic background camera monitoring disabled.", Toast.LENGTH_SHORT).show();
            }
        });

        switchIncludeVideos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            historyRepository.setIncludeVideos(isChecked);
            Toast.makeText(this, isChecked ? "Video organizing enabled." : "Photos only mode enabled.", Toast.LENGTH_SHORT).show();
        });
    }

    private void scheduleBackgroundWorker() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(PhotoWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WORK_NAME_AUTO_ORGANIZE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    private void cancelBackgroundWorker() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME_AUTO_ORGANIZE);
    }
}
