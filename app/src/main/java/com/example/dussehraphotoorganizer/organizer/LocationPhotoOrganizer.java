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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * LocationPhotoOrganizer safely copies eligible camera photos into the public
 * Pictures/Dussehra_2026/[Devi_Folder]/[Location_Or_Date_Folder]/ structure.
 *
 * Folders include:
 * - Pictures/Dussehra_2026/Sri_Gayatri_Devi/Home/
 * - Pictures/Dussehra_2026/Sri_Gayatri_Devi/Yeendda_Durga_Devi_Temple/
 * - Pictures/Dussehra_2026/Sri_Gayatri_Devi/Carshed_Bobleam/
 * - Pictures/Dussehra_2026/Sri_Gayatri_Devi/YYYY-MM-DD/
 * - Pictures/Dussehra_2026/Other_Dates/... (for dates outside festival schedule)
 *
 * STRICT SAFETY:
 * NEVER touches, modifies, moves, or deletes original camera photos.
 * Prevents duplicates and handles filename collisions safely.
 */
public class LocationPhotoOrganizer {

    public static volatile boolean isOrganizing = false;

    private final Context context;
    private final LocalHistoryRepository historyRepository;

    public interface CopyProgressListener {
        void onProgress(int current, int total, String currentFileName, String destinationFolder);
    }

    public LocationPhotoOrganizer(Context context) {
        this.context = context.getApplicationContext();
        this.historyRepository = new LocalHistoryRepository(context);
    }

    public CopyResult copyPhotosToLocationFolders(List<PhotoItem> photosToCopy, CopyProgressListener listener) {
        int copiedCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int total = photosToCopy.size();

        isOrganizing = true;
        try {
            for (int i = 0; i < total; i++) {
                PhotoItem photo = photosToCopy.get(i);
                if (!photo.isSelected()) {
                    skippedCount++;
                    continue;
                }

                if (listener != null) {
                    listener.onProgress(i + 1, total, photo.getFileName(), photo.getDestinationFolderName());
                }

                boolean success = copySinglePhoto(photo);
                if (success) {
                    copiedCount++;
                    historyRepository.recordProcessedPhotoForLocation(photo.getId(), photo.getFileName(), photo.getFileSize());
                } else {
                    failedCount++;
                }
            }
        } finally {
            isOrganizing = false;
        }

        String summary = String.format("Copied: %d, Skipped: %d, Failed: %d", copiedCount, skippedCount, failedCount);
        historyRepository.setLastLocationRunSummary(summary);

        return new CopyResult(copiedCount, skippedCount, failedCount, summary);
    }

    public boolean copySinglePhoto(PhotoItem photo) {
        if (photo.getUri() == null || photo.getDestinationFolderName() == null) {
            return false;
        }

        // Subfolder path is e.g. "Sri_Gayatri_Devi/Home" or "Sri_Gayatri_Devi/2026-10-12"
        String subfolder = photo.getDestinationFolderName();
        String relativeFolderPath = Environment.DIRECTORY_PICTURES + "/" + AlankaramSchedule.MAIN_FOLDER_NAME + "/" + subfolder + "/";
        String displayName = photo.getFileName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "IMG_" + System.currentTimeMillis() + ".jpg";
        }

        ContentResolver resolver = context.getContentResolver();

        // Check if directory on filesystem is known
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File mainDir = new File(picturesDir, AlankaramSchedule.MAIN_FOLDER_NAME);
        File targetSubDir = new File(mainDir, subfolder);

        // PRE-INSERT DEDUPLICATION CHECK:
        // Crucial: If file already exists in destination folder, DO NOT insert again!
        // Calling resolver.insert on an existing file forces MediaStore to create numbered duplicates like (1).jpg, (2).jpg.
        if (isFileAlreadyInDestination(resolver, displayName, relativeFolderPath, targetSubDir)) {
            historyRepository.recordProcessedPhotoForLocation(photo.getId(), photo.getFileName(), photo.getFileSize());
            return true;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29-35) Scoped MediaStore Public Directory insertion
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativeFolderPath);
                if (photo.getCaptureTimestamp() > 0) {
                    values.put(MediaStore.Images.Media.DATE_TAKEN, photo.getCaptureTimestamp());
                }
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
                if (!targetSubDir.exists()) {
                    boolean created = targetSubDir.mkdirs();
                    if (!created && !targetSubDir.exists()) {
                        return false;
                    }
                }

                File destinationFile = new File(targetSubDir, displayName);
                if (destinationFile.exists()) {
                    // Photo already exists in destination, skip to avoid duplicate
                    return true;
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

                MediaStore.Images.Media.insertImage(resolver, destinationFile.getAbsolutePath(), destinationFile.getName(), null);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isFileAlreadyInDestination(ContentResolver resolver, String displayName, String relativeFolderPath, File targetSubDir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String cleanRelPath = relativeFolderPath.endsWith("/") ? relativeFolderPath : (relativeFolderPath + "/");
            String altRelPath = relativeFolderPath.endsWith("/") ? relativeFolderPath.substring(0, relativeFolderPath.length() - 1) : relativeFolderPath;
            String selection = MediaStore.Images.Media.DISPLAY_NAME + "=? AND (" +
                    MediaStore.Images.Media.RELATIVE_PATH + "=? OR " +
                    MediaStore.Images.Media.RELATIVE_PATH + "=?)";
            String[] selectionArgs = new String[]{displayName, cleanRelPath, altRelPath};

            try (android.database.Cursor cursor = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID},
                    selection,
                    selectionArgs,
                    null
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }

        if (targetSubDir != null && targetSubDir.exists()) {
            File dest = new File(targetSubDir, displayName);
            if (dest.exists()) {
                return true;
            }
        }
        return false;
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
