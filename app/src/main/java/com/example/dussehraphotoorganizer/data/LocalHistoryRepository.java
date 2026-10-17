package com.example.dussehraphotoorganizer.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * LocalHistoryRepository stores processed media records to guarantee zero duplicate copies
 * across repeated scans, as well as configuration preferences for both Festival and Location modes.
 */
public class LocalHistoryRepository {

    private static final String PREF_NAME = "dussehra_history_prefs";
    
    // Festival processing history
    private static final String KEY_PROCESSED_MEDIA_IDS = "key_processed_media_ids";
    private static final String KEY_PROCESSED_FILE_SIGNATURES = "key_processed_file_signatures";
    private static final String KEY_PROCESSED_FILE_NAMES = "key_processed_file_names";
    private static final String KEY_LAST_RUN_SUMMARY = "key_last_run_summary";

    // Location processing history
    private static final String KEY_LOC_PROCESSED_MEDIA_IDS = "key_loc_processed_media_ids";
    private static final String KEY_LOC_PROCESSED_FILE_SIGNATURES = "key_loc_processed_file_signatures";
    private static final String KEY_LOC_PROCESSED_FILE_NAMES = "key_loc_processed_file_names";
    private static final String KEY_LAST_LOC_RUN_SUMMARY = "key_last_loc_run_summary";
    private static final String KEY_AUTO_LOCATION_ORGANIZE = "key_auto_location_organize";

    // General preferences
    private static final String KEY_INCLUDE_VIDEOS = "key_include_videos";
    private static final String KEY_AUTO_ORGANIZE = "key_auto_organize";
    private static final String KEY_CUSTOM_CAMERA_PATH = "key_custom_camera_path";

    private final SharedPreferences prefs;

    public LocalHistoryRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==========================================
    // Festival Processing
    // ==========================================
    public boolean isPhotoAlreadyProcessed(long mediaId, String fileName, long size) {
        if (fileName != null && !fileName.isEmpty()) {
            Set<String> nameSet = prefs.getStringSet(KEY_PROCESSED_FILE_NAMES, new HashSet<>());
            if (nameSet.contains(fileName)) {
                return true;
            }
        }
        if (mediaId > 0) {
            Set<String> idSet = prefs.getStringSet(KEY_PROCESSED_MEDIA_IDS, new HashSet<>());
            if (idSet.contains(String.valueOf(mediaId))) {
                return true;
            }
        }
        Set<String> sigSet = prefs.getStringSet(KEY_PROCESSED_FILE_SIGNATURES, new HashSet<>());
        if (sigSet.contains(fileName + "_" + size)) {
            return true;
        }
        if (size > 0 && sigSet.contains(fileName + "_0")) {
            return true;
        }
        return false;
    }

    public boolean isPhotoProcessedForFestival(long mediaId, String fileName, long size) {
        return isPhotoAlreadyProcessed(mediaId, fileName, size);
    }

    public synchronized void recordProcessedPhoto(long mediaId, String fileName, long size) {
        SharedPreferences.Editor editor = prefs.edit();

        if (mediaId > 0) {
            Set<String> idSet = new HashSet<>(prefs.getStringSet(KEY_PROCESSED_MEDIA_IDS, new HashSet<>()));
            idSet.add(String.valueOf(mediaId));
            editor.putStringSet(KEY_PROCESSED_MEDIA_IDS, idSet);
        }

        if (fileName != null && !fileName.isEmpty()) {
            Set<String> nameSet = new HashSet<>(prefs.getStringSet(KEY_PROCESSED_FILE_NAMES, new HashSet<>()));
            nameSet.add(fileName);
            editor.putStringSet(KEY_PROCESSED_FILE_NAMES, nameSet);

            Set<String> sigSet = new HashSet<>(prefs.getStringSet(KEY_PROCESSED_FILE_SIGNATURES, new HashSet<>()));
            sigSet.add(fileName + "_" + size);
            editor.putStringSet(KEY_PROCESSED_FILE_SIGNATURES, sigSet);
        }

        editor.apply();
    }

