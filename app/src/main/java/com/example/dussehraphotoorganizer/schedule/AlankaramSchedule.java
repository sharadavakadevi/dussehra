package com.example.dussehraphotoorganizer.schedule;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AlankaramSchedule manages the official 2026 Dussehra Navaratri schedule
 * extracted directly from the Visakhapatnam temple poster.
 *
 * Folder names STRICTLY DO NOT include dates.
 * Persists in SharedPreferences so the user can edit dates/names in-app anytime.
 */
public class AlankaramSchedule {

    private static final String PREF_NAME = "dussehra_schedule_prefs";
    private static final String KEY_SCHEDULE_JSON = "key_schedule_json";
    public static final String MAIN_FOLDER_NAME = "Dussehra_2026";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    public AlankaramSchedule(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Returns the master list of 11 Dussehra 2026 festival dates and alankarams.
     * Note: Folder names DO NOT contain date prefixes.
     */
    public List<FestivalDateItem> getDefaultSchedule() {
        List<FestivalDateItem> list = new ArrayList<>();
        list.add(new FestivalDateItem(
                "11-10-2026",
                "ఆదివారం శుద్ధ పాడ్యమి",
                "శ్రీ బాలాత్రిపురసుందరి దేవి",
                "Sri Bala Tripura Sundari Devi",
                "Sri_Bala_Tripura_Sundari_Devi",
                "కలశస్థాపన ఉ॥ గం॥ 8.09 ని॥లకు మరియు సహస్ర కుంకుమార్చన"
        ));
        list.add(new FestivalDateItem(
                "12-10-2026",
                "సోమవారం శుద్ధ విదియ",
                "శ్రీ గాయత్రి దేవి",
                "Sri Gayatri Devi",
                "Sri_Gayatri_Devi",
                "సహస్ర పుష్పార్చనలు"
        ));
        list.add(new FestivalDateItem(
                "13-10-2026",
                "మంగళవారం శుద్ధ తదియ",
                "శ్రీ అన్నపూర్ణ దేవి",
                "Sri Annapurna Devi",
                "Sri_Annapurna_Devi",
                "గాజుల పూజ"
        ));
        list.add(new FestivalDateItem(
                "14-10-2026",
                "బుధవారం శుద్ధ చవితి",
                "శ్రీ కాత్యాయని దేవి",
                "Sri Kathyayani Devi",
                "Sri_Kathyayani_Devi",
                "బిల్వదలార్చనలు"
        ));
        list.add(new FestivalDateItem(
                "15-10-2026",
                "గురువారం శుద్ధ పంచమి",
                "శ్రీ లలిత త్రిపురసుందరి దేవి",
                "Sri Lalitha Tripura Sundari Devi",
                "Sri_Lalitha_Tripura_Sundari_Devi",
                "రుద్రాభిషేకం"
        ));
        list.add(new FestivalDateItem(
                "16-10-2026",
                "శుక్రవారం శుద్ధ షష్ఠి",
                "శ్రీ సరస్వతి దేవి",
                "Sri Saraswathi Devi",
                "Sri_Saraswathi_Devi",
                "మహా కుంభాభిషేకము"
        ));
        list.add(new FestivalDateItem(
                "17-10-2026",
                "శనివారం శుద్ధ సప్తమి",
                "శ్రీ మహాలక్ష్మి దేవి",
                "Sri Mahalakshmi Devi",
                "Sri_Mahalakshmi_Devi",
                "పసుపుకొమ్ముల పూజ"
        ));
        list.add(new FestivalDateItem(
                "18-10-2026",
                "ఆదివారం శుద్ధ అష్టమి",
                "శ్రీ దుర్గాదేవి",
                "Sri Durga Devi",
                "Sri_Durga_Devi",
                "లక్ష కుంకుమార్చన, దీపారాధన"
        ));
        list.add(new FestivalDateItem(
                "19-10-2026",
                "సోమవారం శుద్ధ నవమి",
                "శ్రీ మహిషాసురమర్దిని దేవి",
                "Sri Mahishasuramardini Devi",
                "Sri_Mahishasuramardini_Devi",
                "లక్ష తులసి పూజ, సుహాసినీ పూజ"
        ));
        list.add(new FestivalDateItem(
                "20-10-2026",
                "మంగళవారం శుద్ధ దశమి",
                "శ్రీ రాజరాజేశ్వరీ దేవి (విజయదశమి)",
                "Sri Rajarajeshwari Devi",
                "Sri_Rajarajeshwari_Devi",
                "విజయదశమి (వాహన పూజలు)"
        ));
        list.add(new FestivalDateItem(
                "21-10-2026",
                "బుధవారం శుద్ధ ఏకాదశి",
                "నిత్య అలంకరణ",
                "Nitya Alankaranam",
                "Nitya_Alankaranam",
                "సహస్రధారా శాంతి అభిషేకములు"
        ));
        return list;
    }

    /**
     * Loads current schedule from storage or returns default if not modified.
     */
    public List<FestivalDateItem> getSchedule() {
        String json = prefs.getString(KEY_SCHEDULE_JSON, null);
        if (json == null || json.trim().isEmpty()) {
            List<FestivalDateItem> defaults = getDefaultSchedule();
            saveSchedule(defaults);
            return defaults;
        }
        try {
            Type type = new TypeToken<List<FestivalDateItem>>() {}.getType();
            List<FestivalDateItem> list = gson.fromJson(json, type);
            if (list == null || list.isEmpty()) {
                return getDefaultSchedule();
            }
            return list;
        } catch (Exception e) {
            return getDefaultSchedule();
        }
    }

    /**
     * Saves updated schedule to local storage.
     */
    public void saveSchedule(List<FestivalDateItem> schedule) {
        String json = gson.toJson(schedule);
        prefs.edit().putString(KEY_SCHEDULE_JSON, json).apply();
    }

    /**
     * Finds festival date item for a given date formatted as "dd-MM-yyyy".
     */
    public FestivalDateItem getFestivalDateItem(String dateFormatted) {
        if (dateFormatted == null) return null;
        for (FestivalDateItem item : getSchedule()) {
            if (item.getDate().equalsIgnoreCase(dateFormatted.trim())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Checks if today's device date matches a festival date.
     */
    public FestivalDateItem getTodayFestivalItem() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        String today = sdf.format(new Date());
        return getFestivalDateItem(today);
    }
}
