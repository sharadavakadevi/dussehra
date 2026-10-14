package com.example.dussehraphotoorganizer.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.model.LocationConfigItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages registered physical locations with user-verified GPS coordinates and radii.
 * 
 * Verified-by-user Coordinates:
 * - Location 1 — Home: 17.782889° N, 83.361833° E (Radius: 40m) -> Home
 * - Location 2 — Sri Lakshmi Villa / Yeendda Durga Devi Temple: 17.781944° N, 83.361917° E (Radius: 40m) -> Yeendda_Durga_Devi_Temple
 * - Location 3 — Carshed Bobleam: 17.802778° N, 83.352667° E (Radius: 40m) -> Carshed_Bobleam
 * 
 * Works 100% offline using Android's native Location.distanceBetween without internet access.
 */
public class LocationConfigManager {

    private static final String PREF_NAME = "dussehra_location_config_prefs";
    private static final String KEY_LOCATIONS_JSON = "key_registered_locations_json_v2";

    public static final String FOLDER_HOME = "Home";
    public static final String FOLDER_TEMPLE = "Yeendda_Durga_Devi_Temple";
    public static final String FOLDER_CARSHED = "Carshed_Bobleam";
    public static final String FOLDER_OTHER_DATES = "Other_Dates";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final AlankaramSchedule alankaramSchedule;

