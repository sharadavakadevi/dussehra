package com.example.dussehraphotoorganizer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;
import com.example.dussehraphotoorganizer.service.CameraPhotoContentObserver;
import com.example.dussehraphotoorganizer.util.PermissionHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvTodayDate;
    private TextView tvTodayTeluguTitle;
    private TextView tvTodayEnglishTitle;
    private TextView tvTodayFolderTarget;
    private TextView tvLastResult;

    // Location Organizer Views
    private TextView tvLastLocationResult;
    private TextView tvAutoOrganizeBadge;
    private MaterialButton btnOrganizeLocation;
    private MaterialButton btnManageLocations;

    // Wi-Fi Photo Sync Views
    private TextView tvWifiSyncBadge;
    private TextView tvWifiSyncStatus;
    private MaterialButton btnWifiSync;
    private com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository syncLedgerRepository;

    // Festival Organization Views
    private MaterialButton btnOrganizeExisting;
    private MaterialButton btnOrganizeNew;
    private MaterialButton btnFestivalSchedule;
    private MaterialButton btnViewOrganized;
    private MaterialButton btnSettings;

    private AlankaramSchedule schedule;
    private LocalHistoryRepository historyRepository;
    private CameraPhotoContentObserver cameraObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        schedule = new AlankaramSchedule(this);
        historyRepository = new LocalHistoryRepository(this);
        syncLedgerRepository = new com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository(this);
        cameraObserver = new CameraPhotoContentObserver(this);

        initViews();
        setupClickListeners();
        checkPermissions();
        com.example.dussehraphotoorganizer.sync.service.SyncTriggerManager.startSyncService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardInfo();
        try {
            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    cameraObserver
            );
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            getContentResolver().unregisterContentObserver(cameraObserver);
        } catch (Exception ignored) {}
    }

    private void initViews() {
        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvTodayTeluguTitle = findViewById(R.id.tvTodayTeluguTitle);
        tvTodayEnglishTitle = findViewById(R.id.tvTodayEnglishTitle);
        tvTodayFolderTarget = findViewById(R.id.tvTodayFolderTarget);
        tvLastResult = findViewById(R.id.tvLastResult);

        tvLastLocationResult = findViewById(R.id.tvLastLocationResult);
        tvAutoOrganizeBadge = findViewById(R.id.tvAutoOrganizeBadge);
        btnOrganizeLocation = findViewById(R.id.btnOrganizeLocation);
        btnManageLocations = findViewById(R.id.btnManageLocations);

        tvWifiSyncBadge = findViewById(R.id.tvWifiSyncBadge);
        tvWifiSyncStatus = findViewById(R.id.tvWifiSyncStatus);
        btnWifiSync = findViewById(R.id.btnWifiSync);

        btnOrganizeExisting = findViewById(R.id.btnOrganizeExisting);
        btnOrganizeNew = findViewById(R.id.btnOrganizeNew);
        btnFestivalSchedule = findViewById(R.id.btnFestivalSchedule);
        btnViewOrganized = findViewById(R.id.btnViewOrganized);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void updateDashboardInfo() {
        // Wi-Fi Photo Sync Dashboard Update
        if (syncLedgerRepository.isPaired()) {
            tvWifiSyncBadge.setText("PAIRED & READY");
            tvWifiSyncBadge.setBackgroundColor(getColor(R.color.status_success));
            int sent = syncLedgerRepository.getOutgoingSyncedCount();
            int recv = syncLedgerRepository.getIncomingReceivedCount();
            tvWifiSyncStatus.setText(String.format("Paired with: %s • Synced: %d Sent, %d Received",
                    syncLedgerRepository.getPairedDeviceName(), sent, recv));
        } else {
            tvWifiSyncBadge.setText("UNPAIRED");
            tvWifiSyncBadge.setBackgroundColor(getColor(R.color.maroon_kumkum));
            tvWifiSyncStatus.setText("Tap to pair with your other phone for automatic Wi-Fi photo sync");
        }

        // Location Dashboard Update
        String lastLocSummary = historyRepository.getLastLocationRunSummary();
        if (lastLocSummary != null && !lastLocSummary.isEmpty()) {
            tvLastLocationResult.setText("Last Location Result: " + lastLocSummary);
        } else {
            tvLastLocationResult.setText("Target: Home, Yeendda_Durga_Devi_Temple, Carshed_Bobleam, YYYY-MM-DD");
        }

        if (historyRepository.isAutoLocationOrganizeEnabled()) {
            tvAutoOrganizeBadge.setText("Auto-Organize ON");
            tvAutoOrganizeBadge.setVisibility(View.VISIBLE);
        } else {
            tvAutoOrganizeBadge.setText("Manual Only");
            tvAutoOrganizeBadge.setVisibility(View.VISIBLE);
        }

        // Festival Dashboard Update
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        String today = sdf.format(new Date());
        tvTodayDate.setText(today);

        FestivalDateItem todayItem = schedule.getFestivalDateItem(today);
        if (todayItem != null) {
            tvTodayTeluguTitle.setText(todayItem.getTeluguName());
            tvTodayEnglishTitle.setText(todayItem.getEnglishName() + " (" + todayItem.getDayTithi() + ")");
            tvTodayFolderTarget.setText("Target: Pictures/Dussehra_2026/" + todayItem.getFolderName() + "/");
        } else {
            tvTodayTeluguTitle.setText("శ్రీ దుర్గాదేవి దసరా ఉత్సవములు 2026");
            tvTodayEnglishTitle.setText(getString(R.string.no_alankaram_today));
            tvTodayFolderTarget.setText("Main: Pictures/Dussehra_2026/ (11 Subfolders ready)");
        }

        String lastSummary = historyRepository.getLastRunSummary();
        if (lastSummary != null && !lastSummary.isEmpty()) {
            tvLastResult.setText(lastSummary);
        } else {
            tvLastResult.setText(getString(R.string.last_result_none));
        }
    }

    private void setupClickListeners() {
        btnWifiSync.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PairDevicesActivity.class);
            startActivity(intent);
        });

        btnOrganizeLocation.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizePhotosActivity.class);
            intent.putExtra(OrganizePhotosActivity.EXTRA_MODE, OrganizePhotosActivity.MODE_LOCATION);
            startActivity(intent);
        });

        btnManageLocations.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ManageLocationsActivity.class);
            startActivity(intent);
        });

        btnOrganizeExisting.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizePhotosActivity.class);
            intent.putExtra(OrganizePhotosActivity.EXTRA_MODE, OrganizePhotosActivity.MODE_FESTIVAL);
            startActivity(intent);
        });

        btnOrganizeNew.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizePhotosActivity.class);
            intent.putExtra("SCAN_MODE", "NEW_ONLY");
            intent.putExtra(OrganizePhotosActivity.EXTRA_MODE, OrganizePhotosActivity.MODE_FESTIVAL);
            startActivity(intent);
        });

        btnFestivalSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FestivalScheduleActivity.class);
            startActivity(intent);
        });

        btnViewOrganized.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void checkPermissions() {
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_PERMISSIONS) {
            if (PermissionHelper.hasAllPermissions(this)) {
                Toast.makeText(this, "Camera & storage permissions granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission required to access DCIM/Camera photos.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
