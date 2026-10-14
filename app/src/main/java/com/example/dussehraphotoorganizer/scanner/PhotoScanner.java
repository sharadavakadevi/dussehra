package com.example.dussehraphotoorganizer.scanner;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.model.PhotoItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * PhotoScanner scans public MediaStore photos, strictly enforcing camera-only capture rules
 * and matching only photos taken on configured Dussehra 2026 festival dates.
 */
public class PhotoScanner {

    private final Context context;
    private final AlankaramSchedule schedule;
    private final PhotoMetadataReader metadataReader;
    private final LocalHistoryRepository historyRepository;

    public interface ScanProgressListener {
        void onProgress(int scannedCount, int matchingCount);
    }

    public PhotoScanner(Context context) {
        this.context = context.getApplicationContext();
        this.schedule = new AlankaramSchedule(context);
        this.metadataReader = new PhotoMetadataReader(context);
        this.historyRepository = new LocalHistoryRepository(context);
    }

    public ScanResult scanCameraPhotos(ScanProgressListener listener) {
        List<PhotoItem> eligiblePhotos = new ArrayList<>();
        List<PhotoItem> reviewNeededPhotos = new ArrayList<>();
        int totalScanned = 0;
        int skippedCount = 0;

        String[] projection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.SIZE
            };
        } else {
            projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.SIZE
            };
        }

        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        String customCameraPath = historyRepository.getCustomCameraPath();

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int pathCol = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ? cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                        : cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                while (cursor.moveToNext()) {
                    totalScanned++;
                    long id = cursor.getLong(idCol);
                    String displayName = cursor.getString(nameCol);
                    String path = cursor.getString(pathCol);
                    long dateTaken = cursor.getLong(dateTakenCol);
                    long size = cursor.getLong(sizeCol);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // 1. Strict Camera Verification Check
                    boolean isCamera = metadataReader.isAuthenticCameraPhoto(path, displayName, customCameraPath);
                    if (!isCamera) {
                        // Strictly skip WhatsApp, Telegram, Screenshots, Downloads, etc.
                        skippedCount++;
                        continue;
                    }

                    // 2. Check if already processed (duplicate prevention)
                    if (historyRepository.isPhotoAlreadyProcessed(id, displayName, size)) {
                        skippedCount++;
                        continue;
                    }

                    // 3. Extract exact capture date from EXIF (or MediaStore creation fallback)
                    PhotoMetadataReader.MetadataResult meta = metadataReader.extractCaptureDate(contentUri, dateTaken);
                    if (!meta.isValid()) {
                        PhotoItem unverified = new PhotoItem(id, displayName, path, contentUri, 0, "Unknown", null, true);
                        unverified.setStatusMessage("Missing capture date (Needs Review)");
                        reviewNeededPhotos.add(unverified);
                        continue;
                    }

                    // 4. Match capture date against Dussehra 2026 Schedule
                    FestivalDateItem matchedFestival = schedule.getFestivalDateItem(meta.formattedDate);
                    if (matchedFestival == null) {
                        // Camera photo captured on non-festival date -> leave untouched!
                        skippedCount++;
                        continue;
                    }

                    // Eligible matching festival photo!
                    // Note: folderName does NOT contain date
                    PhotoItem eligible = new PhotoItem(
                            id,
                            displayName,
                            path,
                            contentUri,
                            meta.timestamp,
                            meta.formattedDate,
                            matchedFestival.getFolderName(),
                            true
                    );
                    eligible.setStatusMessage(matchedFestival.getTeluguName() + " (" + meta.source + ")");
                    eligiblePhotos.add(eligible);

                    if (listener != null && totalScanned % 20 == 0) {
                        listener.onProgress(totalScanned, eligiblePhotos.size());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ScanResult(eligiblePhotos, reviewNeededPhotos, totalScanned, skippedCount);
    }

    public static class ScanResult {
        public final List<PhotoItem> eligiblePhotos;
        public final List<PhotoItem> reviewNeededPhotos;
        public final int totalScanned;
        public final int skippedCount;

        public ScanResult(List<PhotoItem> eligiblePhotos, List<PhotoItem> reviewNeededPhotos, int totalScanned, int skippedCount) {
            this.eligiblePhotos = eligiblePhotos;
            this.reviewNeededPhotos = reviewNeededPhotos;
            this.totalScanned = totalScanned;
            this.skippedCount = skippedCount;
        }
    }
}
