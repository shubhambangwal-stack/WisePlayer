package com.iptv.wiseplayer.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    // In a real production app, this key should be loaded from a secure vault or
    // env var.
    // For this implementation, we will use a static key (generated once) or a fixed
    // one for simplicity/persistence across restarts during dev.
    // Using a hardcoded key for MVP/Demo purposes to ensure data persists across
    // server restarts.
    private static final String FIXED_KEY_BASE64 = "cZ8/7q3g+P5f9L1k2m4n6p8r0t2v4x6z8A0C2E4G6I8=";
    private final SecretKey secretKey;

    public EncryptionUtil() {
        // Initialize with fixed key
        byte[] decodedKey = Base64.getDecoder().decode(FIXED_KEY_BASE64);
        this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    public String encrypt(String plainText) {
        if (plainText == null)
            return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            // Ideally secure random IV, but for simplicity/determinism in this specific
            // context we might just use random.
            // But GCM requires unique IV.
            new java.security.SecureRandom().nextBytes(iv);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to cipherText for storage
            byte[] message = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, message, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, message, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(message);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null)
            return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            // Extract Message
            int messageLength = decoded.length - GCM_IV_LENGTH;
            byte[] message = new byte[messageLength];
            System.arraycopy(decoded, GCM_IV_LENGTH, message, 0, messageLength);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] plainTextBytes = cipher.doFinal(message);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}
