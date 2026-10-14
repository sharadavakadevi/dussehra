package com.example.dussehraphotoorganizer.scanner;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.exifinterface.media.ExifInterface;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * PhotoMetadataReader strictly parses EXIF timestamps and validates camera authenticity.
 * Excludes WhatsApp, Telegram, Screenshots, Facebook, Instagram, and web downloads.
 */
public class PhotoMetadataReader {

    private final Context context;
    private final SimpleDateFormat exifDateFormat;
    private final SimpleDateFormat displayDateFormat;

    public PhotoMetadataReader(Context context) {
        this.context = context.getApplicationContext();
        this.exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
        this.displayDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    }

    /**
     * Strict Camera Capture Validator:
     * Validates whether a file comes from Samsung Galaxy A04e, Vivo Y18t, or standard DCIM/Camera,
     * and strictly rejects WhatsApp, Telegram, Screenshots, and Downloads.
     */
    public boolean isAuthenticCameraPhoto(String relativePath, String displayName, String customCameraPath) {
        if (displayName == null) return false;
        String nameLower = displayName.toLowerCase(Locale.US);
        String pathLower = (relativePath != null) ? relativePath.toLowerCase(Locale.US) : "";

        // 1. Strict Exclusion of non-camera sources
        if (pathLower.contains("whatsapp") || nameLower.startsWith("img-") && nameLower.contains("-wa")) {
            return false;
        }
        if (pathLower.contains("telegram") || pathLower.contains("messenger") || pathLower.contains("viber") || pathLower.contains("signal")) {
            return false;
        }
        if (pathLower.contains("screenshot") || nameLower.contains("screenshot") || nameLower.startsWith("screencapture")) {
            return false;
        }
        if (pathLower.contains("download") || pathLower.contains("downloaded")) {
            return false;
        }
        if (pathLower.contains("facebook") || pathLower.contains("instagram") || pathLower.contains("twitter") || pathLower.contains("reddit")) {
            return false;
        }
        if (pathLower.contains("dussehra_2026")) {
            // Already inside target festival folder
            return false;
        }

        // 2. Custom camera folder check (if user configured one)
        if (customCameraPath != null && !customCameraPath.trim().isEmpty()) {
            if (pathLower.contains(customCameraPath.trim().toLowerCase(Locale.US))) {
                return true;
            }
        }

        // 3. Samsung Galaxy A04e & Vivo Y18t standard camera directories
        // Typical camera locations on Samsung & Vivo: DCIM/Camera, DCIM/100ANDRO, DCIM/Camera/
        if (pathLower.contains("dcim/camera") || pathLower.contains("dcim/100andro") || pathLower.equals("dcim/")) {
            return true;
        }

        // 4. File naming pattern fallback for native camera captures
        // E.g., IMG_20261013_..., 20261013_..., PANO_..., SAM_...
        boolean matchesCameraNamePattern = nameLower.startsWith("img_20") ||
                nameLower.matches("^202[0-9]{5}_[0-9]{6}.*") ||
                nameLower.startsWith("sam_") ||
                nameLower.startsWith("vivo_") ||
                nameLower.startsWith("cam_");

        return matchesCameraNamePattern && pathLower.contains("dcim");
    }

    /**
     * Extracts capture date in "dd-MM-yyyy" format.
     * Prioritizes EXIF TAG_DATETIME_ORIGINAL and TAG_DATETIME_DIGITIZED.
     * Fallbacks to MediaStore dateTaken (media creation timestamp).
     * Never pretends modified date is capture date.
     */
    public MetadataResult extractCaptureDate(Uri photoUri, long mediaStoreDateTaken) {
        String formattedDate = null;
        long timestamp = 0;
        String source = "Unknown";

        if (photoUri != null) {
            try (InputStream is = context.getContentResolver().openInputStream(photoUri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);
                    String exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                    if (exifDateStr == null) {
                        exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
                    }
                    if (exifDateStr != null && !exifDateStr.trim().isEmpty()) {
                        Date parsed = exifDateFormat.parse(exifDateStr.trim());
                        if (parsed != null) {
                            timestamp = parsed.getTime();
                            formattedDate = displayDateFormat.format(parsed);
                            source = "EXIF Original";
                        }
                    }
                }
            } catch (Exception ignored) {
                // EXIF reading failed, will use fallback
            }
        }

        // MediaStore Date Taken fallback (creation time recorded by camera service)
        if (formattedDate == null && mediaStoreDateTaken > 0) {
            timestamp = mediaStoreDateTaken;
            formattedDate = displayDateFormat.format(new Date(mediaStoreDateTaken));
            source = "MediaStore Creation Time";
        }

        return new MetadataResult(formattedDate, timestamp, source);
    }

    public static class MetadataResult {
        public final String formattedDate; // dd-MM-yyyy
        public final long timestamp;
        public final String source;

        public MetadataResult(String formattedDate, long timestamp, String source) {
            this.formattedDate = formattedDate;
            this.timestamp = timestamp;
            this.source = source;
        }

        public boolean isValid() {
            return formattedDate != null && !formattedDate.isEmpty();
        }
    }
}
