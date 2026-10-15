package com.example.dussehraphotoorganizer.scanner;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.exifinterface.media.ExifInterface;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * PhotoMetadataReader strictly parses EXIF timestamps, GPS coordinates, and validates camera authenticity.
 * Excludes WhatsApp, Telegram, Screenshots, Facebook, Instagram, and web downloads.
 */
public class PhotoMetadataReader {

    private final Context context;
    private final SimpleDateFormat exifDateFormat;
    private final SimpleDateFormat displayDateFormat; // dd-MM-yyyy
    private final SimpleDateFormat isoDateFormat;     // yyyy-MM-dd

    public PhotoMetadataReader(Context context) {
        this.context = context.getApplicationContext();
        this.exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
        this.displayDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
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
        if (pathLower.contains("whatsapp") || (nameLower.startsWith("img-") && nameLower.contains("-wa"))) {
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
        if (pathLower.contains("dussehra_2026") || pathLower.contains("location_organized")) {
            // Already inside target organized folders
            return false;
        }

        // 2. Custom camera folder check (if user configured one)
        if (customCameraPath != null && !customCameraPath.trim().isEmpty()) {
            if (pathLower.contains(customCameraPath.trim().toLowerCase(Locale.US))) {
                return true;
            }
        }

        // 3. Samsung Galaxy A04e & Vivo Y18t standard camera directories
        if (pathLower.contains("dcim/camera") || pathLower.contains("dcim/100andro") || pathLower.equals("dcim/")) {
            return true;
        }

        // 4. File naming pattern fallback for native camera captures
        boolean matchesCameraNamePattern = nameLower.startsWith("img_20") ||
                nameLower.matches("^202[0-9]{5}_[0-9]{6}.*") ||
                nameLower.startsWith("sam_") ||
                nameLower.startsWith("vivo_") ||
                nameLower.startsWith("cam_");

        return matchesCameraNamePattern && pathLower.contains("dcim");
    }

    /**
     * Extracts capture date in "dd-MM-yyyy" format.
     * Backwards-compatible for festival schedule matcher.
     */
    public MetadataResult extractCaptureDate(Uri photoUri, long mediaStoreDateTaken) {
        PhotoFullMetadata full = extractFullMetadata(photoUri, mediaStoreDateTaken);
        return new MetadataResult(full.formattedDate, full.timestamp, full.source);
    }

    /**
     * Helper to parse date strings across common camera EXIF formatting variations
     */
    private Date parseExifDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        String trimmed = dateStr.trim();
        String[] patterns = new String[]{
                "yyyy:MM:dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy:MM:dd",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                Date d = sdf.parse(trimmed);
                if (d != null) return d;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Extracts comprehensive EXIF metadata including original capture date (both dd-MM-yyyy and yyyy-MM-dd)
     * and GPS coordinates without geographic redaction.
     */
    public PhotoFullMetadata extractFullMetadata(Uri photoUri, long mediaStoreDateTaken) {
        String formattedDate = null;
        String isoFormattedDate = null;
        long timestamp = 0;
        String source = "Unknown";
        Double latitude = null;
        Double longitude = null;

        if (photoUri != null) {
            Uri targetUri = photoUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                boolean hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_MEDIA_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
                if (hasLocationPermission) {
                    try {
                        targetUri = MediaStore.setRequireOriginal(photoUri);
                    } catch (Exception ignored) {
                        targetUri = photoUri;
                    }
                }
            }

            try (InputStream is = context.getContentResolver().openInputStream(targetUri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);

                    // 1. Parse Capture Date from multiple standard EXIF tags
                    String exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                    if (exifDateStr == null) {
                        exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
                    }
                    if (exifDateStr == null) {
                        exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME);
                    }
                    if (exifDateStr != null && !exifDateStr.trim().isEmpty()) {
                        Date parsed = parseExifDate(exifDateStr);
                        if (parsed != null) {
                            timestamp = parsed.getTime();
                            formattedDate = displayDateFormat.format(parsed);
                            isoFormattedDate = isoDateFormat.format(parsed);
                            source = "EXIF Original";
                        }
                    }

                    // 2. Parse GPS Coordinates
                    double[] coords = exif.getLatLong();
                    if (coords != null && coords.length >= 2) {
                        latitude = coords[0];
                        longitude = coords[1];
                    }
                }
            } catch (Exception ignored) {
                // If setRequireOriginal or EXIF reading fails, fall back to default URI reading
                if (photoUri != targetUri) {
                    try (InputStream fallbackIs = context.getContentResolver().openInputStream(photoUri)) {
                        if (fallbackIs != null) {
                            ExifInterface exif = new ExifInterface(fallbackIs);
                            String exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                            if (exifDateStr == null) {
                                exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
                            }
                            if (exifDateStr == null) {
                                exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME);
                            }
                            if (exifDateStr != null && !exifDateStr.trim().isEmpty()) {
                                Date parsed = parseExifDate(exifDateStr);
                                if (parsed != null) {
                                    timestamp = parsed.getTime();
                                    formattedDate = displayDateFormat.format(parsed);
                                    isoFormattedDate = isoDateFormat.format(parsed);
                                    source = "EXIF Original";
                                }
                            }
                            double[] fallbackCoords = exif.getLatLong();
                            if (fallbackCoords != null && fallbackCoords.length >= 2) {
                                latitude = fallbackCoords[0];
                                longitude = fallbackCoords[1];
                            }
                        }
                    } catch (Exception ignored2) {}
                }
            }
        }

        // MediaStore Date Taken fallback (creation time recorded by camera service)
        if (formattedDate == null && mediaStoreDateTaken > 0) {
            long ms = mediaStoreDateTaken;
            // Handle timestamps stored in seconds instead of milliseconds
            if (ms < 100000000000L) {
                ms *= 1000L;
            }
            timestamp = ms;
            Date d = new Date(ms);
            formattedDate = displayDateFormat.format(d);
            isoFormattedDate = isoDateFormat.format(d);
            source = "MediaStore Creation Time";
        }

        return new PhotoFullMetadata(formattedDate, isoFormattedDate, timestamp, source, latitude, longitude);
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

    public static class PhotoFullMetadata {
        public final String formattedDate;    // dd-MM-yyyy
        public final String isoFormattedDate; // yyyy-MM-dd (for Location_Organized folders)
        public final long timestamp;
        public final String source;
        public final Double latitude;
        public final Double longitude;

        public PhotoFullMetadata(String formattedDate, String isoFormattedDate, long timestamp, String source, Double latitude, Double longitude) {
            this.formattedDate = formattedDate;
            this.isoFormattedDate = isoFormattedDate;
            this.timestamp = timestamp;
            this.source = source;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public boolean hasValidCaptureDate() {
            return (formattedDate != null && !formattedDate.isEmpty()) || (isoFormattedDate != null && !isoFormattedDate.isEmpty());
        }

        public boolean hasGpsCoordinates() {
            return latitude != null && longitude != null;
        }
    }
}
