package com.example.dussehraphotoorganizer.sync.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent synchronization ledger and device pairing storage.
 * Records unique content hashes (SHA-256) of every synchronized photo to guarantee:
 * 1. Zero duplicate photo transfers.
 * 2. Unambiguous photo identification even if filenames match.
 * 3. Bidirectional tracking of both outgoing and incoming photos.
 */
public class SyncLedgerRepository {

    private static final String PREF_NAME = "dussehra_sync_ledger_prefs";

    // Own Device Keys
    private static final String KEY_DEVICE_ID = "key_own_device_id";
    private static final String KEY_DEVICE_NAME = "key_own_device_name";
    private static final String KEY_PAIRING_PIN = "key_own_pairing_pin";

    // Paired Device Keys
    private static final String KEY_IS_PAIRED = "key_is_paired";
    private static final String KEY_PAIRED_DEVICE_ID = "key_paired_device_id";
    private static final String KEY_PAIRED_DEVICE_NAME = "key_paired_device_name";
    private static final String KEY_SHARED_SECRET = "key_shared_secret";
    private static final String KEY_LAST_KNOWN_IP = "key_last_known_ip";
    private static final String KEY_LAST_SYNC_TIMESTAMP = "key_last_sync_timestamp";

    // Content Hash Sets
    private static final String KEY_SYNCED_OUTGOING_HASHES = "key_synced_outgoing_hashes";
    private static final String KEY_RECEIVED_INCOMING_HASHES = "key_received_incoming_hashes";

    // Activity Log & Preferences
    private static final String KEY_SYNC_LOGS_JSON = "key_sync_logs_json";
    private static final String KEY_AUTO_WIFI_SYNC_ENABLED = "key_auto_wifi_sync_enabled";

    private final SharedPreferences prefs;
    private final Gson gson;

    public static class SyncLogItem {
        public final long timestamp;
        public final String fileName;
        public final String direction; // "SENT" or "RECEIVED"
        public final String status;    // "SUCCESS" or "FAILED"
        public final String details;

        public SyncLogItem(long timestamp, String fileName, String direction, String status, String details) {
            this.timestamp = timestamp;
            this.fileName = fileName;
            this.direction = direction;
            this.status = status;
            this.details = details;
        }
    }

