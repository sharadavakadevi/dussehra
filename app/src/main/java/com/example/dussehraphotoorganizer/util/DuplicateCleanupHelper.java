package com.example.dussehraphotoorganizer.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DuplicateCleanupHelper safely scans Pictures/Dussehra_2026/ for numbered duplicate copies
 * (e.g. "IMG_20261013_064738 (1).jpg", "(2).jpg", "(3).jpg") generated during MediaStore collisions,
 * and removes the redundant copies while preserving the single pristine original file ("IMG_20261013_064738.jpg").
 */
public class DuplicateCleanupHelper {

    private static final String TAG = "DuplicateCleanupHelper";
    private static final Pattern DUPLICATE_PATTERN = Pattern.compile("^(.+?)\\s*\\(\\d+\\)\\.([a-zA-Z0-9]+)$");

    private final Context context;

    public interface CleanupCallback {
        void onComplete(int removedCount, long freedBytes, String message);
    }

    public DuplicateCleanupHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void cleanDuplicates(CleanupCallback callback) {
        new Thread(() -> {
            int removedCount = 0;
            long freedBytes = 0;

            try {
                ContentResolver resolver = context.getContentResolver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Query all photos inside Pictures/Dussehra_2026/
                    String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
                    String[] selectionArgs = new String[]{"Pictures/" + AlankaramSchedule.MAIN_FOLDER_NAME + "/%"};

                    String[] projection = new String[]{
                            MediaStore.Images.Media._ID,
                            MediaStore.Images.Media.DISPLAY_NAME,
                            MediaStore.Images.Media.RELATIVE_PATH,
                            MediaStore.Images.Media.SIZE
                    };

                    Map<String, List<DuplicateCandidate>> mapByFolder = new HashMap<>();

                    try (Cursor cursor = resolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            null
                    )) {
                        if (cursor != null) {
                            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                            int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                            int pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);
                            int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                            while (cursor.moveToNext()) {
                                long id = cursor.getLong(idCol);
                                String name = cursor.getString(nameCol);
                                String path = cursor.getString(pathCol);
                                long size = cursor.getLong(sizeCol);

                                if (path == null) path = "";
                                if (!mapByFolder.containsKey(path)) {
                                    mapByFolder.put(path, new ArrayList<>());
                                }
                                mapByFolder.get(path).add(new DuplicateCandidate(id, name, path, size));
                            }
                        }
                    }

                    // Process each folder: find duplicates matching original files
                    for (Map.Entry<String, List<DuplicateCandidate>> entry : mapByFolder.entrySet()) {
                        List<DuplicateCandidate> items = entry.getValue();
                        Map<String, DuplicateCandidate> masterMap = new HashMap<>();

                        // Identify master files (without numbered suffixes)
                        for (DuplicateCandidate item : items) {
                            Matcher m = DUPLICATE_PATTERN.matcher(item.displayName);
                            if (!m.matches()) {
                                masterMap.put(item.displayName, item);
                            }
                        }

                        // Remove duplicates that match an existing master file or another identical duplicate
                        for (DuplicateCandidate item : items) {
                            Matcher m = DUPLICATE_PATTERN.matcher(item.displayName);
                            if (m.matches()) {
                                String baseName = m.group(1);
                                String ext = m.group(2);
                                String originalName = baseName + "." + ext;

                                // If master file exists in the folder, delete the numbered duplicate
                                if (masterMap.containsKey(originalName)) {
                                    Uri itemUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id);
                                    try {
                                        int deleted = resolver.delete(itemUri, null, null);
                                        if (deleted > 0) {
                                            removedCount++;
                                            freedBytes += item.size;
                                            Log.d(TAG, "Safely removed duplicate copy: " + item.displayName + " from " + item.relativePath);
                                        }
                                    } catch (Exception e) {
                                        Log.w(TAG, "Could not delete duplicate " + item.displayName + ": " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // Pre-Android 10: Filesystem scan
                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File mainDir = new File(picturesDir, AlankaramSchedule.MAIN_FOLDER_NAME);
                    if (mainDir.exists() && mainDir.isDirectory()) {
                        int[] stats = cleanDirectoryRecursive(mainDir);
                        removedCount = stats[0];
                        freedBytes = stats[1];
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during duplicate cleanup: " + e.getMessage(), e);
            }

            final int finalRemoved = removedCount;
            final long finalFreed = freedBytes;
            double mbFreed = (double) finalFreed / (1024 * 1024);
            final String msg = String.format("Cleaned %d duplicate copies (Freed %.2f MB)", finalRemoved, mbFreed);

            if (callback != null) {
                callback.onComplete(finalRemoved, finalFreed, msg);
            }
        }).start();
    }

    private int[] cleanDirectoryRecursive(File dir) {
        int count = 0;
        long bytes = 0;
        File[] files = dir.listFiles();
        if (files == null) return new int[]{0, 0};

        Map<String, File> originalFiles = new HashMap<>();
        for (File f : files) {
            if (f.isFile()) {
                Matcher m = DUPLICATE_PATTERN.matcher(f.getName());
                if (!m.matches()) {
                    originalFiles.put(f.getName(), f);
                }
            }
        }

        for (File f : files) {
            if (f.isDirectory()) {
                int[] sub = cleanDirectoryRecursive(f);
                count += sub[0];
                bytes += sub[1];
            } else if (f.isFile()) {
                Matcher m = DUPLICATE_PATTERN.matcher(f.getName());
                if (m.matches()) {
                    String originalName = m.group(1) + "." + m.group(2);
                    if (originalFiles.containsKey(originalName)) {
                        long size = f.length();
                        if (f.delete()) {
                            count++;
                            bytes += size;
                        }
                    }
                }
            }
        }

        return new int[]{count, (int) bytes};
    }

    private static class DuplicateCandidate {
        final long id;
        final String displayName;
        final String relativePath;
        final long size;

        DuplicateCandidate(long id, String displayName, String relativePath, long size) {
            this.id = id;
            this.displayName = displayName;
            this.relativePath = relativePath;
            this.size = size;
        }
    }
}
