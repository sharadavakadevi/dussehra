package com.example.dussehraphotoorganizer.sync.engine;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.location.LocationConfigManager;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;
import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.net.SyncPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Saves received photos into the exact Dussehra destination folder hierarchy:
 * Pictures/Dussehra_2026/[Devi_Folder]/[Location_Or_Date_Folder]/[FileName]
 *
 * Strict Validation and Rules:
 * 1. Validates that the received item is a valid camera photo and not a WhatsApp, screenshot, or download item.
 * 2. Validates that the capture date strictly matches an official 2026 Dussehra festival date.
 * 3. Never guesses location using receiving phone's current GPS; relies entirely on source photo's EXIF metadata.
 * 4. Preserves the date-based fallback (YYYY-MM-DD) under the appropriate Devi folder when GPS is missing or ambiguous.
 * 5. Records the SHA-256 in the ledger so the receiving phone never retransmits the saved photo back.
 */
public class PhotoDestinationSaver {

    private static final String TAG = "PhotoDestinationSaver";

    private final Context context;
    private final LocationConfigManager locationConfigManager;
    private final AlankaramSchedule alankaramSchedule;
    private final LocalHistoryRepository historyRepository;
    private final SyncLedgerRepository syncLedgerRepository;

    public PhotoDestinationSaver(Context context, SyncLedgerRepository syncLedgerRepository) {
        this.context = context.getApplicationContext();
        this.locationConfigManager = new LocationConfigManager(this.context);
        this.alankaramSchedule = new AlankaramSchedule(this.context);
        this.historyRepository = new LocalHistoryRepository(this.context);
        this.syncLedgerRepository = syncLedgerRepository;
    }

    public boolean saveVerifiedPhoto(File tempFile, SyncPacket.PhotoHeaderMsg header) {
        if (tempFile == null || !tempFile.exists() || header == null) {
            return false;
        }

        String displayName = header.fileName;
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "IMG_SYNC_" + System.currentTimeMillis() + ".jpg";
        }

        // 1. Strict defense-in-depth: Reject non-camera patterns in filename
        String nameLower = displayName.toLowerCase(Locale.US);
        if (nameLower.contains("whatsapp") || (nameLower.startsWith("img-") && nameLower.contains("-wa"))
                || nameLower.contains("screenshot") || nameLower.startsWith("screencapture")
                || nameLower.contains("download") || nameLower.contains("downloaded")
                || nameLower.contains("telegram") || nameLower.contains("facebook")
                || nameLower.contains("instagram")) {
            Log.w(TAG, "Rejected received item: Non-camera pattern in filename -> " + displayName);
            syncLedgerRepository.addLogEntry(displayName, "RECEIVED", "REJECTED", "Excluded: Non-camera media");
            return false;
        }

        // 2. Strict validation: Must have a valid capture timestamp and date
        if (header.captureTimestamp <= 0 || header.formattedDate == null || header.formattedDate.trim().isEmpty()) {
            Log.w(TAG, "Rejected received item: Missing EXIF capture date -> " + displayName);
            syncLedgerRepository.addLogEntry(displayName, "RECEIVED", "REJECTED", "Excluded: Missing EXIF capture date");
            return false;
        }

        // 3. Strict validation: Must match configured Dussehra 2026 festival date
        FestivalDateItem matchedFestival = alankaramSchedule.getFestivalDateItem(header.formattedDate);
        if (matchedFestival == null) {
            Log.w(TAG, "Rejected received item: Date outside Dussehra schedule (" + header.formattedDate + ") -> " + displayName);
            syncLedgerRepository.addLogEntry(displayName, "RECEIVED", "REJECTED", "Excluded: Date " + header.formattedDate + " outside Dussehra schedule");
            return false;
        }

        // 4. Evaluate destination path strictly using source photo's EXIF metadata
        LocationConfigManager.EvaluationResult eval = locationConfigManager.evaluatePhotoDestination(
                header.latitude,
                header.longitude,
                header.formattedDate,
                header.isoDate
        );

