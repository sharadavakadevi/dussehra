package com.example.dussehraphotoorganizer.sync.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.net.DeviceDiscoveryManager;
import com.example.dussehraphotoorganizer.sync.net.SyncPacket;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unified Sync Engine that manages:
 * 1. Background SyncServer (port 8989)
 * 2. Peer Discovery (NSD + UDP Beacon)
 * 3. Automatic bidirectional client triggers on new photo captures / Wi-Fi reconnections
 */
public class SyncEngine {

    private static final String TAG = "SyncEngine";

    public enum SyncState {
        UNPAIRED,
        DISCONNECTED_WIFI,
        SEARCHING_PEER,
        PEER_FOUND,
        SYNCING,
        SYNC_SUCCESS,
        SYNC_ERROR
    }

    public interface SyncEngineListener {
        void onStateChanged(SyncState state, String detailMessage);
        void onProgress(String currentFile, int current, int total);
    }

    private static volatile SyncEngine instance;

    private final Context context;
    private final SyncLedgerRepository ledgerRepository;
    private final SyncServer syncServer;
    private final SyncClient syncClient;
    private final DeviceDiscoveryManager discoveryManager;
    private final Handler mainHandler;

    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<SyncEngineListener> listeners = new CopyOnWriteArrayList<>();

    private SyncState currentState = SyncState.SEARCHING_PEER;
    private String currentDetail = "";
    private String peerDiscoveredIp = null;
    private int peerDiscoveredPort = SyncPacket.DEFAULT_TCP_PORT;

    public static synchronized SyncEngine getInstance(Context context) {
        if (instance == null) {
            instance = new SyncEngine(context.getApplicationContext());
        }
        return instance;
    }

    private SyncEngine(Context context) {
        this.context = context;
        this.ledgerRepository = new SyncLedgerRepository(context);
        this.syncServer = new SyncServer(context, ledgerRepository);
        this.syncClient = new SyncClient(context, ledgerRepository);
        this.discoveryManager = new DeviceDiscoveryManager(context, ledgerRepository);
        this.mainHandler = new Handler(Looper.getMainLooper());

        setupDiscoveryCallback();
        setupServerCallback();
    }

