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
    private String selectedFolder = null; // null means "All Folders"
    private String selectedRoot = null;   // null, "Dussehra_2026", or "Location_Organized"

    public static class GalleryItem {
        public final Uri uri;
        public final String folderName;
        public final String fileName;

        public GalleryItem(Uri uri, String folderName, String fileName) {
            this.uri = uri;
            this.folderName = folderName;
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
            selectedFolder = getIntent().getStringExtra("FILTER_FOLDER");
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
        allChip.setChecked(selectedFolder == null);
        allChip.setOnClickListener(v -> {
            selectedFolder = null;
            selectedRoot = null;
            loadPhotos();
        });
        chipGroupFolders.addView(allChip);

        // Location Folders
        for (LocationConfigItem loc : locationConfigManager.getRegisteredLocations()) {
            Chip chip = new Chip(this);
            chip.setText("📍 " + loc.getFolderName());
            chip.setCheckable(true);
            if (loc.getFolderName().equals(selectedFolder)) {
                chip.setChecked(true);
            }
            chip.setOnClickListener(v -> {
                selectedFolder = loc.getFolderName();
                selectedRoot = LocationConfigManager.ROOT_FOLDER_NAME;
                loadPhotos();
            });
            chipGroupFolders.addView(chip);
        }

        // Dussehra Festival Folders
        for (FestivalDateItem item : schedule.getSchedule()) {
            Chip chip = new Chip(this);
            chip.setText("🌸 " + item.getFolderName());
            chip.setCheckable(true);
            if (item.getFolderName().equals(selectedFolder)) {
                chip.setChecked(true);
            }
            chip.setOnClickListener(v -> {
                selectedFolder = item.getFolderName();
                selectedRoot = AlankaramSchedule.MAIN_FOLDER_NAME;
                loadPhotos();
            });
            chipGroupFolders.addView(chip);
        }
    }

    private void loadPhotos() {
        currentPhotos.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String selection;
            String[] selectionArgs;

            if (selectedFolder != null && selectedRoot != null) {
                selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                String folderQuery = Environment.DIRECTORY_PICTURES + "/" + selectedRoot + "/" + selectedFolder + "/%";
                selectionArgs = new String[]{folderQuery};
            } else if (selectedFolder != null) {
                selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                String folderQuery = "%/" + selectedFolder + "/%";
                selectionArgs = new String[]{folderQuery};
            } else {
                selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ? OR " + MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                String rootLoc = Environment.DIRECTORY_PICTURES + "/" + LocationConfigManager.ROOT_FOLDER_NAME + "/%";
                String rootFest = Environment.DIRECTORY_PICTURES + "/" + AlankaramSchedule.MAIN_FOLDER_NAME + "/%";
                selectionArgs = new String[]{rootLoc, rootFest};
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
            // Legacy files scan
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            scanLegacyDirectory(new File(picturesDir, LocationConfigManager.ROOT_FOLDER_NAME));
            scanLegacyDirectory(new File(picturesDir, AlankaramSchedule.MAIN_FOLDER_NAME));
        }

        if (currentPhotos.isEmpty()) {
            tvGalleryEmpty.setVisibility(View.VISIBLE);
        } else {
            tvGalleryEmpty.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void scanLegacyDirectory(File mainDir) {
        if (mainDir.exists() && mainDir.isDirectory()) {
            File[] subdirs = mainDir.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File sub : subdirs) {
                    if (selectedFolder == null || sub.getName().equals(selectedFolder)) {
                        File[] files = sub.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
                        if (files != null) {
                            for (File f : files) {
                                currentPhotos.add(new GalleryItem(Uri.fromFile(f), sub.getName(), f.getName()));
                            }
                        }
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
                .setMessage("Folder: " + item.folderName)
                .setView(previewImage)
                .setPositiveButton("Close", null)
                .show();
    }
}
