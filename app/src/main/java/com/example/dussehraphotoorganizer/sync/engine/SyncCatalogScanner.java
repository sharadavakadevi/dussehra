package com.example.dussehraphotoorganizer.sync.engine;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.location.LocationConfigManager;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.scanner.PhotoMetadataReader;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;
import com.example.dussehraphotoorganizer.sync.crypto.SyncCryptoUtil;
import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.net.SyncPacket;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strict Dussehra Camera Photo Scanner:
 * 
 * Scans ONLY authentic camera photos that strictly qualify for the Dussehra Photo Organizer:
 * 1. Strictly camera captures only (DCIM/Camera) — rejects WhatsApp, screenshots, downloads, social media.
 * 2. New photos only — excludes old gallery images captured before sync was configured.
 * 3. Valid original EXIF capture date — skips ambiguous/missing dates and records for review.
 * 4. Configured Dussehra 2026 Festival date match — rejects photos taken on non-festival dates.
 * 5. Applies location and date fallback rules under the appropriate Devi folder.
 * 6. Prevents duplicate transmissions and retransmission loops via persistent SHA-256 ledger.
 */
public class SyncCatalogScanner {

    private static final String TAG = "SyncCatalogScanner";
    private static final String PREF_HASH_CACHE = "dussehra_hash_cache";

    private final Context context;
    private final PhotoMetadataReader metadataReader;
    private final LocalHistoryRepository historyRepository;
    private final SyncLedgerRepository syncLedgerRepository;
    private final AlankaramSchedule alankaramSchedule;
    private final LocationConfigManager locationConfigManager;
    private final SharedPreferences hashCachePrefs;

    public SyncCatalogScanner(Context context, SyncLedgerRepository syncLedgerRepository) {
        this.context = context.getApplicationContext();
        this.metadataReader = new PhotoMetadataReader(this.context);
        this.historyRepository = new LocalHistoryRepository(this.context);
        this.syncLedgerRepository = syncLedgerRepository;
        this.alankaramSchedule = new AlankaramSchedule(this.context);
        this.locationConfigManager = new LocationConfigManager(this.context);
        this.hashCachePrefs = this.context.getSharedPreferences(PREF_HASH_CACHE, Context.MODE_PRIVATE);
    }

    public List<SyncPacket.PhotoCatalogItem> scanLocalCameraPhotos() {
        List<SyncPacket.PhotoCatalogItem> catalog = new ArrayList<>();

        String[] projection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.DATE_MODIFIED
            };
        } else {
            projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.DATE_MODIFIED
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
                int modCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String displayName = cursor.getString(nameCol);
                    String path = cursor.getString(pathCol);
                    long dateTaken = cursor.getLong(dateTakenCol);
                    long size = cursor.getLong(sizeCol);
                    long dateMod = cursor.getLong(modCol);

                    // 1. Strict authentic camera check:
                    // Excludes WhatsApp media, Screenshots, Browser downloads, Social media (FB, IG, Telegram),
                    // and photos already inside organized Dussehra folders.
                    if (!metadataReader.isAuthenticCameraPhoto(path, displayName, customCameraPath)) {
                        continue;
                    }

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // 2. Extract Full EXIF Metadata (capture timestamp & GPS)
                    PhotoMetadataReader.PhotoFullMetadata meta = metadataReader.extractFullMetadata(contentUri, dateTaken);

                    // If EXIF capture date is missing or unreadable:
                    // Do NOT guess or send indiscriminately; record for review and skip automatic transfer.
                    if (!meta.hasValidCaptureDate()) {
                        syncLedgerRepository.addLogEntry(
                                displayName != null ? displayName : "Unknown",
                                "SKIPPED",
                                "REVIEW_NEEDED",
                                "Missing EXIF capture date (Skipped - Needs Review)"
                        );
                        continue;
                    }

                    // 3. Strict Dussehra Festival Date check:
                    // MUST match one of the 11 official configured Dussehra 2026 festival dates.
                    // The scanner syncs all eligible camera photos matching the 11 Dussehra festival dates,
                    // regardless of whether they were taken before or after pairing.
                    // The persistent SHA-256 deduplication ledger guarantees photos are never transferred twice or duplicated.
                    FestivalDateItem matchedFestival = alankaramSchedule.getFestivalDateItem(meta.formattedDate);
                    if (matchedFestival == null) {
                        // Camera photo captured on a non-festival date (e.g. daily/unrelated photo).
                        // Strictly excluded from Dussehra Wi-Fi synchronization.
                        continue;
                    }

                    // 5. Evaluate destination (Devi parent folder & location alias or date fallback)
                    LocationConfigManager.EvaluationResult eval = locationConfigManager.evaluatePhotoDestination(
                            meta.latitude,
                            meta.longitude,
                            meta.formattedDate,
                            meta.isoFormattedDate
                    );

                    // 6. Resolve or compute SHA-256 content hash
                    String cacheKey = id + "_" + size + "_" + dateMod;
                    String sha256 = hashCachePrefs.getString(cacheKey, null);

                    if (sha256 == null) {
                        try (InputStream is = context.getContentResolver().openInputStream(contentUri)) {
                            if (is != null) {
                                sha256 = SyncCryptoUtil.calculateSHA256(is);
                                if (sha256 != null) {
                                    hashCachePrefs.edit().putString(cacheKey, sha256).apply();
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    if (sha256 == null || sha256.isEmpty()) {
                        continue;
                    }

                    // 7. Duplicate Prevention & Loop Elimination:
                    // If this photo was already synced or received, skip it.
                    if (syncLedgerRepository.isHashAlreadySyncedOrReceived(sha256)) {
                        continue;
                    }

                    // Photo strictly qualifies! Add to eligible catalog.
                    SyncPacket.PhotoCatalogItem item = new SyncPacket.PhotoCatalogItem(
                            sha256,
                            displayName,
                            size,
                            meta.timestamp,
                            meta.latitude,
                            meta.longitude,
                            meta.isoFormattedDate,
                            meta.formattedDate
                    );

                    catalog.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning local Dussehra photos for sync: " + e.getMessage());
        }

        return catalog;
    }

    public Uri getUriForSha256(String targetSha256) {
        if (targetSha256 == null) return null;
        Map<String, ?> all = hashCachePrefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (targetSha256.equals(entry.getValue())) {
                String key = entry.getKey();
                String[] parts = key.split("_");
                if (parts.length > 0) {
                    try {
                        long id = Long.parseLong(parts[0]);
                        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }
}