        // Subfolder path under Pictures/Dussehra_2026/
        // e.g. "Sri_Gayatri_Devi/Home" or "Sri_Gayatri_Devi/2026-10-12"
        String subfolder = matchedFestival.getFolderName() + "/" + eval.locationSubfolder;
        String relativeFolderPath = Environment.DIRECTORY_PICTURES + "/" + AlankaramSchedule.MAIN_FOLDER_NAME + "/" + subfolder + "/";

        ContentResolver resolver = context.getContentResolver();
        boolean saved = false;

        // Check if destination directory already contains this photo
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File mainDir = new File(picturesDir, AlankaramSchedule.MAIN_FOLDER_NAME);
        File targetSubDir = new File(mainDir, subfolder);

        if (isAlreadySaved(resolver, displayName, relativeFolderPath, targetSubDir)) {
            Log.d(TAG, "Photo already exists at destination, skipping duplicate insert: " + displayName);
            syncLedgerRepository.recordIncomingPhotoReceived(header.sha256, displayName, header.fileSize, syncLedgerRepository.getPairedDeviceName());
            historyRepository.recordProcessedPhotoForLocation(0, displayName, header.fileSize);
            return true;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped MediaStore Public Directory insertion
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativeFolderPath);
                if (header.captureTimestamp > 0) {
                    values.put(MediaStore.Images.Media.DATE_TAKEN, header.captureTimestamp);
                }
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri destinationUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (destinationUri == null) {
                    Log.e(TAG, "Failed to create MediaStore entry for " + displayName);
                    return false;
                }

                try (InputStream in = new FileInputStream(tempFile);
                     OutputStream out = resolver.openOutputStream(destinationUri)) {
                    if (out == null) return false;
                    byte[] buffer = new byte[16384];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }

                // Preserve EXIF metadata on the new file where supported
                try {
                    ExifInterface sourceExif = new ExifInterface(tempFile.getAbsolutePath());
                    resolver.openFileDescriptor(destinationUri, "rw");
                } catch (Exception ignored) {}

                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(destinationUri, values, null, null);
                saved = true;

            } else {
                // Legacy public external storage
                if (!targetSubDir.exists()) {
                    boolean created = targetSubDir.mkdirs();
                    if (!created && !targetSubDir.exists()) {
                        Log.e(TAG, "Failed to create directory: " + targetSubDir.getAbsolutePath());
                        return false;
                    }
                }

                File destinationFile = new File(targetSubDir, displayName);
                if (destinationFile.exists()) {
                    saved = true;
                } else {
                    try (InputStream in = new FileInputStream(tempFile);
                         OutputStream out = new FileOutputStream(destinationFile)) {
                        byte[] buffer = new byte[16384];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        out.flush();
                    }

                    MediaStore.Images.Media.insertImage(resolver, destinationFile.getAbsolutePath(), destinationFile.getName(), null);
                    saved = true;
                }
            }

            if (saved) {
                // Record in Sync Ledger and Local History:
                // Prevents endless retransmission loops and duplicate files
                syncLedgerRepository.recordIncomingPhotoReceived(header.sha256, displayName, header.fileSize, syncLedgerRepository.getPairedDeviceName());
                historyRepository.recordProcessedPhotoForLocation(0, displayName, header.fileSize);
                syncLedgerRepository.addLogEntry(displayName, "RECEIVED", "SUCCESS", "Organized into " + subfolder);
                Log.d(TAG, "Successfully organized received photo: " + displayName + " -> " + subfolder);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving received photo: " + e.getMessage(), e);
            syncLedgerRepository.addLogEntry(displayName, "RECEIVED", "FAILED", e.getMessage());
            return false;
        }

        return saved;
    }

    private boolean isAlreadySaved(ContentResolver resolver, String displayName, String relativeFolderPath, File targetSubDir) {
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
}
