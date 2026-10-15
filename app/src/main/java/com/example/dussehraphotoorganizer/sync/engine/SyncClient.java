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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * TCP Sync Client connecting to the paired peer on port 8989.
 * Initiates handshake authentication, sends local camera photo catalog,
 * transfers missing photos, and receives remote photos missing locally.
 */
public class SyncClient {

    private static final String TAG = "SyncClient";

    private final Context context;
    private final SyncLedgerRepository ledgerRepository;
    private final PhotoDestinationSaver destinationSaver;
    private final SyncCatalogScanner catalogScanner;
    private final Gson gson;

    public interface SyncClientListener {
        void onProgress(String statusMessage, int current, int total);
    }

    public static class SyncResult {
        public final boolean success;
        public final int sentCount;
        public final int receivedCount;
        public final String message;

        public SyncResult(boolean success, int sentCount, int receivedCount, String message) {
            this.success = success;
            this.sentCount = sentCount;
            this.receivedCount = receivedCount;
            this.message = message;
        }
    }

    public SyncClient(Context context, SyncLedgerRepository ledgerRepository) {
        this.context = context.getApplicationContext();
        this.ledgerRepository = ledgerRepository;
        this.destinationSaver = new PhotoDestinationSaver(this.context, ledgerRepository);
        this.catalogScanner = new SyncCatalogScanner(this.context, ledgerRepository);
        this.gson = new Gson();
    }

