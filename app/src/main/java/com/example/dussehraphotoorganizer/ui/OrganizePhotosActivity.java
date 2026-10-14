package com.example.dussehraphotoorganizer.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.location.LocationConfigManager;
import com.example.dussehraphotoorganizer.model.PhotoItem;
import com.example.dussehraphotoorganizer.organizer.LocationPhotoOrganizer;
import com.example.dussehraphotoorganizer.organizer.PhotoOrganizer;
import com.example.dussehraphotoorganizer.scanner.LocationPhotoScanner;
import com.example.dussehraphotoorganizer.scanner.PhotoScanner;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrganizePhotosActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "ORGANIZATION_MODE";
    public static final String MODE_LOCATION = "MODE_LOCATION";
    public static final String MODE_FESTIVAL = "MODE_FESTIVAL";

    private ChipGroup chipGroupMode;
    private Chip chipLocationMode;
    private Chip chipFestivalMode;
    private TextView tvModeTargetDirectory;
    private TextView tvFoundCount;
    private MaterialButton btnToggleSelectAll;
    private ProgressBar progressBar;
    private RecyclerView rvEligiblePhotos;
    private MaterialButton btnAutoCopyAll;
    private MaterialButton btnConfirmCopy;

    private String currentMode = MODE_LOCATION;
    private PhotoScanner festivalScanner;
    private PhotoOrganizer festivalOrganizer;
    private LocationPhotoScanner locationScanner;
    private LocationPhotoOrganizer locationOrganizer;

    private final List<PhotoItem> photoList = new ArrayList<>();
    private PhotoAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean allSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize_photos);

        MaterialToolbar toolbar = findViewById(R.id.toolbarOrganize);
        toolbar.setNavigationOnClickListener(v -> finish());

        chipGroupMode = findViewById(R.id.chipGroupMode);
        chipLocationMode = findViewById(R.id.chipLocationMode);
        chipFestivalMode = findViewById(R.id.chipFestivalMode);
        tvModeTargetDirectory = findViewById(R.id.tvModeTargetDirectory);
        tvFoundCount = findViewById(R.id.tvFoundCount);
        btnToggleSelectAll = findViewById(R.id.btnToggleSelectAll);
        progressBar = findViewById(R.id.progressBar);
        rvEligiblePhotos = findViewById(R.id.rvEligiblePhotos);
        btnAutoCopyAll = findViewById(R.id.btnAutoCopyAll);
        btnConfirmCopy = findViewById(R.id.btnConfirmCopy);

        festivalScanner = new PhotoScanner(this);
        festivalOrganizer = new PhotoOrganizer(this);
        locationScanner = new LocationPhotoScanner(this);
        locationOrganizer = new LocationPhotoOrganizer(this);

        rvEligiblePhotos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PhotoAdapter();
        rvEligiblePhotos.setAdapter(adapter);

        if (getIntent().hasExtra(EXTRA_MODE)) {
            currentMode = getIntent().getStringExtra(EXTRA_MODE);
        } else if ("NEW_ONLY".equals(getIntent().getStringExtra("SCAN_MODE"))) {
            currentMode = MODE_FESTIVAL;
        }

        if (MODE_FESTIVAL.equals(currentMode)) {
            chipFestivalMode.setChecked(true);
        } else {
            chipLocationMode.setChecked(true);
        }

        chipGroupMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipFestivalMode)) {
                currentMode = MODE_FESTIVAL;
            } else {
                currentMode = MODE_LOCATION;
            }
            updateModeUi();
            startScanning();
        });

        btnToggleSelectAll.setOnClickListener(v -> toggleSelectAll());
        btnConfirmCopy.setOnClickListener(v -> showCopyConfirmationDialog());
        btnAutoCopyAll.setOnClickListener(v -> executeAutoCopyAll());

        updateModeUi();
        startScanning();
    }

    private void updateModeUi() {
        if (MODE_FESTIVAL.equals(currentMode)) {
            tvModeTargetDirectory.setText("Target: Pictures/Dussehra_2026/ (11 Goddess Alankaram subfolders)");
        } else {
            tvModeTargetDirectory.setText("Target: Pictures/Location_Organized/ (Home, Yeendda_Durga_Devi_Temple, Carshed_Bobleam, YYYY-MM-DD)");
        }
    }

    private void startScanning() {
        progressBar.setVisibility(View.VISIBLE);
        tvFoundCount.setText(getString(R.string.scanning_camera_photos));
        btnConfirmCopy.setEnabled(false);
        btnAutoCopyAll.setEnabled(false);

        executor.execute(() -> {
            if (MODE_FESTIVAL.equals(currentMode)) {
                PhotoScanner.ScanResult result = festivalScanner.scanCameraPhotos((scanned, matching) -> {
                    runOnUiThread(() -> tvFoundCount.setText("Scanning camera... " + matching + " festival photos found"));
                });

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    photoList.clear();
                    photoList.addAll(result.eligiblePhotos);
                    adapter.notifyDataSetChanged();

                    tvFoundCount.setText(String.format("Found: %d eligible festival camera photos (Skipped %d non-matching / duplicates)",
                            result.eligiblePhotos.size(), result.skippedCount));

                    boolean hasItems = !photoList.isEmpty();
                    btnConfirmCopy.setEnabled(hasItems);
                    btnAutoCopyAll.setEnabled(hasItems);

                    if (!hasItems) {
                        Toast.makeText(OrganizePhotosActivity.this, "No eligible festival camera photos found.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                LocationPhotoScanner.LocationScanResult result = locationScanner.scanCameraPhotos((scanned, matching) -> {
                    runOnUiThread(() -> tvFoundCount.setText("Scanning camera... " + matching + " eligible photos"));
                });

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    photoList.clear();
                    photoList.addAll(result.eligiblePhotos);
                    adapter.notifyDataSetChanged();

                    tvFoundCount.setText(String.format("Found: %d eligible photos (Skipped %d duplicates / non-camera)",
                            result.eligiblePhotos.size(), result.skippedCount));

                    boolean hasItems = !photoList.isEmpty();
                    btnConfirmCopy.setEnabled(hasItems);
                    btnAutoCopyAll.setEnabled(hasItems);

                    if (!hasItems) {
                        Toast.makeText(OrganizePhotosActivity.this, "No new camera photos found to organize.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void toggleSelectAll() {
        allSelected = !allSelected;
        for (PhotoItem item : photoList) {
            item.setSelected(allSelected);
        }
        adapter.notifyDataSetChanged();
        btnToggleSelectAll.setText(allSelected ? R.string.deselect_all : R.string.select_all);
    }

    private int getSelectedCount() {
        int count = 0;
        for (PhotoItem item : photoList) {
            if (item.isSelected()) count++;
        }
        return count;
    }

    private void showCopyConfirmationDialog() {
        int count = getSelectedCount();
        if (count == 0) {
            Toast.makeText(this, "Please select at least one photo to organize.", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetPath = MODE_FESTIVAL.equals(currentMode) ? "Pictures/Dussehra_2026/" : "Pictures/Location_Organized/";
        String msg = String.format("Selected %d camera photos will be safely copied into %s subfolders.\n\nOriginal camera photos remain untouched.",
                count, targetPath);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_confirm_title)
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> executePhotoCopy())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void executeAutoCopyAll() {
        for (PhotoItem item : photoList) {
            item.setSelected(true);
        }
        adapter.notifyDataSetChanged();
        executePhotoCopy();
    }

    private void executePhotoCopy() {
        progressBar.setVisibility(View.VISIBLE);
        btnConfirmCopy.setEnabled(false);
        btnAutoCopyAll.setEnabled(false);

        executor.execute(() -> {
            int copied;
            int skipped;
            int failed;
            String root;

            if (MODE_FESTIVAL.equals(currentMode)) {
                root = "Pictures/Dussehra_2026/";
                PhotoOrganizer.CopyResult result = festivalOrganizer.copyPhotosToFestivalFolders(photoList, (current, total, name) -> {
                    runOnUiThread(() -> tvFoundCount.setText(String.format("Copying (%d/%d): %s", current, total, name)));
                });
                copied = result.copiedCount;
                skipped = result.skippedCount;
                failed = result.failedCount;
            } else {
                root = "Pictures/Location_Organized/";
                LocationPhotoOrganizer.CopyResult result = locationOrganizer.copyPhotosToLocationFolders(photoList, (current, total, name, folder) -> {
                    runOnUiThread(() -> tvFoundCount.setText(String.format("Copying (%d/%d) to %s: %s", current, total, folder, name)));
                });
                copied = result.copiedCount;
                skipped = result.skippedCount;
                failed = result.failedCount;
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnConfirmCopy.setEnabled(true);
                btnAutoCopyAll.setEnabled(true);

                new AlertDialog.Builder(OrganizePhotosActivity.this)
                        .setTitle("Organization Complete")
                        .setMessage(String.format("Result:\n• Copied: %d\n• Skipped: %d\n• Failed: %d\n\nAll photos are saved in %s\nOriginal camera photos remain untouched.",
                                copied, skipped, failed, root))
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .show();
            });
        });
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> {

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_card, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            PhotoItem item = photoList.get(position);
            holder.tvPhotoFileName.setText(item.getFileName());
            holder.tvCaptureDate.setText("Captured: " + item.getCaptureDateFormatted());
            holder.tvDestinationFolder.setText("Folder: " + item.getDestinationFolderName());
            holder.tvStatusBadge.setText(item.getStatusMessage());

            holder.cbSelectPhoto.setOnCheckedChangeListener(null);
            holder.cbSelectPhoto.setChecked(item.isSelected());
            holder.cbSelectPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> item.setSelected(isChecked));

            if (item.getUri() != null) {
                Glide.with(OrganizePhotosActivity.this)
                        .load(item.getUri())
                        .centerCrop()
                        .placeholder(R.color.cream_surface)
                        .into(holder.ivThumbnail);
            }
        }

        @Override
        public int getItemCount() {
            return photoList.size();
        }
    }

    private static class PhotoViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectPhoto;
        ImageView ivThumbnail;
        TextView tvPhotoFileName;
        TextView tvCaptureDate;
        TextView tvDestinationFolder;
        TextView tvStatusBadge;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectPhoto = itemView.findViewById(R.id.cbSelectPhoto);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvPhotoFileName = itemView.findViewById(R.id.tvPhotoFileName);
            tvCaptureDate = itemView.findViewById(R.id.tvCaptureDate);
            tvDestinationFolder = itemView.findViewById(R.id.tvDestinationFolder);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        }
    }
}
