package com.example.dussehraphotoorganizer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;
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

    private MaterialButton btnOrganizeExisting;
    private MaterialButton btnOrganizeNew;
    private MaterialButton btnFestivalSchedule;
    private MaterialButton btnViewOrganized;
    private MaterialButton btnSettings;

    private AlankaramSchedule schedule;
    private LocalHistoryRepository historyRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        schedule = new AlankaramSchedule(this);
        historyRepository = new LocalHistoryRepository(this);

        initViews();
        setupClickListeners();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardInfo();
    }

    private void initViews() {
        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvTodayTeluguTitle = findViewById(R.id.tvTodayTeluguTitle);
        tvTodayEnglishTitle = findViewById(R.id.tvTodayEnglishTitle);
        tvTodayFolderTarget = findViewById(R.id.tvTodayFolderTarget);
        tvLastResult = findViewById(R.id.tvLastResult);

        btnOrganizeExisting = findViewById(R.id.btnOrganizeExisting);
        btnOrganizeNew = findViewById(R.id.btnOrganizeNew);
        btnFestivalSchedule = findViewById(R.id.btnFestivalSchedule);
        btnViewOrganized = findViewById(R.id.btnViewOrganized);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void updateDashboardInfo() {
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
        btnOrganizeExisting.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizePhotosActivity.class);
            startActivity(intent);
        });

        btnOrganizeNew.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizePhotosActivity.class);
            intent.putExtra("SCAN_MODE", "NEW_ONLY");
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
