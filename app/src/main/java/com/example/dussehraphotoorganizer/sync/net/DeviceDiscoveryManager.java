package com.example.dussehraphotoorganizer.sync.net;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Handles automatic peer discovery on local Wi-Fi.
 * Implements dual-layer discovery:
 * 1. Android NsdManager (DNS-SD / mDNS service type: _dussehra-sync._tcp)
 * 2. UDP Broadcast Beacon on port 8990 (fallback if router drops mDNS multicast)
 */
public class DeviceDiscoveryManager {

    private static final String TAG = "DeviceDiscovery";
    public static final String SERVICE_TYPE = "_dussehra-sync._tcp.";
    public static final String BEACON_PREFIX = "DUSSEHRA_BEACON_V1:";

    private final Context context;
    private final NsdManager nsdManager;
    private final WifiManager wifiManager;
    private final SyncLedgerRepository ledgerRepository;
    private final Handler mainHandler;

    private WifiManager.MulticastLock multicastLock;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private DatagramSocket udpSocket;
    private Thread udpListenThread;
    private Thread udpBeaconThread;
    private volatile boolean isRunning = false;

    public interface OnDeviceDiscoveredListener {
        void onDeviceDiscovered(String deviceId, String deviceName, String ipAddress, int port);
    }

    private OnDeviceDiscoveredListener discoveryCallback;

