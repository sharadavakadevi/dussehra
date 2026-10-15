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
 * LocationPhotoScanner scans authentic camera photos (DCIM/Camera),
 * extracts EXIF capture timestamps and unredacted GPS coordinates,
 * and routes photos into Pictures/Dussehra_2026/[Devi_Folder]/[Location_Or_Date_Folder]/.
 * 
 * Target Folders:
 * - Pictures/Dussehra_2026/[Devi_Folder]/Home/
 * - Pictures/Dussehra_2026/[Devi_Folder]/Yeendda_Durga_Devi_Temple/
 * - Pictures/Dussehra_2026/[Devi_Folder]/Carshed_Bobleam/
 * - Pictures/Dussehra_2026/[Devi_Folder]/YYYY-MM-DD/ (for no GPS, unmatched, or ambiguous GPS)
 */
public class LocationPhotoScanner {

    private final Context context;
    private final LocationConfigManager locationConfigManager;
    private final PhotoMetadataReader metadataReader;
    private final LocalHistoryRepository historyRepository;
    private final AlankaramSchedule schedule;

    public interface ScanProgressListener {
        void onProgress(int scannedCount, int matchingCount);
    }

    public LocationPhotoScanner(Context context) {
        this.context = context.getApplicationContext();
        this.locationConfigManager = new LocationConfigManager(context);
        this.metadataReader = new PhotoMetadataReader(context);
        this.historyRepository = new LocalHistoryRepository(context);
        this.schedule = new AlankaramSchedule(context);
    }

    public LocationScanResult scanCameraPhotos(ScanProgressListener listener) {
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
                    
                    // Robust path extraction: try RELATIVE_PATH, fallback to DATA
                    String path = (relPathCol != -1) ? cursor.getString(relPathCol) : null;
                    if ((path == null || path.trim().isEmpty()) && dataCol != -1) {
                        path = cursor.getString(dataCol);
                    }
                    
                    long dateTaken = cursor.getLong(dateTakenCol);
                    long size = cursor.getLong(sizeCol);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // 1. Strict Camera Verification Check: only authentic camera photos
                    boolean isCamera = metadataReader.isAuthenticCameraPhoto(path, displayName, customCameraPath);
                    if (!isCamera) {
                        skippedCount++;
                        continue;
                    }

                    // 2. Duplicate Check: Check if already organized in Location pipeline
                    boolean alreadyProcessed = historyRepository.isPhotoProcessedForLocation(id, displayName, size);

                    // 3. Extract Full EXIF Metadata (Date & GPS)
                    PhotoMetadataReader.PhotoFullMetadata meta = metadataReader.extractFullMetadata(contentUri, dateTaken);

                    // Check capture date validity: if missing, record for review rather than guessing
                    if (!meta.hasValidCaptureDate()) {
                        PhotoItem unverified = new PhotoItem(id, displayName, path, contentUri, 0, "Unknown", null, true);
                        unverified.setRootDirectory(AlankaramSchedule.MAIN_FOLDER_NAME);
                        unverified.setStatusMessage("Missing EXIF capture date (Skipped - Needs Review)");
                        reviewNeededPhotos.add(unverified);
                        skippedCount++;
                        continue;
                    }

                    // 4. Evaluate Destination path under Pictures/Dussehra_2026/
                    // Uses LocationConfigManager to map to Devi Alankaram folder if on festival date,
                    // or to Other_Dates folder if outside festival schedule.
                    LocationConfigManager.EvaluationResult eval = locationConfigManager.evaluatePhotoDestination(
                            meta.latitude,
                            meta.longitude,
                            meta.formattedDate,
                            meta.isoFormattedDate
                    );

                    // Destination subpath: e.g. "Sri_Gayatri_Devi/Home" or "Other_Dates/Home" or "Sri_Gayatri_Devi/2026-10-12"
                    String destinationSubPath = eval.deviParentFolder + "/" + eval.locationSubfolder;

                    // Create eligible PhotoItem with alreadyCopied status for visual highlighting
                    PhotoItem eligible = new PhotoItem(
                            id,
                            displayName,
                            path,
                            contentUri,
                            meta.timestamp,
                            meta.isoFormattedDate != null ? meta.isoFormattedDate : meta.formattedDate,
                            destinationSubPath,
                            true
                    );
                    eligible.setRootDirectory(AlankaramSchedule.MAIN_FOLDER_NAME);
                    eligible.setFileSize(size);
                    eligible.setAlreadyCopied(alreadyProcessed);
                    eligible.setSelected(!alreadyProcessed); // Unselected if already copied to prevent redundant copying
                    if (alreadyProcessed) {
                        eligible.setStatusMessage("✓ Copied to Pictures/" + destinationSubPath);
                    } else {
                        eligible.setStatusMessage(eval.statusSummary);
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

        return new LocationScanResult(eligiblePhotos, reviewNeededPhotos, totalScanned, skippedCount);
    }

    public static class LocationScanResult {
        public final List<PhotoItem> eligiblePhotos;
        public final List<PhotoItem> reviewNeededPhotos;
        public final int totalScanned;
        public final int skippedCount;

        public LocationScanResult(List<PhotoItem> eligiblePhotos, List<PhotoItem> reviewNeededPhotos, int totalScanned, int skippedCount) {
            this.eligiblePhotos = eligiblePhotos;
            this.reviewNeededPhotos = reviewNeededPhotos;
            this.totalScanned = totalScanned;
            this.skippedCount = skippedCount;
        }
    }
}
