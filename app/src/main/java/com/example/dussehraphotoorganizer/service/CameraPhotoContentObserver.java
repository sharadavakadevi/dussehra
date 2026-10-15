package com.example.dussehraphotoorganizer.service;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.sync.service.SyncTriggerManager;

/**
 * CameraPhotoContentObserver monitors MediaStore for new camera captures in real-time.
 * When a new camera photo is detected, it delegates execution to WorkManager,
 * ensuring strict battery and background constraint compliance.
 */
public class CameraPhotoContentObserver extends ContentObserver {

    private final Context context;
    private final LocalHistoryRepository historyRepository;
    private long lastTriggerTime = 0;

    public CameraPhotoContentObserver(Context context) {
        super(new Handler(Looper.getMainLooper()));
        this.context = context.getApplicationContext();
        this.historyRepository = new LocalHistoryRepository(context);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

        // 1. Prevent infinite loops: If an organization copy operation is actively writing, ignore
        if (com.example.dussehraphotoorganizer.organizer.LocationPhotoOrganizer.isOrganizing ||
            com.example.dussehraphotoorganizer.organizer.PhotoOrganizer.isOrganizing) {
            return;
        }

        // 2. Debounce rapid camera shutter bursts (within 3 seconds)
        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < 3000) {
            return;
        }
        lastTriggerTime = now;

        // 1. Automatic local photo organization (Location & Festival)
        if (historyRepository.isAutoLocationOrganizeEnabled() || historyRepository.isAutoOrganizeEnabled()) {
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(PhotoWorker.class)
                    .addTag("camera_observer_work")
                    .build();

            WorkManager.getInstance(context).enqueueUniqueWork(
                    "work_camera_capture_event",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
            );
        }

        // 2. Automatic Two-Way Wi-Fi Synchronization between paired phones
        SyncTriggerManager.triggerSync(context);
    }
}
