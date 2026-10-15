package com.example.dussehraphotoorganizer.sync.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data contracts and packet models for P2P Wi-Fi synchronization.
 */
public class SyncPacket {

    public static final int PROTOCOL_VERSION = 1;
    public static final int DEFAULT_TCP_PORT = 8989;
    public static final int DEFAULT_UDP_PORT = 8990;

    public static class PhotoCatalogItem implements Serializable {
        public String sha256;
        public String fileName;
        public long fileSize;
        public long captureTimestamp;
        public Double latitude;
        public Double longitude;
        public String isoDate;
        public String formattedDate;

        public PhotoCatalogItem() {}

        public PhotoCatalogItem(String sha256, String fileName, long fileSize, long captureTimestamp,
                                Double latitude, Double longitude, String isoDate, String formattedDate) {
            this.sha256 = sha256;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.captureTimestamp = captureTimestamp;
            this.latitude = latitude;
            this.longitude = longitude;
            this.isoDate = isoDate;
            this.formattedDate = formattedDate;
        }
    }

    public static class HandshakeMsg implements Serializable {
        public int protocolVersion = PROTOCOL_VERSION;
        public String deviceId;
        public String deviceName;
        public String challengeNonce;
        public String hmacSignature; // HMAC(deviceId + ":" + challengeNonce, sharedSecret)
    }

    public static class HandshakeAck implements Serializable {
        public boolean success;
        public String serverDeviceId;
        public String serverDeviceName;
        public String responseNonce;
        public String hmacSignature;
        public String errorMessage;
    }

    public static class CatalogMsg implements Serializable {
        public List<PhotoCatalogItem> items = new ArrayList<>();
    }

    public static class PullRequestMsg implements Serializable {
        public List<String> requestedHashes = new ArrayList<>();
    }

    public static class PhotoHeaderMsg implements Serializable {
        public String sha256;
        public String fileName;
        public long fileSize;
        public long captureTimestamp;
        public Double latitude;
        public Double longitude;
        public String isoDate;
        public String formattedDate;
    }

    public static class PhotoTransferAck implements Serializable {
        public boolean success;
        public String sha256;
        public String savedRelativePath;
        public String errorMessage;
    }
}
