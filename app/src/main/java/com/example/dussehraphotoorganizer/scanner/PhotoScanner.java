package com.example.dussehraphotoorganizer.scanner;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.location.LocationConfigManager;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.model.PhotoItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * PhotoScanner scans public MediaStore photos, strictly enforcing camera-only capture rules,
 * matches photos taken on configured Dussehra 2026 festival dates, and sorts them into
 * Pictures/Dussehra_2026/[Devi_Alankaram]/[Home | Yeendda_Durga_Devi_Temple | Carshed_Bobleam | YYYY-MM-DD]/.
 */
public class PhotoScanner {

    private final Context context;
    private final AlankaramSchedule schedule;
    private final LocationConfigManager locationConfigManager;
    private final PhotoMetadataReader metadataReader;
    private final LocalHistoryRepository historyRepository;

    public interface ScanProgressListener {
        void onProgress(int scannedCount, int matchingCount);
    }

    public PhotoScanner(Context context) {
        this.context = context.getApplicationContext();
        this.schedule = new AlankaramSchedule(context);
        this.locationConfigManager = new LocationConfigManager(context);
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
                    MediaStore.Images.Media.DATA,
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
                int relPathCol = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ? cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                        : -1;
                int dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                while (cursor.moveToNext()) {
                    totalScanned++;
                    long id = cursor.getLong(idCol);
                    String displayName = cursor.getString(nameCol);
                    
                    String path = (relPathCol != -1) ? cursor.getString(relPathCol) : null;
                    if ((path == null || path.trim().isEmpty()) && dataCol != -1) {
                        path = cursor.getString(dataCol);
                    }
                    
                    long dateTaken = cursor.getLong(dateTakenCol);
                    long size = cursor.getLong(sizeCol);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // 1. Strict Camera Verification Check
                    boolean isCamera = metadataReader.isAuthenticCameraPhoto(path, displayName, customCameraPath);
                    if (!isCamera) {
                        skippedCount++;
                        continue;
                    }

                    // 2. Duplicate Check: Check if already organized in Festival pipeline
                    boolean alreadyProcessed = historyRepository.isPhotoAlreadyProcessed(id, displayName, size);

                    // 3. Extract exact capture date and GPS from EXIF
                    PhotoMetadataReader.PhotoFullMetadata meta = metadataReader.extractFullMetadata(contentUri, dateTaken);
                    if (!meta.hasValidCaptureDate()) {
                        PhotoItem unverified = new PhotoItem(id, displayName, path, contentUri, 0, "Unknown", null, true);
                        unverified.setStatusMessage("Missing capture date (Needs Review)");
                        reviewNeededPhotos.add(unverified);
                        continue;
                    }

                    // 4. Match capture date against Dussehra 2026 Schedule
                    FestivalDateItem matchedFestival = schedule.getFestivalDateItem(meta.formattedDate);
                    if (matchedFestival == null) {
                        // Camera photo captured on non-festival date
                        skippedCount++;
                        continue;
                    }

                    // 5. Evaluate Location under the Devi folder
                    LocationConfigManager.EvaluationResult eval = locationConfigManager.evaluatePhotoDestination(
                            meta.latitude,
                            meta.longitude,
                            meta.formattedDate,
                            meta.isoFormattedDate
                    );

                    String destinationSubPath = matchedFestival.getFolderName() + "/" + eval.locationSubfolder;

                    PhotoItem eligible = new PhotoItem(
                            id,
                            displayName,
                            path,
                            contentUri,
                            meta.timestamp,
                            meta.formattedDate,
                            destinationSubPath,
                            true
                    );
                    eligible.setRootDirectory(AlankaramSchedule.MAIN_FOLDER_NAME);
                    eligible.setFileSize(size);
                    eligible.setAlreadyCopied(alreadyProcessed);
                    eligible.setSelected(!alreadyProcessed);
                    if (alreadyProcessed) {
                        eligible.setStatusMessage("✓ Copied to Pictures/" + destinationSubPath);
                    } else {
                        eligible.setStatusMessage(matchedFestival.getTeluguName() + " (" + eval.statusSummary + ")");
                    }
                    eligible.setLocationLabel(eval.selectedAlias != null ? eval.selectedAlias : ("Date: " + eval.locationSubfolder));
                    if (meta.hasGpsCoordinates()) {
                        eligible.setLatitude(meta.latitude);
                        eligible.setLongitude(meta.longitude);
                    }
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
