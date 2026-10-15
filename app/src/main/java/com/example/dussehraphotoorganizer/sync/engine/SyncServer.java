package com.example.dussehraphotoorganizer.sync.engine;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.dussehraphotoorganizer.sync.crypto.SyncCryptoUtil;
import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.net.SyncPacket;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP Sync Server listening on port 8989 for incoming connections from paired devices.
 * Handles mutual challenge authentication, two-way catalog exchange, and verified streaming.
 */
public class SyncServer {

    private static final String TAG = "SyncServer";

    private final Context context;
    private final SyncLedgerRepository ledgerRepository;
    private final PhotoDestinationSaver destinationSaver;
    private final SyncCatalogScanner catalogScanner;
    private final Gson gson;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean isRunning = false;

    public interface SyncServerListener {
        void onSyncStarted(String remoteDeviceName);
        void onPhotoReceived(String fileName, int current, int total);
        void onPhotoSent(String fileName, int current, int total);
        void onSyncFinished(String summary);
        void onSyncError(String errorMessage);
    }

    private SyncServerListener listener;

    public SyncServer(Context context, SyncLedgerRepository ledgerRepository) {
        this.context = context.getApplicationContext();
        this.ledgerRepository = ledgerRepository;
        this.destinationSaver = new PhotoDestinationSaver(this.context, ledgerRepository);
        this.catalogScanner = new SyncCatalogScanner(this.context, ledgerRepository);
        this.gson = new Gson();
    }

    public void setListener(SyncServerListener listener) {
        this.listener = listener;
    }