    public LocationConfigManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.alankaramSchedule = new AlankaramSchedule(this.context);
    }

    /**
     * Exact user-provided Google Maps coordinates with non-overlapping 40m radii.
     * Note: Home & Temple are ~106m apart. 40m each leaves a ~26m buffer zone.
     */
    public List<LocationConfigItem> getDefaultLocations() {
        List<LocationConfigItem> list = new ArrayList<>();

        // Location 1 — Home
        list.add(new LocationConfigItem(
                "loc_home",
                "Home",
                "ఇల్లు (Home)",
                "1-72, Yendada, Endada, Andhra Pradesh 530045, India",
                FOLDER_HOME,
                17.782889,
                83.361833,
                40.0f
        ));

        // Location 2 — Sri Lakshmi Villa / Yeendda Durga Devi Temple
        list.add(new LocationConfigItem(
                "loc_temple",
                "Sri Lakshmi Villa / Yeendda Durga Devi Temple",
                "ఎండడ దుర్గాదేవి ఆలయం",
                "Sri Lakshmi Villa, 8, Endada Rushikonda Rd, Sandipini Nagar, Mubarak Colony, Yendada, Endada, Andhra Pradesh 530045, India",
                FOLDER_TEMPLE,
                17.781944,
                83.361917,
                40.0f
        ));

        // Location 3 — Carshed Bobleam
        list.add(new LocationConfigItem(
                "loc_carshed",
                "Carshed Bobleam",
                "కార్ షెడ్ బొబ్బిలి / పి.ఎం పాలెం",
                "NH 16 Service Rd, Anand Nagar, Pothinamallayya Palem, Pothinamallayyapalem, Andhra Pradesh 530041, India",
                FOLDER_CARSHED,
                17.802778,
                83.352667,
                40.0f
        ));

        return list;
    }

    public List<LocationConfigItem> getRegisteredLocations() {
        String json = prefs.getString(KEY_LOCATIONS_JSON, null);
        if (json == null || json.trim().isEmpty()) {
            List<LocationConfigItem> defaults = getDefaultLocations();
            saveLocations(defaults);
            return defaults;
        }
        try {
            Type type = new TypeToken<List<LocationConfigItem>>() {}.getType();
            List<LocationConfigItem> list = gson.fromJson(json, type);
            if (list == null || list.isEmpty()) {
                return getDefaultLocations();
            }
            return list;
        } catch (Exception e) {
            return getDefaultLocations();
        }
    }

    public void saveLocations(List<LocationConfigItem> locations) {
        String json = gson.toJson(locations);
        prefs.edit().putString(KEY_LOCATIONS_JSON, json).apply();
    }

    public void resetToDefaults() {
        saveLocations(getDefaultLocations());
    }

    /**
     * Evaluates full destination path and location matching for a photo.
     * Computes distance to every registered location.
     * If 0 match -> date fallback (YYYY-MM-DD).
     * If 1 match -> matching location folder (Home / Temple / Carshed).
     * If >1 match -> ambiguous overlap -> date fallback (YYYY-MM-DD) instead of guessing.
     */
    public EvaluationResult evaluatePhotoDestination(
            Double photoLatitude,
            Double photoLongitude,
            String captureDateFormatted, // dd-MM-yyyy
            String isoDate // yyyy-MM-dd
    ) {
        List<LocationConfigItem> locations = getRegisteredLocations();
        Map<String, Float> distances = new HashMap<>();
        List<LocationConfigItem> matchedLocations = new ArrayList<>();

        float[] results = new float[1];
        boolean hasGps = (photoLatitude != null && photoLongitude != null);

        if (hasGps) {
            for (LocationConfigItem loc : locations) {
                if (!loc.isEnabled()) continue;
                Location.distanceBetween(photoLatitude, photoLongitude, loc.getLatitude(), loc.getLongitude(), results);
                float dist = results[0];
                distances.put(loc.getFolderName(), dist);

                if (dist <= loc.getRadiusMeters()) {
                    matchedLocations.add(loc);
                }
            }
        }

        // Determine Devi parent folder under Pictures/Dussehra_2026/
        String deviParentFolder;
        FestivalDateItem matchedFestival = null;
        if (captureDateFormatted != null && !captureDateFormatted.isEmpty()) {
            matchedFestival = alankaramSchedule.getFestivalDateItem(captureDateFormatted);
        }

        if (matchedFestival != null) {
            deviParentFolder = matchedFestival.getFolderName();
        } else {
            deviParentFolder = FOLDER_OTHER_DATES;
        }

        // Determine location subfolder
        String locationSubfolder;
        String selectedAlias;
        String statusSummary;
        String matchType;

        if (!hasGps) {
            selectedAlias = null;
            locationSubfolder = (isoDate != null && !isoDate.isEmpty()) ? isoDate : "Unknown_Date";
            matchType = "NO_GPS";
            statusSummary = String.format("📅 No GPS -> Fallback: %s/%s/", deviParentFolder, locationSubfolder);
        } else if (matchedLocations.size() == 1) {
            LocationConfigItem single = matchedLocations.get(0);
            selectedAlias = single.getFolderName();
            locationSubfolder = single.getFolderName();
            matchType = "SINGLE_MATCH";
            float dist = distances.get(single.getFolderName());
            statusSummary = String.format(Locale.US, "📍 %s (%.1fm <= %.0fm) -> %s/%s/",
                    single.getName(), dist, single.getRadiusMeters(), deviParentFolder, locationSubfolder);
        } else if (matchedLocations.size() > 1) {
            // Ambiguous match (overlapping radii) -> Use date fallback rather than guessing
            selectedAlias = null;
            locationSubfolder = (isoDate != null && !isoDate.isEmpty()) ? isoDate : "Unknown_Date";
            matchType = "AMBIGUOUS_MULTIPLE_MATCH";
            statusSummary = String.format("⚠️ Multiple Locations Matched (%d) -> Fallback: %s/%s/",
                    matchedLocations.size(), deviParentFolder, locationSubfolder);
        } else {
            // No location matched
            selectedAlias = null;
            locationSubfolder = (isoDate != null && !isoDate.isEmpty()) ? isoDate : "Unknown_Date";
            matchType = "UNMATCHED_GPS";
            statusSummary = String.format("📅 Unmatched GPS -> Fallback: %s/%s/", deviParentFolder, locationSubfolder);
        }

        String destinationRelativePath = AlankaramSchedule.MAIN_FOLDER_NAME + "/" + deviParentFolder + "/" + locationSubfolder;

        return new EvaluationResult(
                photoLatitude,
                photoLongitude,
                distances,
                matchedLocations,
                matchType,
                selectedAlias,
                deviParentFolder,
                locationSubfolder,
                destinationRelativePath,
                statusSummary
        );
    }

    public static class EvaluationResult {
        public final Double photoLatitude;
        public final Double photoLongitude;
        public final Map<String, Float> distancesToLocations; // e.g. "Home" -> 12.5f
        public final List<LocationConfigItem> matchedLocations;
        public final String matchType; // SINGLE_MATCH, UNMATCHED_GPS, AMBIGUOUS_MULTIPLE_MATCH, NO_GPS
        public final String selectedAlias; // "Home", "Yeendda_Durga_Devi_Temple", "Carshed_Bobleam", or null
        public final String deviParentFolder; // e.g. "Sri_Gayatri_Devi" or "Other_Dates"
        public final String locationSubfolder; // e.g. "Home" or "2026-10-12"
        public final String destinationRelativePath; // e.g. "Dussehra_2026/Sri_Gayatri_Devi/Home"
        public final String statusSummary;

        public EvaluationResult(
                Double photoLatitude,
                Double photoLongitude,
                Map<String, Float> distancesToLocations,
                List<LocationConfigItem> matchedLocations,
                String matchType,
                String selectedAlias,
                String deviParentFolder,
                String locationSubfolder,
                String destinationRelativePath,
                String statusSummary
        ) {
            this.photoLatitude = photoLatitude;
            this.photoLongitude = photoLongitude;
            this.distancesToLocations = distancesToLocations;
            this.matchedLocations = matchedLocations;
            this.matchType = matchType;
            this.selectedAlias = selectedAlias;
            this.deviParentFolder = deviParentFolder;
            this.locationSubfolder = locationSubfolder;
            this.destinationRelativePath = destinationRelativePath;
            this.statusSummary = statusSummary;
        }

        public String getDetailedLog() {
            StringBuilder sb = new StringBuilder();
            sb.append("EXIF GPS: ").append(photoLatitude != null && photoLongitude != null
                    ? String.format(Locale.US, "%.6f, %.6f", photoLatitude, photoLongitude) : "None").append("\n");

            if (!distancesToLocations.isEmpty()) {
                sb.append("Distances:\n");
                for (Map.Entry<String, Float> e : distancesToLocations.entrySet()) {
                    sb.append(String.format(Locale.US, " • %s: %.1f meters\n", e.getKey(), e.getValue()));
                }
            }

            sb.append("Matched Alias: ").append(selectedAlias != null ? selectedAlias : "(None - Fallback)").append("\n");
            sb.append("Devi Folder: ").append(deviParentFolder).append("\n");
            sb.append("Subfolder: ").append(locationSubfolder).append("\n");
            sb.append("Full Path: Pictures/").append(destinationRelativePath).append("/");
            return sb.toString();
        }
    }
}