    /**
     * Executes pair handshake with target device using a 6-digit PIN.
     */
    public boolean performPairing(String targetIp, int port, String enteredPin) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetIp, port), 5000);
            socket.setSoTimeout(15000);

            OutputStream rawOut = socket.getOutputStream();
            InputStream rawIn = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8), true);

            String sharedSecret = SyncCryptoUtil.generateRandomNonce();
            SyncPacket.HandshakeMsg handshake = new SyncPacket.HandshakeMsg();
            handshake.deviceId = ledgerRepository.getOwnDeviceId();
            handshake.deviceName = ledgerRepository.getOwnDeviceName();
            handshake.challengeNonce = sharedSecret; // The shared secret passed during pairing
            handshake.hmacSignature = enteredPin;   // Entered PIN for pairing validation

            writer.println(gson.toJson(handshake));

            String responseLine = reader.readLine();
            if (responseLine == null) return false;

            SyncPacket.HandshakeAck ack = gson.fromJson(responseLine, SyncPacket.HandshakeAck.class);
            if (ack != null && ack.success) {
                ledgerRepository.savePairing(ack.serverDeviceId, ack.serverDeviceName, sharedSecret, targetIp);
                Log.d(TAG, "Pairing succeeded with " + ack.serverDeviceName + " (" + targetIp + ")");
                return true;
            } else {
                Log.e(TAG, "Pairing rejected: " + (ack != null ? ack.errorMessage : "Unknown error"));
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Pairing connection error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes bidirectional automatic synchronization with paired peer.
     */
    public SyncResult executeSync(String targetIp, int port, SyncClientListener listener) {
        if (!ledgerRepository.isPaired()) {
            return new SyncResult(false, 0, 0, "Device is not paired.");
        }

        int sentCount = 0;
        int receivedCount = 0;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetIp, port), 6000);
            socket.setSoTimeout(30000);

            OutputStream rawOut = socket.getOutputStream();
            InputStream rawIn = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8), true);

            // 1. Handshake Phase
            String nonce = SyncCryptoUtil.generateRandomNonce();
            byte[] key = SyncCryptoUtil.deriveKey(ledgerRepository.getSharedSecret());
            String signature = SyncCryptoUtil.computeHmacHex(ledgerRepository.getOwnDeviceId() + ":" + nonce, key);

            SyncPacket.HandshakeMsg handshake = new SyncPacket.HandshakeMsg();
            handshake.deviceId = ledgerRepository.getOwnDeviceId();
            handshake.deviceName = ledgerRepository.getOwnDeviceName();
            handshake.challengeNonce = nonce;
            handshake.hmacSignature = signature;

            writer.println(gson.toJson(handshake));

            String ackLine = reader.readLine();
            if (ackLine == null) {
                return new SyncResult(false, 0, 0, "No handshake response from peer.");
            }

            SyncPacket.HandshakeAck ack = gson.fromJson(ackLine, SyncPacket.HandshakeAck.class);
            if (ack == null || !ack.success) {
                String err = (ack != null && ack.errorMessage != null) ? ack.errorMessage : "Peer rejected authentication.";
                return new SyncResult(false, 0, 0, err);
            }

            ledgerRepository.setLastKnownIp(targetIp);

            // 2. Bidirectional Catalog Exchange
            List<SyncPacket.PhotoCatalogItem> localPhotos = catalogScanner.scanLocalCameraPhotos();
            SyncPacket.CatalogMsg localCatalog = new SyncPacket.CatalogMsg();
            localCatalog.items = localPhotos;
            writer.println(gson.toJson(localCatalog));

            // Read server's pull request (photos server wants from us)
            String serverPullLine = reader.readLine();
            SyncPacket.PullRequestMsg serverWants = gson.fromJson(serverPullLine, SyncPacket.PullRequestMsg.class);

            // Read server's catalog
            String serverCatalogLine = reader.readLine();
            SyncPacket.CatalogMsg serverCatalog = gson.fromJson(serverCatalogLine, SyncPacket.CatalogMsg.class);

            // Determine what we want from server
            SyncPacket.PullRequestMsg clientWants = new SyncPacket.PullRequestMsg();
            if (serverCatalog != null && serverCatalog.items != null) {
                for (SyncPacket.PhotoCatalogItem item : serverCatalog.items) {
                    if (!ledgerRepository.isHashAlreadySyncedOrReceived(item.sha256)) {
                        clientWants.requestedHashes.add(item.sha256);
                    }
                }
            }
            writer.println(gson.toJson(clientWants));

            // 3. Transfer Outgoing: Client -> Server
            if (serverWants != null && serverWants.requestedHashes != null) {
                int totalToSend = serverWants.requestedHashes.size();
                for (int i = 0; i < totalToSend; i++) {
                    String hash = serverWants.requestedHashes.get(i);
                    Uri photoUri = catalogScanner.getUriForSha256(hash);
                    if (photoUri == null) continue;

                    SyncPacket.PhotoCatalogItem item = findItem(localPhotos, hash);
                    if (item == null) continue;

                    if (listener != null) {
                        listener.onProgress("Sending " + item.fileName, i + 1, totalToSend);
                    }

                    SyncPacket.PhotoHeaderMsg header = new SyncPacket.PhotoHeaderMsg();
                    header.sha256 = item.sha256;
                    header.fileName = item.fileName;
                    header.fileSize = item.fileSize;
                    header.captureTimestamp = item.captureTimestamp;
                    header.latitude = item.latitude;
                    header.longitude = item.longitude;
                    header.isoDate = item.isoDate;
                    header.formattedDate = item.formattedDate;

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

                    String transferAckLine = reader.readLine();
                    SyncPacket.PhotoTransferAck transferAck = gson.fromJson(transferAckLine, SyncPacket.PhotoTransferAck.class);
                    if (transferAck != null && transferAck.success) {
                        ledgerRepository.recordOutgoingPhotoSynced(hash);
                        ledgerRepository.addLogEntry(item.fileName, "SENT", "SUCCESS", "Synced to " + ack.serverDeviceName);
                        sentCount++;
                    }
                }
            }

            // 4. Transfer Incoming: Server -> Client
            int totalToReceive = clientWants.requestedHashes.size();
            for (int i = 0; i < totalToReceive; i++) {
                String headerLine = reader.readLine();
                if (headerLine == null) break;

                SyncPacket.PhotoHeaderMsg header = gson.fromJson(headerLine, SyncPacket.PhotoHeaderMsg.class);
                if (listener != null) {
                    listener.onProgress("Receiving " + header.fileName, i + 1, totalToReceive);
                }

                File tempFile = new File(context.getCacheDir(), "recv_client_" + System.currentTimeMillis() + ".tmp");
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
                SyncPacket.PhotoTransferAck transferAck = new SyncPacket.PhotoTransferAck();

                if (computedHash.equalsIgnoreCase(header.sha256)) {
                    boolean saved = destinationSaver.saveVerifiedPhoto(tempFile, header);
                    transferAck.success = saved;
                    transferAck.sha256 = header.sha256;
                    if (saved) {
                        receivedCount++;
                    }
                } else {
                    transferAck.success = false;
                    transferAck.errorMessage = "Checksum mismatch";
                }

                if (tempFile.exists()) tempFile.delete();
                writer.println(gson.toJson(transferAck));
            }

            ledgerRepository.setLastSyncTimestamp(System.currentTimeMillis());
            String summary = String.format("Auto-Sync Complete: %d Sent, %d Received", sentCount, receivedCount);
            return new SyncResult(true, sentCount, receivedCount, summary);

        } catch (Exception e) {
            Log.e(TAG, "Sync execution failed: " + e.getMessage(), e);
            return new SyncResult(false, sentCount, receivedCount, "Sync failed: " + e.getMessage());
        }
    }

    private SyncPacket.PhotoCatalogItem findItem(List<SyncPacket.PhotoCatalogItem> list, String hash) {
        for (SyncPacket.PhotoCatalogItem item : list) {
            if (hash.equals(item.sha256)) return item;
        }
        return null;
    }
}
