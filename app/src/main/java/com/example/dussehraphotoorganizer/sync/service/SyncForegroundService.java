package com.example.dussehraphotoorganizer.sync.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.engine.SyncEngine;
import com.example.dussehraphotoorganizer.ui.PairDevicesActivity;

/**
 * Low-power Foreground Service maintaining local Wi-Fi synchronization.
 * Uses ConnectivityManager.NetworkCallback to automatically activate on Wi-Fi
 * and suspend on cellular/airplane mode to preserve battery.
 */
public class SyncForegroundService extends Service {

    private static final String TAG = "SyncService";
    private static final String CHANNEL_ID = "channel_dussehra_sync";
    private static final int NOTIFICATION_ID = 2026;

    public static final String ACTION_START_SYNC_SERVICE = "action_start_sync_service";
    public static final String ACTION_TRIGGER_SYNC = "action_trigger_sync";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SyncEngine syncEngine;
    private SyncLedgerRepository ledgerRepository;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        ledgerRepository = new SyncLedgerRepository(this);
        syncEngine = SyncEngine.getInstance(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        createNotificationChannel();
        startInForeground("Dussehra Wi-Fi Sync Active", "Listening for paired phone on Wi-Fi");

        setupSyncEngineListener();
        registerWifiNetworkCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TRIGGER_SYNC.equals(intent.getAction())) {
            syncEngine.triggerSync();
        } else {
            // Check if currently on Wi-Fi
            if (isWifiConnected()) {
                syncEngine.startEngine();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterWifiNetworkCallback();
        syncEngine.stopEngine();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Dussehra Wi-Fi Synchronization",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Automatic photo synchronization between paired devices on local Wi-Fi");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void startInForeground(String title, String content) {
        Notification notification = buildNotification(title, content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String title, String content) {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content));
        }
    }

    private Notification buildNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, PairDevicesActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void setupSyncEngineListener() {
        syncEngine.addListener(new SyncEngine.SyncEngineListener() {
            @Override
            public void onStateChanged(SyncEngine.SyncState state, String detailMessage) {
                switch (state) {
                    case SYNCING:
                        updateNotification("Syncing Dussehra Photos", detailMessage);
                        break;
                    case SYNC_SUCCESS:
                        updateNotification("Photos Synced", detailMessage);
                        break;
                    case PEER_FOUND:
                        updateNotification("Paired Phone Connected", detailMessage);
                        break;
                    case SEARCHING_PEER:
                        updateNotification("Dussehra Wi-Fi Sync Active", "Searching for paired phone on Wi-Fi...");
                        break;
                    case UNPAIRED:
                        updateNotification("Dussehra Wi-Fi Sync", "Pair with another phone to enable auto-sync");
                        break;
                    case DISCONNECTED_WIFI:
                        updateNotification("Dussehra Wi-Fi Sync Paused", "Waiting for Wi-Fi connection...");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onProgress(String currentFile, int current, int total) {
                updateNotification("Transferring Photos (" + current + "/" + total + ")", currentFile);
            }
        });
    }

    private void registerWifiNetworkCallback() {
        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Wi-Fi network available: Starting SyncEngine");
                syncEngine.startEngine();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Wi-Fi network lost: Stopping SyncEngine");
                syncEngine.stopEngine();
            }
        };

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback);
        } catch (Exception e) {
            Log.e(TAG, "Error registering network callback: " + e.getMessage());
        }
    }

    private void unregisterWifiNetworkCallback() {
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            }
        } catch (Exception ignored) {}
    }

    private boolean isWifiConnected() {
        if (connectivityManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network active = connectivityManager.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            android.net.NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;
        }
    }
}
