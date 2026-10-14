package com.example.dussehraphotoorganizer.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.dussehraphotoorganizer.organizer.PhotoOrganizer;
import com.example.dussehraphotoorganizer.scanner.PhotoScanner;

public class PhotoWorker extends Worker {

    public PhotoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            PhotoScanner scanner = new PhotoScanner(context);
            PhotoScanner.ScanResult scanResult = scanner.scanCameraPhotos(null);

            if (!scanResult.eligiblePhotos.isEmpty()) {
                PhotoOrganizer organizer = new PhotoOrganizer(context);
                organizer.copyPhotosToFestivalFolders(scanResult.eligiblePhotos, null);
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
