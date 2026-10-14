package com.example.dussehraphotoorganizer.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.dussehraphotoorganizer.data.LocalHistoryRepository;
import com.example.dussehraphotoorganizer.organizer.LocationPhotoOrganizer;
import com.example.dussehraphotoorganizer.organizer.PhotoOrganizer;
import com.example.dussehraphotoorganizer.scanner.LocationPhotoScanner;
import com.example.dussehraphotoorganizer.scanner.PhotoScanner;

/**
 * Background worker that respects Android background execution limits.
 * Automatically organizes new eligible camera photos into Location folders
 * and Festival folders without requiring repetitive manual user confirmation.
 */
public class PhotoWorker extends Worker {

    public PhotoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            LocalHistoryRepository history = new LocalHistoryRepository(context);

            // 1. Automatic Location Organization
            if (history.isAutoLocationOrganizeEnabled()) {
                LocationPhotoScanner locationScanner = new LocationPhotoScanner(context);
                LocationPhotoScanner.LocationScanResult locationResult = locationScanner.scanCameraPhotos(null);

                if (!locationResult.eligiblePhotos.isEmpty()) {
                    LocationPhotoOrganizer locationOrganizer = new LocationPhotoOrganizer(context);
                    locationOrganizer.copyPhotosToLocationFolders(locationResult.eligiblePhotos, null);
                }
            }

            // 2. Automatic Festival Organization (if enabled)
            if (history.isAutoOrganizeEnabled()) {
                PhotoScanner festivalScanner = new PhotoScanner(context);
                PhotoScanner.ScanResult festivalResult = festivalScanner.scanCameraPhotos(null);

                if (!festivalResult.eligiblePhotos.isEmpty()) {
                    PhotoOrganizer festivalOrganizer = new PhotoOrganizer(context);
                    festivalOrganizer.copyPhotosToFestivalFolders(festivalResult.eligiblePhotos, null);
                }
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
