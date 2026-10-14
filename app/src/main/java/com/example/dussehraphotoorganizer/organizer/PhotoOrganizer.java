package com.example.dussehraphotoorganizer.organizer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.model.PhotoItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * PhotoOrganizer safely copies eligible camera photos into the public Pictures/Dussehra_2026/
 * festival subfolders (without dates in folder names).
 *
 * NEVER touches, modifies, moves, or deletes the original camera files.
 */
public class PhotoOrganizer {

    private final Context context;
    private final LocalHistoryRepository historyRepository;

    public interface CopyProgressListener {
        void onProgress(int current, int total, String currentFileName);
    }

    public PhotoOrganizer(Context context) {
        this.context = context.getApplicationContext();
        this.historyRepository = new LocalHistoryRepository(context);
    }

    public CopyResult copyPhotosToFestivalFolders(List<PhotoItem> photosToCopy, CopyProgressListener listener) {
        int copiedCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int total = photosToCopy.size();

        for (int i = 0; i < total; i++) {
            PhotoItem photo = photosToCopy.get(i);
            if (!photo.isSelected()) {
                skippedCount++;
                continue;
            }

            if (listener != null) {
                listener.onProgress(i + 1, total, photo.getFileName());
            }

            boolean success = copySinglePhoto(photo);
            if (success) {
                copiedCount++;
                // Record in history repository to prevent duplicate copies in future scans
                historyRepository.recordProcessedPhoto(photo.getId(), photo.getFileName(), 0);
            } else {
                failedCount++;
            }
        }

        String summary = String.format("Copied: %d, Skipped: %d, Failed: %d", copiedCount, skippedCount, failedCount);
        historyRepository.setLastRunSummary(summary);

        return new CopyResult(copiedCount, skippedCount, failedCount, summary);
    }

    private boolean copySinglePhoto(PhotoItem photo) {
        if (photo.getUri() == null || photo.getDestinationFolderName() == null) {
            return false;
        }

        String subfolder = photo.getDestinationFolderName(); // e.g. "Sri_Annapurna_Devi" (NO DATE)
        String relativeFolderPath = Environment.DIRECTORY_PICTURES + "/" + AlankaramSchedule.MAIN_FOLDER_NAME + "/" + subfolder + "/";
        String displayName = photo.getFileName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "IMG_" + System.currentTimeMillis() + ".jpg";
        }

        ContentResolver resolver = context.getContentResolver();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ Scoped MediaStore Public Directory insertion
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativeFolderPath);
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri destinationUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (destinationUri == null) {
                    return false;
                }

                try (InputStream in = resolver.openInputStream(photo.getUri());
                     OutputStream out = resolver.openOutputStream(destinationUri)) {
                    if (in == null || out == null) return false;
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }

                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(destinationUri, values, null, null);
                return true;

            } else {
                // Android 8 & 9 (API 26-28) Legacy Public Pictures directory
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File mainDir = new File(picturesDir, AlankaramSchedule.MAIN_FOLDER_NAME);
                File targetSubDir = new File(mainDir, subfolder);
                if (!targetSubDir.exists()) {
                    boolean created = targetSubDir.mkdirs();
                    if (!created && !targetSubDir.exists()) {
                        return false;
                    }
                }

                // Unique filename collision handling
                File destinationFile = new File(targetSubDir, displayName);
                if (destinationFile.exists()) {
                    String baseName = displayName.contains(".") ? displayName.substring(0, displayName.lastIndexOf('.')) : displayName;
                    String extension = displayName.contains(".") ? displayName.substring(displayName.lastIndexOf('.')) : ".jpg";
                    destinationFile = new File(targetSubDir, baseName + "_" + System.currentTimeMillis() + extension);
                }

                try (InputStream in = resolver.openInputStream(photo.getUri());
                     OutputStream out = new FileOutputStream(destinationFile)) {
                    if (in == null) return false;
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }

                // Trigger MediaScanner for Gallery visibility on older Android
                MediaStore.Images.Media.insertImage(resolver, destinationFile.getAbsolutePath(), destinationFile.getName(), null);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class CopyResult {
        public final int copiedCount;
        public final int skippedCount;
        public final int failedCount;
        public final String summary;

        public CopyResult(int copiedCount, int skippedCount, int failedCount, String summary) {
            this.copiedCount = copiedCount;
            this.skippedCount = skippedCount;
            this.failedCount = failedCount;
            this.summary = summary;
        }
    }
}