    public SyncLedgerRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        initializeOwnDeviceIdentity();
    }

    private void initializeOwnDeviceIdentity() {
        if (!prefs.contains(KEY_DEVICE_ID)) {
            String deviceId = UUID.randomUUID().toString();
            String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
            String pin = generateRandomPin();

            prefs.edit()
                    .putString(KEY_DEVICE_ID, deviceId)
                    .putString(KEY_DEVICE_NAME, deviceName)
                    .putString(KEY_PAIRING_PIN, pin)
                    .apply();
        }
    }

    public static String generateRandomPin() {
        SecureRandom random = new SecureRandom();
        int pinInt = 100000 + random.nextInt(900000);
        return String.valueOf(pinInt);
    }

    public String getOwnDeviceId() {
        return prefs.getString(KEY_DEVICE_ID, "");
    }

    public String getOwnDeviceName() {
        return prefs.getString(KEY_DEVICE_NAME, Build.MODEL);
    }

    public void setOwnDeviceName(String name) {
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply();
    }

    public String getOwnPairingPin() {
        return prefs.getString(KEY_PAIRING_PIN, "123456");
    }

    public String regenerateOwnPairingPin() {
        String newPin = generateRandomPin();
        prefs.edit().putString(KEY_PAIRING_PIN, newPin).apply();
        return newPin;
    }

    // ==========================================
    // Pairing Management
    // ==========================================

    public boolean isPaired() {
        return prefs.getBoolean(KEY_IS_PAIRED, false) && !getPairedDeviceId().isEmpty();
    }

    public String getPairedDeviceId() {
        return prefs.getString(KEY_PAIRED_DEVICE_ID, "");
    }

    public String getPairedDeviceName() {
        return prefs.getString(KEY_PAIRED_DEVICE_NAME, "Paired Device");
    }

    public String getSharedSecret() {
        return prefs.getString(KEY_SHARED_SECRET, "");
    }

    public String getLastKnownIp() {
        return prefs.getString(KEY_LAST_KNOWN_IP, "");
    }

    public void setLastKnownIp(String ip) {
        prefs.edit().putString(KEY_LAST_KNOWN_IP, ip).apply();
    }

    public long getLastSyncTimestamp() {
        return prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0);
    }

    public void setLastSyncTimestamp(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply();
    }

    public synchronized void savePairing(String pairedDeviceId, String pairedDeviceName, String sharedSecret, String ip) {
        prefs.edit()
                .putBoolean(KEY_IS_PAIRED, true)
                .putString(KEY_PAIRED_DEVICE_ID, pairedDeviceId)
                .putString(KEY_PAIRED_DEVICE_NAME, pairedDeviceName)
                .putString(KEY_SHARED_SECRET, sharedSecret)
                .putString(KEY_LAST_KNOWN_IP, ip != null ? ip : "")
                .apply();
    }

    public synchronized void unpair() {
        prefs.edit()
                .remove(KEY_IS_PAIRED)
                .remove(KEY_PAIRED_DEVICE_ID)
                .remove(KEY_PAIRED_DEVICE_NAME)
                .remove(KEY_SHARED_SECRET)
                .remove(KEY_LAST_KNOWN_IP)
                .apply();
    }

    // ==========================================
    // Hash Ledger & Deduplication
    // ==========================================

    public synchronized boolean isHashAlreadySyncedOrReceived(String sha256Hash) {
        if (sha256Hash == null || sha256Hash.isEmpty()) return false;
        Set<String> outgoing = prefs.getStringSet(KEY_SYNCED_OUTGOING_HASHES, Collections.emptySet());
        if (outgoing.contains(sha256Hash)) return true;
        Set<String> incoming = prefs.getStringSet(KEY_RECEIVED_INCOMING_HASHES, Collections.emptySet());
        return incoming.contains(sha256Hash);
    }

    public synchronized void recordOutgoingPhotoSynced(String sha256Hash) {
        if (sha256Hash == null || sha256Hash.isEmpty()) return;
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_SYNCED_OUTGOING_HASHES, Collections.emptySet()));
        set.add(sha256Hash);
        prefs.edit().putStringSet(KEY_SYNCED_OUTGOING_HASHES, set).apply();
    }

    public synchronized void recordIncomingPhotoReceived(String sha256Hash) {
        if (sha256Hash == null || sha256Hash.isEmpty()) return;
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_RECEIVED_INCOMING_HASHES, Collections.emptySet()));
        set.add(sha256Hash);
        prefs.edit().putStringSet(KEY_RECEIVED_INCOMING_HASHES, set).apply();
    }

    public int getOutgoingSyncedCount() {
        Set<String> set = prefs.getStringSet(KEY_SYNCED_OUTGOING_HASHES, Collections.emptySet());
        return set.size();
    }

    public int getIncomingReceivedCount() {
        Set<String> set = prefs.getStringSet(KEY_RECEIVED_INCOMING_HASHES, Collections.emptySet());
        return set.size();
    }

    // ==========================================
    // Activity History Logs
    // ==========================================

    public synchronized void addLogEntry(String fileName, String direction, String status, String details) {
        List<SyncLogItem> logs = getSyncLogs();
        logs.add(0, new SyncLogItem(System.currentTimeMillis(), fileName, direction, status, details));
        // Keep last 100 entries to prevent memory bloating
        if (logs.size() > 100) {
            logs = new ArrayList<>(logs.subList(0, 100));
        }
        String json = gson.toJson(logs);
        prefs.edit().putString(KEY_SYNC_LOGS_JSON, json).apply();
    }

    public List<SyncLogItem> getSyncLogs() {
        String json = prefs.getString(KEY_SYNC_LOGS_JSON, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<SyncLogItem>>() {}.getType();
            List<SyncLogItem> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ==========================================
    // Sync Preferences
    // ==========================================

    public boolean isAutoWifiSyncEnabled() {
        return prefs.getBoolean(KEY_AUTO_WIFI_SYNC_ENABLED, true);
    }

    public void setAutoWifiSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_WIFI_SYNC_ENABLED, enabled).apply();
    }
}