    public synchronized void start(int port) {
        if (isRunning) return;
        isRunning = true;

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                Log.d(TAG, "SyncServer listening on port " + port);

                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClientConnection(clientSocket);
                    } catch (Exception e) {
                        if (isRunning) Log.e(TAG, "Error accepting client: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Server socket error: " + e.getMessage());
            } finally {
                stop();
            }
        });
        serverThread.start();
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}

        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    private void handleClientConnection(Socket socket) {
        new Thread(() -> {
            String clientIp = socket.getInetAddress().getHostAddress();
            Log.d(TAG, "Incoming connection from " + clientIp);

            try (
                InputStream rawIn = socket.getInputStream();
                OutputStream rawOut = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8), true)
            ) {
                // 1. Handshake Phase
                String handshakeLine = reader.readLine();
                if (handshakeLine == null) return;

                SyncPacket.HandshakeMsg handshake = gson.fromJson(handshakeLine, SyncPacket.HandshakeMsg.class);
                if (handshake == null) return;

                boolean authenticated = false;
                String sharedSecret = ledgerRepository.getSharedSecret();

                if (ledgerRepository.isPaired()) {
                    // Check paired device ID
                    if (handshake.deviceId.equals(ledgerRepository.getPairedDeviceId())) {
                        byte[] key = SyncCryptoUtil.deriveKey(sharedSecret);
                        String challengeData = handshake.deviceId + ":" + handshake.challengeNonce;
                        authenticated = SyncCryptoUtil.verifyHmac(challengeData, key, handshake.hmacSignature);
                    }
                } else {
                    // Pairing attempt: check if PIN matches
                    if (handshake.hmacSignature != null && handshake.hmacSignature.equals(ledgerRepository.getOwnPairingPin())) {
                        // Accept pairing
                        String newSecret = handshake.challengeNonce;
                        ledgerRepository.savePairing(handshake.deviceId, handshake.deviceName, newSecret, clientIp);
                        authenticated = true;
                    }
                }

                SyncPacket.HandshakeAck ack = new SyncPacket.HandshakeAck();
                ack.success = authenticated;
                ack.serverDeviceId = ledgerRepository.getOwnDeviceId();
                ack.serverDeviceName = ledgerRepository.getOwnDeviceName();

                if (!authenticated) {
                    ack.errorMessage = "Authentication failed. Devices not paired or invalid credentials.";
                    writer.println(gson.toJson(ack));
                    return;
                }

                ledgerRepository.setLastKnownIp(clientIp);
                writer.println(gson.toJson(ack));

                if (listener != null) {
                    listener.onSyncStarted(handshake.deviceName);
                }

                // 2. Bidirectional Catalog Exchange
                // Read client catalog
                String clientCatalogLine = reader.readLine();
                SyncPacket.CatalogMsg clientCatalog = gson.fromJson(clientCatalogLine, SyncPacket.CatalogMsg.class);

                // Determine which photos server needs from client
                SyncPacket.PullRequestMsg serverWants = new SyncPacket.PullRequestMsg();
                if (clientCatalog != null && clientCatalog.items != null) {
                    for (SyncPacket.PhotoCatalogItem item : clientCatalog.items) {
                        if (!ledgerRepository.isHashAlreadySyncedOrReceived(item.sha256)) {
                            serverWants.requestedHashes.add(item.sha256);
                        }
                    }
                }
                writer.println(gson.toJson(serverWants));

                // Send server's catalog to client
                List<SyncPacket.PhotoCatalogItem> serverLocalPhotos = catalogScanner.scanLocalCameraPhotos();
                SyncPacket.CatalogMsg serverCatalog = new SyncPacket.CatalogMsg();
                serverCatalog.items = serverLocalPhotos;
                writer.println(gson.toJson(serverCatalog));

                // Read client's pull request (photos client wants from server)
                String clientPullLine = reader.readLine();
                SyncPacket.PullRequestMsg clientWants = gson.fromJson(clientPullLine, SyncPacket.PullRequestMsg.class);

                // 3. Receive Incoming Photos from Client -> Server
                int receivedCount = 0;
                int incomingTotal = serverWants.requestedHashes.size();

                for (int i = 0; i < incomingTotal; i++) {
                    String photoHeaderLine = reader.readLine();
                    if (photoHeaderLine == null || photoHeaderLine.equals("TRANSFER_CANCELLED")) break;

                    SyncPacket.PhotoHeaderMsg header = gson.fromJson(photoHeaderLine, SyncPacket.PhotoHeaderMsg.class);
                    File tempFile = new File(context.getCacheDir(), "recv_" + System.currentTimeMillis() + ".tmp");

                    MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[16384];
                        long remaining = header.fileSize;
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buffer.length, remaining);
                            int read = rawIn.read(buffer, 0, toRead);
                            if (read == -1) break;
                            fos.write(buffer, 0, read);
                            shaDigest.update(buffer, 0, read);
                            remaining -= read;
                        }
                        fos.flush();
                    }

                    String computedHash = SyncCryptoUtil.bytesToHex(shaDigest.digest());
                    SyncPacket.PhotoTransferAck photoAck = new SyncPacket.PhotoTransferAck();

                    if (computedHash.equalsIgnoreCase(header.sha256)) {
                        boolean saved = destinationSaver.saveVerifiedPhoto(tempFile, header);
                        photoAck.success = saved;
                        photoAck.sha256 = header.sha256;
                        if (saved) {
                            receivedCount++;
                            if (listener != null) {
                                listener.onPhotoReceived(header.fileName, receivedCount, incomingTotal);
                            }
                        }
                    } else {
                        photoAck.success = false;
                        photoAck.errorMessage = "SHA-256 hash checksum mismatch";
                    }

                    if (tempFile.exists()) tempFile.delete();
                    writer.println(gson.toJson(photoAck));
                }

                // 4. Send Outgoing Photos from Server -> Client
                int sentCount = 0;
                if (clientWants != null && clientWants.requestedHashes != null) {
                    int outgoingTotal = clientWants.requestedHashes.size();

                    for (String hash : clientWants.requestedHashes) {
                        Uri photoUri = catalogScanner.getUriForSha256(hash);
                        if (photoUri == null) continue;

                        SyncPacket.PhotoCatalogItem catalogItem = findCatalogItem(serverLocalPhotos, hash);
                        if (catalogItem == null) continue;

                        SyncPacket.PhotoHeaderMsg header = new SyncPacket.PhotoHeaderMsg();
                        header.sha256 = catalogItem.sha256;
                        header.fileName = catalogItem.fileName;
                        header.fileSize = catalogItem.fileSize;
                        header.captureTimestamp = catalogItem.captureTimestamp;
                        header.latitude = catalogItem.latitude;
                        header.longitude = catalogItem.longitude;
                        header.isoDate = catalogItem.isoDate;
                        header.formattedDate = catalogItem.formattedDate;

                        writer.println(gson.toJson(header));

                        try (InputStream is = context.getContentResolver().openInputStream(photoUri)) {
                            if (is != null) {
                                byte[] buffer = new byte[16384];
                                int len;
                                while ((len = is.read(buffer)) > 0) {
                                    rawOut.write(buffer, 0, len);
                                }
                                rawOut.flush();
                            }
                        }

                        // Read ack from client
                        String ackLine = reader.readLine();
                        SyncPacket.PhotoTransferAck clientAck = gson.fromJson(ackLine, SyncPacket.PhotoTransferAck.class);
                        if (clientAck != null && clientAck.success) {
                            ledgerRepository.recordOutgoingPhotoSynced(hash);
                            ledgerRepository.addLogEntry(header.fileName, "SENT", "SUCCESS", "Synced to " + handshake.deviceName);
                            sentCount++;
                            if (listener != null) {
                                listener.onPhotoSent(header.fileName, sentCount, outgoingTotal);
                            }
                        }
                    }
                }

                ledgerRepository.setLastSyncTimestamp(System.currentTimeMillis());
                String summary = String.format("Sync Completed: %d Received, %d Sent", receivedCount, sentCount);
                if (listener != null) {
                    listener.onSyncFinished(summary);
                }

            } catch (Exception e) {
                Log.e(TAG, "Sync session error: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onSyncError(e.getMessage());
                }
            }
        }).start();
    }

    private SyncPacket.PhotoCatalogItem findCatalogItem(List<SyncPacket.PhotoCatalogItem> list, String hash) {
        for (SyncPacket.PhotoCatalogItem item : list) {
            if (hash.equals(item.sha256)) return item;
        }
        return null;
    }
}