    public void setLastRunSummary(String summary) {
        prefs.edit().putString(KEY_LAST_RUN_SUMMARY, summary).apply();
    }

    public String getLastRunSummary() {
        return prefs.getString(KEY_LAST_RUN_SUMMARY, null);
    }

    // ==========================================
    // Location Processing
    // ==========================================
    public boolean isPhotoProcessedForLocation(long mediaId, String fileName, long size) {
        if (fileName != null && !fileName.isEmpty()) {
            Set<String> nameSet = prefs.getStringSet(KEY_LOC_PROCESSED_FILE_NAMES, new HashSet<>());
            if (nameSet.contains(fileName)) {
                return true;
            }
        }
        if (mediaId > 0) {
            Set<String> idSet = prefs.getStringSet(KEY_LOC_PROCESSED_MEDIA_IDS, new HashSet<>());
            if (idSet.contains(String.valueOf(mediaId))) {
                return true;
            }
        }
        Set<String> sigSet = prefs.getStringSet(KEY_LOC_PROCESSED_FILE_SIGNATURES, new HashSet<>());
        if (sigSet.contains(fileName + "_" + size)) {
            return true;
        }
        if (size > 0 && sigSet.contains(fileName + "_0")) {
            return true;
        }
        return false;
    }

    public synchronized void recordProcessedPhotoForLocation(long mediaId, String fileName, long size) {
        SharedPreferences.Editor editor = prefs.edit();

        if (mediaId > 0) {
            Set<String> idSet = new HashSet<>(prefs.getStringSet(KEY_LOC_PROCESSED_MEDIA_IDS, new HashSet<>()));
            idSet.add(String.valueOf(mediaId));
            editor.putStringSet(KEY_LOC_PROCESSED_MEDIA_IDS, idSet);
        }

        if (fileName != null && !fileName.isEmpty()) {
            Set<String> nameSet = new HashSet<>(prefs.getStringSet(KEY_LOC_PROCESSED_FILE_NAMES, new HashSet<>()));
            nameSet.add(fileName);
            editor.putStringSet(KEY_LOC_PROCESSED_FILE_NAMES, nameSet);

            Set<String> sigSet = new HashSet<>(prefs.getStringSet(KEY_LOC_PROCESSED_FILE_SIGNATURES, new HashSet<>()));
            sigSet.add(fileName + "_" + size);
            editor.putStringSet(KEY_LOC_PROCESSED_FILE_SIGNATURES, sigSet);
        }

        editor.apply();
    }

    public void setLastLocationRunSummary(String summary) {
        prefs.edit().putString(KEY_LAST_LOC_RUN_SUMMARY, summary).apply();
    }

    public String getLastLocationRunSummary() {
        return prefs.getString(KEY_LAST_LOC_RUN_SUMMARY, null);
    }

    public boolean isAutoLocationOrganizeEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOCATION_ORGANIZE, true); // Enabled by default for automatic operation
    }

    public void setAutoLocationOrganizeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_LOCATION_ORGANIZE, enabled).apply();
    }

    // ==========================================
    // Shared Preferences
    // ==========================================
    public boolean isIncludeVideos() {
        return prefs.getBoolean(KEY_INCLUDE_VIDEOS, false);
    }

    public void setIncludeVideos(boolean include) {
        prefs.edit().putBoolean(KEY_INCLUDE_VIDEOS, include).apply();
    }

    public boolean isAutoOrganizeEnabled() {
        return prefs.getBoolean(KEY_AUTO_ORGANIZE, false);
    }

    public void setAutoOrganizeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_ORGANIZE, enabled).apply();
    }

    public String getCustomCameraPath() {
        return prefs.getString(KEY_CUSTOM_CAMERA_PATH, "");
    }

    public void setCustomCameraPath(String path) {
        prefs.edit().putString(KEY_CUSTOM_CAMERA_PATH, path).apply();
    }

    public void clearHistory() {
        prefs.edit().clear().apply();
    }
}