    public void addListener(SyncEngineListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            listener.onStateChanged(currentState, currentDetail);
        }
    }

    public void removeListener(SyncEngineListener listener) {
        listeners.remove(listener);
    }

    public SyncState getCurrentState() {
        return currentState;
    }

    public String getCurrentDetail() {
        return currentDetail;
    }

    public SyncLedgerRepository getLedgerRepository() {
        return ledgerRepository;
    }

    public synchronized void startEngine() {
        if (!ledgerRepository.isAutoWifiSyncEnabled()) {
            Log.d(TAG, "Auto Wi-Fi sync disabled in settings");
            return;
        }

        // 1. Start Server
        syncServer.start(SyncPacket.DEFAULT_TCP_PORT);

        // 2. Start Discovery
        discoveryManager.start(SyncPacket.DEFAULT_TCP_PORT);

        if (!ledgerRepository.isPaired()) {
            updateState(SyncState.UNPAIRED, "Not paired with another phone");
        } else {
            updateState(SyncState.SEARCHING_PEER, "Searching for " + ledgerRepository.getPairedDeviceName() + " on Wi-Fi...");
        }
    }

    public synchronized void stopEngine() {
        discoveryManager.stop();
        syncServer.stop();
        updateState(SyncState.DISCONNECTED_WIFI, "Wi-Fi Sync Stopped");
    }

    private void setupDiscoveryCallback() {
        discoveryManager.setDiscoveryCallback((deviceId, name, ip, port) -> {
            if (ledgerRepository.isPaired()) {
                if (deviceId.equals(ledgerRepository.getPairedDeviceId()) || name.contains(ledgerRepository.getPairedDeviceName())) {
                    peerDiscoveredIp = ip;
                    peerDiscoveredPort = port;
                    ledgerRepository.setLastKnownIp(ip);
                    updateState(SyncState.PEER_FOUND, "Connected with " + ledgerRepository.getPairedDeviceName() + " (" + ip + ")");

                    // Automatically trigger sync when paired peer is discovered
                    triggerSync();
                }
            } else {
                updateState(SyncState.UNPAIRED, "Found nearby device: " + name + " (" + ip + ")");
            }
        });
    }

    private void setupServerCallback() {
        syncServer.setListener(new SyncServer.SyncServerListener() {
            @Override
            public void onSyncStarted(String remoteDeviceName) {
                updateState(SyncState.SYNCING, "Incoming sync from " + remoteDeviceName);
            }

            @Override
            public void onPhotoReceived(String fileName, int current, int total) {
                notifyProgress("Receiving: " + fileName, current, total);
            }

            @Override
            public void onPhotoSent(String fileName, int current, int total) {
                notifyProgress("Sending: " + fileName, current, total);
            }

            @Override
            public void onSyncFinished(String summary) {
                updateState(SyncState.SYNC_SUCCESS, summary);
            }

            @Override
            public void onSyncError(String errorMessage) {
                updateState(SyncState.SYNC_ERROR, "Server error: " + errorMessage);
            }
        });
    }

    /**
     * Automatic / background trigger to sync with paired peer.
     * Guaranteed no-op if already syncing or not paired.
     */
    public void triggerSync() {
        if (!ledgerRepository.isPaired()) {
            updateState(SyncState.UNPAIRED, "Pair devices to enable automatic synchronization.");
            return;
        }

        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "Sync already in progress, skipping duplicate trigger");
            return;
        }

        new Thread(() -> {
            try {
                String targetIp = peerDiscoveredIp != null ? peerDiscoveredIp : ledgerRepository.getLastKnownIp();
                if (targetIp == null || targetIp.isEmpty()) {
                    updateState(SyncState.SEARCHING_PEER, "Searching for paired device on local Wi-Fi...");
                    isSyncing.set(false);
                    return;
                }

                updateState(SyncState.SYNCING, "Synchronizing photos with " + ledgerRepository.getPairedDeviceName() + "...");

                SyncClient.SyncResult result = syncClient.executeSync(targetIp, peerDiscoveredPort, this::notifyProgress);

                if (result.success) {
                    updateState(SyncState.SYNC_SUCCESS, result.message);
                } else {
                    updateState(SyncState.SYNC_ERROR, result.message);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during triggered sync: " + e.getMessage(), e);
                updateState(SyncState.SYNC_ERROR, "Sync error: " + e.getMessage());
            } finally {
                isSyncing.set(false);
            }
        }).start();
    }

    /**
     * Manually initiate pairing with target IP and PIN.
     */
    public void pairWithDevice(String targetIp, int port, String pin, PairResultCallback callback) {
        new Thread(() -> {
            boolean success = syncClient.performPairing(targetIp, port, pin);
            mainHandler.post(() -> {
                if (success) {
                    peerDiscoveredIp = targetIp;
                    peerDiscoveredPort = port;
                    updateState(SyncState.PEER_FOUND, "Paired with " + ledgerRepository.getPairedDeviceName());
                    triggerSync();
                    callback.onResult(true, "Paired successfully with " + ledgerRepository.getPairedDeviceName());
                } else {
                    callback.onResult(false, "Pairing failed. Verify PIN and ensure both phones are on the same Wi-Fi.");
                }
            });
        }).start();
    }

    public interface PairResultCallback {
        void onResult(boolean success, String message);
    }

    private void updateState(SyncState state, String detail) {
        currentState = state;
        currentDetail = detail;
        mainHandler.post(() -> {
            for (SyncEngineListener l : listeners) {
                l.onStateChanged(state, detail);
            }
        });
    }

    private void notifyProgress(String currentFile, int current, int total) {
        mainHandler.post(() -> {
            for (SyncEngineListener l : listeners) {
                l.onProgress(currentFile, current, total);
            }
        });
    }
}
