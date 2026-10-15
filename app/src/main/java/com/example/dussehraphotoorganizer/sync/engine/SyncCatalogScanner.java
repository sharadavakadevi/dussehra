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
import com.example.dussehraphotoorganizer.scanner.PhotoMetadataReader;
import com.example.dussehraphotoorganizer.sync.crypto.SyncCryptoUtil;
import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.net.SyncPacket;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans local camera photos and produces the catalog of photos available for synchronization.
 * Caches calculated SHA-256 hashes by Media ID + Size + Timestamp to ensure near-instant scans.
 */
public class SyncCatalogScanner {

    private static final String TAG = "SyncCatalogScanner";
    private static final String PREF_HASH_CACHE = "dussehra_hash_cache";

    private final Context context;
    private final PhotoMetadataReader metadataReader;
    private final LocalHistoryRepository historyRepository;
    private final SyncLedgerRepository syncLedgerRepository;
    private final SharedPreferences hashCachePrefs;

    public SyncCatalogScanner(Context context, SyncLedgerRepository syncLedgerRepository) {
        this.context = context.getApplicationContext();
        this.metadataReader = new PhotoMetadataReader(this.context);
        this.historyRepository = new LocalHistoryRepository(this.context);
        this.syncLedgerRepository = syncLedgerRepository;
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

                    // 1. Strict authentic camera check
                    if (!metadataReader.isAuthenticCameraPhoto(path, displayName, customCameraPath)) {
                        continue;
                    }

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // 2. Resolve SHA-256 from cache or compute
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

                    // 3. Extract EXIF details
                    PhotoMetadataReader.PhotoFullMetadata meta = metadataReader.extractFullMetadata(contentUri, dateTaken);

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
            Log.e(TAG, "Error scanning local photos for sync: " + e.getMessage());
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
