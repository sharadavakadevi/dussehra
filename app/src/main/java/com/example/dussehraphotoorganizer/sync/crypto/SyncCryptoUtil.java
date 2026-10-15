package com.example.dussehraphotoorganizer.sync.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptographic utility providing:
 * 1. SHA-256 content hashing for exact photo identity & integrity verification.
 * 2. HMAC-SHA256 authentication of paired devices to prevent arbitrary Wi-Fi connections.
 * 3. AES-GCM encryption for secure peer-to-peer data streaming.
 * 4. Constant-time signature verification preventing timing attacks.
 */
public class SyncCryptoUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag

    public static String calculateSHA256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16384];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String calculateSHA256(File file) {
        if (file == null || !file.exists()) return null;
        try (InputStream is = new FileInputStream(file)) {
            return calculateSHA256(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] deriveKey(String secretString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secretString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static String generateRandomNonce() {
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[16];
        random.nextBytes(nonce);
        return bytesToHex(nonce);
    }

    public static String computeHmacHex(String data, byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean verifyHmac(String data, byte[] key, String expectedHex) {
        if (expectedHex == null || expectedHex.isEmpty()) return false;
        String actualHex = computeHmacHex(data, key);
        return MessageDigest.isEqual(
                actualHex.getBytes(StandardCharsets.UTF_8),
                expectedHex.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Encrypts payload with AES-GCM using derived 256-bit key.
     * Returns IV (12 bytes) prepended to ciphertext + auth tag.
     */
    public static byte[] encryptAesGcm(byte[] plaintext, byte[] key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] cipherText = cipher.doFinal(plaintext);
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        return combined;
    }

    /**
     * Decrypts AES-GCM payload.
     */
    public static byte[] decryptAesGcm(byte[] encryptedPayload, byte[] key) throws Exception {
        if (encryptedPayload.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted payload size");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedPayload, 0, iv, 0, GCM_IV_LENGTH);

        int cipherTextLength = encryptedPayload.length - GCM_IV_LENGTH;
        byte[] cipherText = new byte[cipherTextLength];
        System.arraycopy(encryptedPayload, GCM_IV_LENGTH, cipherText, 0, cipherTextLength);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        return cipher.doFinal(cipherText);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
