package com.example.dussehraphotoorganizer.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.example.dussehraphotoorganizer.model.LocationConfigItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages registered physical locations with exact addresses, target folder names,
 * and offline GPS coordinates & radius for location-based photo organization.
 * 
 * Works 100% offline using Android's native Location.distanceBetween without geocoding or internet access.
 */
public class LocationConfigManager {

    private static final String PREF_NAME = "dussehra_location_config_prefs";
    private static final String KEY_LOCATIONS_JSON = "key_registered_locations_json";

    public static final String ROOT_FOLDER_NAME = "Location_Organized";
    public static final String FOLDER_HOME = "Home";
    public static final String FOLDER_TEMPLE = "Yeendda_Durga_Devi_Temple";
    public static final String FOLDER_CARSHED = "Carshed_Bobleam";

    private final SharedPreferences prefs;
    private final Gson gson;

    public LocationConfigManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Default registered locations extracted from requirement specifications.
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
                17.7772,
                83.3616,
                250.0f
        ));

        // Location 2 — Yeendda Durga Devi Temple
        list.add(new LocationConfigItem(
                "loc_temple",
                "Yeendda Durga Devi Temple",
                "ఎండడ దుర్గాదేవి ఆలయం",
                "Sri Lakshmi Villa, 8, Endada Rushikonda Rd, Sandipini Nagar, Mubarak Colony, Yendada, Endada, Andhra Pradesh 530045, India",
                FOLDER_TEMPLE,
                17.7832,
                83.3603,
                250.0f
        ));

        // Location 3 — Carshed Bobleam
        list.add(new LocationConfigItem(
                "loc_carshed",
                "Carshed Bobleam",
                "కార్ షెడ్ బొబ్బిలి / పి.ఎం పాలెం",
                "NH 16 Service Rd, Anand Nagar, Pothinamallayya Palem, Pothinamallayyapalem, Andhra Pradesh 530041, India",
                FOLDER_CARSHED,
                17.8028,
                83.3531,
                300.0f
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
     * Offline matching of photo EXIF coordinates against registered locations.
     * Computes distance in meters and returns the closest matching location within its defined radius.
     */
    public MatchResult matchLocation(double photoLatitude, double photoLongitude) {
        List<LocationConfigItem> locations = getRegisteredLocations();
        LocationConfigItem closestMatch = null;
        float minDistance = Float.MAX_VALUE;

        float[] results = new float[1];
        for (LocationConfigItem loc : locations) {
            if (!loc.isEnabled()) continue;

            Location.distanceBetween(photoLatitude, photoLongitude, loc.getLatitude(), loc.getLongitude(), results);
            float distanceMeters = results[0];

            if (distanceMeters <= loc.getRadiusMeters()) {
                if (distanceMeters < minDistance) {
                    minDistance = distanceMeters;
                    closestMatch = loc;
                }
            }
        }

        if (closestMatch != null) {
            return new MatchResult(true, closestMatch, minDistance);
        } else {
            return new MatchResult(false, null, -1);
        }
    }

    public static class MatchResult {
        public final boolean isMatched;
        public final LocationConfigItem matchedLocation;
        public final float distanceMeters;

        public MatchResult(boolean isMatched, LocationConfigItem matchedLocation, float distanceMeters) {
            this.isMatched = isMatched;
            this.matchedLocation = matchedLocation;
            this.distanceMeters = distanceMeters;
        }
    }
}
