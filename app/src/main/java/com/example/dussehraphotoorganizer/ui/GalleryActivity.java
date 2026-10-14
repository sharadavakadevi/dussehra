package com.example.dussehraphotoorganizer.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.location.LocationConfigManager;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.model.LocationConfigItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private ChipGroup chipGroupFolders;
    private RecyclerView rvGalleryGrid;
    private TextView tvGalleryEmpty;

    private AlankaramSchedule schedule;
    private LocationConfigManager locationConfigManager;
    private final List<GalleryItem> currentPhotos = new ArrayList<>();
    private GalleryAdapter adapter;
    private String filterQueryPattern = null; // null means "All Folders"

    public static class GalleryItem {
        public final Uri uri;
        public final String folderPath;
        public final String fileName;

        public GalleryItem(Uri uri, String folderPath, String fileName) {
            this.uri = uri;
            this.folderPath = folderPath;
            this.fileName = fileName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        MaterialToolbar toolbar = findViewById(R.id.toolbarGallery);
        toolbar.setNavigationOnClickListener(v -> finish());

        chipGroupFolders = findViewById(R.id.chipGroupFolders);
        rvGalleryGrid = findViewById(R.id.rvGalleryGrid);
        tvGalleryEmpty = findViewById(R.id.tvGalleryEmpty);

        schedule = new AlankaramSchedule(this);
        locationConfigManager = new LocationConfigManager(this);

        if (getIntent().hasExtra("FILTER_FOLDER")) {
            String filter = getIntent().getStringExtra("FILTER_FOLDER");
            if (filter != null) {
                filterQueryPattern = "%/" + filter + "/%";
            }
        }

        rvGalleryGrid.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new GalleryAdapter();
        rvGalleryGrid.setAdapter(adapter);

        setupFolderChips();
        loadPhotos();
    }

    private void setupFolderChips() {
        chipGroupFolders.removeAllViews();

        Chip allChip = new Chip(this);
        allChip.setText("All Folders");
        allChip.setCheckable(true);
        allChip.setChecked(filterQueryPattern == null);
        allChip.setOnClickListener(v -> {
            filterQueryPattern = null;
            loadPhotos();
        });
        chipGroupFolders.addView(allChip);

        // Location Filters (Matches location subfolders across all Devi folders)
        for (LocationConfigItem loc : locationConfigManager.getRegisteredLocations()) {
            Chip chip = new Chip(this);
            chip.setText("📍 " + loc.getFolderName());
            chip.setCheckable(true);
            String pattern = "%/" + loc.getFolderName() + "/%";
            if (pattern.equals(filterQueryPattern)) {
                chip.setChecked(true);
            }
            chip.setOnClickListener(v -> {
                filterQueryPattern = pattern;
                loadPhotos();
            });
            chipGroupFolders.addView(chip);
        }

        // Devi Alankaram Parent Folder Filters
        for (FestivalDateItem item : schedule.getSchedule()) {
            Chip chip = new Chip(this);
            chip.setText("🌸 " + item.getFolderName());
            chip.setCheckable(true);
            String pattern = "%/" + item.getFolderName() + "/%";
            if (pattern.equals(filterQueryPattern)) {
                chip.setChecked(true);
            }
            chip.setOnClickListener(v -> {
                filterQueryPattern = pattern;
                loadPhotos();
            });
            chipGroupFolders.addView(chip);
        }
    }

    private void loadPhotos() {
        currentPhotos.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String baseRoot = Environment.DIRECTORY_PICTURES + "/" + AlankaramSchedule.MAIN_FOLDER_NAME + "/";
            String selection;
            String[] selectionArgs;

            if (filterQueryPattern != null) {
                selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ? AND " + MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                selectionArgs = new String[]{baseRoot + "%", filterQueryPattern};
            } else {
                selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                selectionArgs = new String[]{baseRoot + "%"};
            }

            String[] projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.RELATIVE_PATH
            };

            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    int pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String name = cursor.getString(nameCol);
                        String path = cursor.getString(pathCol);
                        Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                        currentPhotos.add(new GalleryItem(uri, path, name));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Legacy files scan under Pictures/Dussehra_2026/
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File mainDir = new File(picturesDir, AlankaramSchedule.MAIN_FOLDER_NAME);
            scanLegacyDirectoryRecursive(mainDir);
        }

        if (currentPhotos.isEmpty()) {
            tvGalleryEmpty.setVisibility(View.VISIBLE);
        } else {
            tvGalleryEmpty.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void scanLegacyDirectoryRecursive(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                scanLegacyDirectoryRecursive(f);
            } else {
                String nameLower = f.getName().toLowerCase();
                if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".png")) {
                    String relPath = f.getParent() != null ? f.getParent() : "";
                    if (filterQueryPattern == null || relPath.contains(filterQueryPattern.replace("%", ""))) {
                        currentPhotos.add(new GalleryItem(Uri.fromFile(f), relPath, f.getName()));
                    }
                }
            }
        }
    }

    private class GalleryAdapter extends RecyclerView.Adapter<GalleryViewHolder> {

        @NonNull
        @Override
        public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            int size = parent.getWidth() / 3;
            iv.setLayoutParams(new ViewGroup.LayoutParams(size > 0 ? size : 300, size > 0 ? size : 300));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(4, 4, 4, 4);
            return new GalleryViewHolder(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
            GalleryItem item = currentPhotos.get(position);
            Glide.with(GalleryActivity.this)
                    .load(item.uri)
                    .placeholder(R.color.cream_surface)
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> showFullScreenPhoto(item));
        }

        @Override
        public int getItemCount() {
            return currentPhotos.size();
        }
    }

    private static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView;
        }
    }

    private void showFullScreenPhoto(GalleryItem item) {
        ImageView previewImage = new ImageView(this);
        previewImage.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 900));
        previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(this).load(item.uri).into(previewImage);

        new AlertDialog.Builder(this)
                .setTitle(item.fileName)
                .setMessage("Folder: " + item.folderPath)
                .setView(previewImage)
                .setPositiveButton("Close", null)
                .show();
    }
}
