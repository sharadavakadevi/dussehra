package com.example.dussehraphotoorganizer.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * LocalHistoryRepository stores processed media records to guarantee zero duplicate copies
 * across repeated scans, as well as configuration preferences.
 */
public class LocalHistoryRepository {

    private static final String PREF_NAME = "dussehra_history_prefs";
    private static final String KEY_PROCESSED_MEDIA_IDS = "key_processed_media_ids";
    private static final String KEY_PROCESSED_FILE_SIGNATURES = "key_processed_file_signatures";
    private static final String KEY_LAST_RUN_SUMMARY = "key_last_run_summary";
    private static final String KEY_INCLUDE_VIDEOS = "key_include_videos";
    private static final String KEY_AUTO_ORGANIZE = "key_auto_organize";
    private static final String KEY_CUSTOM_CAMERA_PATH = "key_custom_camera_path";

    private final SharedPreferences prefs;

    public LocalHistoryRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isPhotoAlreadyProcessed(long mediaId, String fileName, long size) {
        Set<String> idSet = prefs.getStringSet(KEY_PROCESSED_MEDIA_IDS, new HashSet<>());
        if (idSet.contains(String.valueOf(mediaId))) {
            return true;
        }
        Set<String> sigSet = prefs.getStringSet(KEY_PROCESSED_FILE_SIGNATURES, new HashSet<>());
        String signature = fileName + "_" + size;
        return sigSet.contains(signature);
    }

    public synchronized void recordProcessedPhoto(long mediaId, String fileName, long size) {
        Set<String> idSet = new HashSet<>(prefs.getStringSet(KEY_PROCESSED_MEDIA_IDS, new HashSet<>()));
        idSet.add(String.valueOf(mediaId));

        Set<String> sigSet = new HashSet<>(prefs.getStringSet(KEY_PROCESSED_FILE_SIGNATURES, new HashSet<>()));
        sigSet.add(fileName + "_" + size);

        prefs.edit()
                .putStringSet(KEY_PROCESSED_MEDIA_IDS, idSet)
                .putStringSet(KEY_PROCESSED_FILE_SIGNATURES, sigSet)
                .apply();
    }

    public void setLastRunSummary(String summary) {
        prefs.edit().putString(KEY_LAST_RUN_SUMMARY, summary).apply();
    }

    public String getLastRunSummary() {
        return prefs.getString(KEY_LAST_RUN_SUMMARY, null);
    }

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
