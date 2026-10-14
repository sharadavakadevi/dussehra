package com.example.dussehraphotoorganizer.ui;

import android.content.DialogInterface;
import android.net.Uri;
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
import com.example.dussehraphotoorganizer.model.PhotoItem;
import com.example.dussehraphotoorganizer.organizer.PhotoOrganizer;
import com.example.dussehraphotoorganizer.scanner.PhotoScanner;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrganizePhotosActivity extends AppCompatActivity {

    private TextView tvFoundCount;
    private MaterialButton btnToggleSelectAll;
    private ProgressBar progressBar;
    private RecyclerView rvEligiblePhotos;
    private MaterialButton btnConfirmCopy;

    private PhotoScanner photoScanner;
    private PhotoOrganizer photoOrganizer;
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

        tvFoundCount = findViewById(R.id.tvFoundCount);
        btnToggleSelectAll = findViewById(R.id.btnToggleSelectAll);
        progressBar = findViewById(R.id.progressBar);
        rvEligiblePhotos = findViewById(R.id.rvEligiblePhotos);
        btnConfirmCopy = findViewById(R.id.btnConfirmCopy);

        photoScanner = new PhotoScanner(this);
        photoOrganizer = new PhotoOrganizer(this);

        rvEligiblePhotos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PhotoAdapter();
        rvEligiblePhotos.setAdapter(adapter);

        btnToggleSelectAll.setOnClickListener(v -> toggleSelectAll());
        btnConfirmCopy.setOnClickListener(v -> showCopyConfirmationDialog());

        startScanning();
    }

    private void startScanning() {
        progressBar.setVisibility(View.VISIBLE);
        tvFoundCount.setText(getString(R.string.scanning_camera_photos));

        executor.execute(() -> {
            PhotoScanner.ScanResult result = photoScanner.scanCameraPhotos((scanned, matching) -> {
                runOnUiThread(() -> tvFoundCount.setText("Scanning camera... " + matching + " matching photos found"));
            });

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                photoList.clear();
                photoList.addAll(result.eligiblePhotos);
                adapter.notifyDataSetChanged();

                tvFoundCount.setText(String.format("Found: %d eligible camera photos (Skipped %d non-matching / duplicates)",
                        result.eligiblePhotos.size(), result.skippedCount));

                if (photoList.isEmpty()) {
                    btnConfirmCopy.setEnabled(false);
                    Toast.makeText(OrganizePhotosActivity.this, "No eligible camera photos found for the 2026 festival dates.", Toast.LENGTH_LONG).show();
                } else {
                    btnConfirmCopy.setEnabled(true);
                }
            });
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

        String msg = getString(R.string.dialog_confirm_message, count);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_confirm_title)
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> executePhotoCopy())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void executePhotoCopy() {
        progressBar.setVisibility(View.VISIBLE);
        btnConfirmCopy.setEnabled(false);

        executor.execute(() -> {
            PhotoOrganizer.CopyResult result = photoOrganizer.copyPhotosToFestivalFolders(photoList, (current, total, name) -> {
                runOnUiThread(() -> tvFoundCount.setText(String.format("Copying (%d/%d): %s", current, total, name)));
            });

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnConfirmCopy.setEnabled(true);

                new AlertDialog.Builder(OrganizePhotosActivity.this)
                        .setTitle("Organization Complete")
                        .setMessage(String.format("Result:\n• Copied: %d\n• Skipped: %d\n• Failed: %d\n\nAll photos are saved in Pictures/Dussehra_2026/\nOriginal camera photos remain untouched.",
                                result.copiedCount, result.skippedCount, result.failedCount))
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