    public DeviceDiscoveryManager(Context context, SyncLedgerRepository ledgerRepository) {
        this.context = context.getApplicationContext();
        this.nsdManager = (NsdManager) this.context.getSystemService(Context.NSD_SERVICE);
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.ledgerRepository = ledgerRepository;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setDiscoveryCallback(OnDeviceDiscoveredListener callback) {
        this.discoveryCallback = callback;
    }

    public synchronized void start(int servicePort) {
        if (isRunning) return;
        isRunning = true;

        acquireMulticastLock();
        registerNsdService(servicePort);
        startNsdDiscovery();
        startUdpListener();
        startUdpBeacon(servicePort);
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;

        stopNsdDiscovery();
        unregisterNsdService();
        stopUdpThreads();
        releaseMulticastLock();
    }

    private void acquireMulticastLock() {
        try {
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("dussehra_sync_multicast");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring multicast lock: " + e.getMessage());
        }
    }

    private void releaseMulticastLock() {
        try {
            if (multicastLock != null && multicastLock.isHeld()) {
                multicastLock.release();
            }
        } catch (Exception ignored) {}
    }

    // ==========================================
    // NSD / mDNS Layer
    // ==========================================

    private void registerNsdService(int port) {
        try {
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            String cleanName = "Dussehra_" + ledgerRepository.getOwnDeviceId().substring(0, 8);
            serviceInfo.setServiceName(cleanName);
            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setPort(port);

            registrationListener = new NsdManager.RegistrationListener() {
                @Override
                public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                    Log.d(TAG, "NSD Service registered: " + nsdServiceInfo.getServiceName());
                }

                @Override
                public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.e(TAG, "NSD Registration failed: " + errorCode);
                }

                @Override
                public void onServiceUnregistered(NsdServiceInfo arg0) {
                    Log.d(TAG, "NSD Service unregistered");
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.e(TAG, "NSD Unregistration failed: " + errorCode);
                }
            };

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register NSD service: " + e.getMessage());
        }
    }

    private void unregisterNsdService() {
        try {
            if (registrationListener != null) {
                nsdManager.unregisterService(registrationListener);
                registrationListener = null;
            }
        } catch (Exception ignored) {}
    }

    private void startNsdDiscovery() {
        try {
            discoveryListener = new NsdManager.DiscoveryListener() {
                @Override
                public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "NSD Discovery start failed: " + errorCode);
                }

                @Override
                public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "NSD Discovery stop failed: " + errorCode);
                }

                @Override
                public void onDiscoveryStarted(String serviceType) {
                    Log.d(TAG, "NSD Discovery started for " + serviceType);
                }

                @Override
                public void onDiscoveryStopped(String serviceType) {
                    Log.d(TAG, "NSD Discovery stopped");
                }

                @Override
                public void onServiceFound(NsdServiceInfo serviceInfo) {
                    if (serviceInfo.getServiceType().contains("dussehra-sync")) {
                        try {
                            nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                                @Override
                                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                                    Log.e(TAG, "NSD Resolve failed: " + errorCode);
                                }

                                @Override
                                public void onServiceResolved(NsdServiceInfo resolved) {
                                    if (resolved.getHost() != null) {
                                        String ip = resolved.getHost().getHostAddress();
                                        int port = resolved.getPort();
                                        String serviceName = resolved.getServiceName();
                                        notifyDiscovered("", serviceName, ip, port);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error resolving service: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onServiceLost(NsdServiceInfo serviceInfo) {
                    Log.d(TAG, "NSD Service lost: " + serviceInfo.getServiceName());
                }
            };

            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start NSD discovery: " + e.getMessage());
        }
    }

    private void stopNsdDiscovery() {
        try {
            if (discoveryListener != null) {
                nsdManager.stopServiceDiscovery(discoveryListener);
                discoveryListener = null;
            }
        } catch (Exception ignored) {}
    }

    // ==========================================
    // UDP Broadcast Beacon Layer (Robust Fallback)
    // ==========================================

    private void startUdpListener() {
        udpListenThread = new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(SyncPacket.DEFAULT_UDP_PORT);
                udpSocket.setBroadcast(true);
                byte[] buffer = new byte[1024];

                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (msg.startsWith(BEACON_PREFIX)) {
                        String[] parts = msg.substring(BEACON_PREFIX.length()).split(":");
                        if (parts.length >= 3) {
                            String remoteDeviceId = parts[0];
                            String remoteName = parts[1];
                            int remotePort = Integer.parseInt(parts[2]);
                            String remoteIp = packet.getAddress().getHostAddress();

                            // Don't discover self
                            if (!remoteDeviceId.equals(ledgerRepository.getOwnDeviceId())) {
                                notifyDiscovered(remoteDeviceId, remoteName, remoteIp, remotePort);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (isRunning) Log.e(TAG, "UDP listener error: " + e.getMessage());
            } finally {
                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
        });
        udpListenThread.start();
    }

    private void startUdpBeacon(int localTcpPort) {
        udpBeaconThread = new Thread(() -> {
            DatagramSocket senderSocket = null;
            try {
                senderSocket = new DatagramSocket();
                senderSocket.setBroadcast(true);

                while (isRunning) {
                    try {
                        InetAddress broadcastAddr = getBroadcastAddress();
                        if (broadcastAddr != null) {
                            String beaconPayload = BEACON_PREFIX +
                                    ledgerRepository.getOwnDeviceId() + ":" +
                                    ledgerRepository.getOwnDeviceName() + ":" +
                                    localTcpPort;

                            byte[] data = beaconPayload.getBytes(StandardCharsets.UTF_8);
                            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddr, SyncPacket.DEFAULT_UDP_PORT);
                            senderSocket.send(packet);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Beacon broadcast attempt: " + e.getMessage());
                    }

                    // Send beacon every 4 seconds
                    Thread.sleep(4000);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                Log.e(TAG, "UDP Beacon loop error: " + e.getMessage());
            } finally {
                if (senderSocket != null && !senderSocket.isClosed()) {
                    senderSocket.close();
                }
            }
        });
        udpBeaconThread.start();
    }

    private void stopUdpThreads() {
        try {
            if (udpSocket != null) udpSocket.close();
            if (udpListenThread != null) udpListenThread.interrupt();
            if (udpBeaconThread != null) udpBeaconThread.interrupt();
        } catch (Exception ignored) {}
    }

    private InetAddress getBroadcastAddress() {
        try {
            if (wifiManager == null) return InetAddress.getByName("255.255.255.255");
            DhcpInfo dhcp = wifiManager.getDhcpInfo();
            if (dhcp == null || dhcp.ipAddress == 0) return InetAddress.getByName("255.255.255.255");

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++) {
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            }
            return InetAddress.getByAddress(quads);
        } catch (Exception e) {
            try {
                return InetAddress.getByName("255.255.255.255");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private void notifyDiscovered(String deviceId, String name, String ip, int port) {
        mainHandler.post(() -> {
            if (discoveryCallback != null) {
                discoveryCallback.onDeviceDiscovered(deviceId, name, ip, port);
            }
        });
    }
}
