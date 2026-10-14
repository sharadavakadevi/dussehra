package com.example.dussehraphotoorganizer.scanner;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.location.LocationConfigManager;
import com.example.dussehraphotoorganizer.model.PhotoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * LocationPhotoScanner scans public MediaStore photos strictly from camera captures (DCIM/Camera),
 * reads unredacted EXIF GPS coordinates, checks against registered physical locations offline,
 * and falls back to ISO date folders (YYYY-MM-DD) for unmatched or GPS-missing photos.
 * 
 * Never guesses capture date from file-modified date; photos without verified capture dates are skipped.
 */
public class LocationPhotoScanner {

    private final Context context;
    private final LocationConfigManager locationConfigManager;
    private final PhotoMetadataReader metadataReader;
    private final LocalHistoryRepository historyRepository;

    public interface ScanProgressListener {
        void onProgress(int scannedCount, int matchingCount);
    }

    public LocationPhotoScanner(Context context) {
        this.context = context.getApplicationContext();
        this.locationConfigManager = new LocationConfigManager(context);
        this.metadataReader = new PhotoMetadataReader(context);
        this.historyRepository = new LocalHistoryRepository(context);
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

                    // 1. Strict Camera Verification Check: only authentic camera photos
                    boolean isCamera = metadataReader.isAuthenticCameraPhoto(path, displayName, customCameraPath);
                    if (!isCamera) {
                        skippedCount++;
                        continue;
                    }

                    // 2. Duplicate Prevention: check if already organized in Location pipeline
                    if (historyRepository.isPhotoProcessedForLocation(id, displayName, size)) {
                        skippedCount++;
                        continue;
                    }

                    // 3. Extract Full EXIF Metadata (Date & GPS)
                    PhotoMetadataReader.PhotoFullMetadata meta = metadataReader.extractFullMetadata(contentUri, dateTaken);

                    String destinationFolder;
                    String statusMessage;
                    String locationLabel;

                    if (meta.hasGpsCoordinates()) {
                        LocationConfigManager.MatchResult match = locationConfigManager.matchLocation(meta.latitude, meta.longitude);
                        if (match.isMatched) {
                            destinationFolder = match.matchedLocation.getFolderName();
                            locationLabel = match.matchedLocation.getName();
                            statusMessage = "📍 " + match.matchedLocation.getName() + " (" + Math.round(match.distanceMeters) + "m)";
                        } else {
                            // GPS coordinates present but do not match any registered location -> use ISO Date folder
                            if (meta.hasValidCaptureDate()) {
                                destinationFolder = meta.isoFormattedDate;
                                locationLabel = "Date: " + meta.isoFormattedDate;
                                statusMessage = "📅 Unmatched GPS -> Date: " + meta.isoFormattedDate;
                            } else {
                                // Missing capture date: skip and record for review
                                PhotoItem unverified = new PhotoItem(id, displayName, path, contentUri, 0, "Unknown", null, true);
                                unverified.setRootDirectory(LocationConfigManager.ROOT_FOLDER_NAME);
                                unverified.setStatusMessage("Missing capture date (Skipped - Needs Review)");
                                reviewNeededPhotos.add(unverified);
                                skippedCount++;
                                continue;
                            }
                        }
                    } else {
                        // GPS coordinates missing -> use ISO Date folder (YYYY-MM-DD)
                        if (meta.hasValidCaptureDate()) {
                            destinationFolder = meta.isoFormattedDate;
                            locationLabel = "Date: " + meta.isoFormattedDate;
                            statusMessage = "📅 No GPS -> Date: " + meta.isoFormattedDate;
                        } else {
                            // Missing capture date: skip and record for review
                            PhotoItem unverified = new PhotoItem(id, displayName, path, contentUri, 0, "Unknown", null, true);
                            unverified.setRootDirectory(LocationConfigManager.ROOT_FOLDER_NAME);
                            unverified.setStatusMessage("Missing capture date (Skipped - Needs Review)");
                            reviewNeededPhotos.add(unverified);
                            skippedCount++;
                            continue;
                        }
                    }

                    // Create eligible PhotoItem for location organization
                    PhotoItem eligible = new PhotoItem(
                            id,
                            displayName,
                            path,
                            contentUri,
                            meta.timestamp,
                            meta.isoFormattedDate != null ? meta.isoFormattedDate : meta.formattedDate,
                            destinationFolder,
                            true
                    );
                    eligible.setRootDirectory(LocationConfigManager.ROOT_FOLDER_NAME);
                    eligible.setStatusMessage(statusMessage);
                    eligible.setLocationLabel(locationLabel);
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
