package com.example.dussehraphotoorganizer.sync.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.engine.SyncEngine;

/**
 * Trigger manager invoked automatically when:
 * 1. CameraPhotoContentObserver detects a new camera capture.
 * 2. PhotoWorker runs in the background.
 * 3. App is launched or Wi-Fi connectivity returns.
 */
public class SyncTriggerManager {

    private static final String TAG = "SyncTriggerManager";

    public static void startSyncService(Context context) {
        try {
            Context appCtx = context.getApplicationContext();
            SyncLedgerRepository repo = new SyncLedgerRepository(appCtx);
            if (!repo.isAutoWifiSyncEnabled()) {
                return;
            }

            Intent intent = new Intent(appCtx, SyncForegroundService.class);
            intent.setAction(SyncForegroundService.ACTION_START_SYNC_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appCtx, intent);
            } else {
                appCtx.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting SyncForegroundService: " + e.getMessage());
        }
    }

    public static void triggerSync(Context context) {
        try {
            Context appCtx = context.getApplicationContext();
            SyncLedgerRepository repo = new SyncLedgerRepository(appCtx);
            if (!repo.isAutoWifiSyncEnabled() || !repo.isPaired()) {
                return;
            }

            // Ensure service is running
            startSyncService(appCtx);

            // Trigger immediate sync evaluation in SyncEngine
            SyncEngine.getInstance(appCtx).triggerSync();
        } catch (Exception e) {
            Log.e(TAG, "Error triggering sync: " + e.getMessage());
        }
    }
}
