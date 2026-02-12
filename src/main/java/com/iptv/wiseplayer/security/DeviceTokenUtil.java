package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.config.SecurityProperties;
import com.iptv.wiseplayer.exception.DeviceAuthenticationException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Component
public class DeviceTokenUtil {

    private final SecurityProperties securityProperties;

    public DeviceTokenUtil(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        if (securityProperties.getTokenSecrets() == null || securityProperties.getTokenSecrets().isEmpty()) {
            throw new IllegalStateException(
                    "At least one security secret must be configured in app.security.token-secrets");
        }
    }

    /**
     * Generates a token: base64(payload).signature
     * Payload contains deviceId, fingerprintHash, and expiry.
     */
    public String generateToken(String deviceId, String fingerprintHash) {
        long expiry = System.currentTimeMillis() + (securityProperties.getTokenTtlMinutes() * 60 * 1000);

        String payload = new StringJoiner("|")
                .add(deviceId)
                .add(fingerprintHash)
                .add(String.valueOf(expiry))
                .toString();

        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        // Always sign with the first (primary) secret
        String signature = sign(encodedPayload, securityProperties.getTokenSecrets().get(0));

        return encodedPayload + "." + signature;
    }

    /**
     * Generates a random secure refresh token.
     */
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public Map<String, String> verifyAndExtract(String token, String requestFingerprint) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new DeviceAuthenticationException("Invalid token format");
            }

            String encodedPayload = parts[0];
            String providedSignature = parts[1];

            // 1. Verify signature against all active secrets (Rotation support)
            boolean validSignature = false;
            for (String secret : securityProperties.getTokenSecrets()) {
                if (sign(encodedPayload, secret).equals(providedSignature)) {
                    validSignature = true;
                    break;
                }
            }

            if (!validSignature) {
                throw new DeviceAuthenticationException("Invalid token signature");
            }

            // 2. Extract payload
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] data = payload.split("\\|");
            if (data.length != 3) {
                throw new DeviceAuthenticationException("Invalid token payload");
            }

            String deviceId = data[0];
            String tokenFingerprint = data[1];
            long expiry = Long.parseLong(data[2]);

            // 3. Check expiry
            if (System.currentTimeMillis() > expiry) {
                throw new DeviceAuthenticationException("Token has expired");
            }

            // 4. Match fingerprint (hash the raw input first)
            String requestFingerprintHash = hashFingerprint(requestFingerprint);
            if (!tokenFingerprint.equals(requestFingerprintHash)) {
                throw new DeviceAuthenticationException("Fingerprint mismatch");
            }

            Map<String, String> claims = new HashMap<>();
            claims.put("deviceId", deviceId);
            claims.put("fingerprintHash", tokenFingerprint);
            return claims;

        } catch (Exception e) {
            if (e instanceof DeviceAuthenticationException)
                throw (DeviceAuthenticationException) e;
            throw new DeviceAuthenticationException("Token verification failed", e);
        }
    }

    private String sign(String data, String secret) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);

            byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }

    /**
     * Hash device fingerprint using SHA-256 (Hex encoded).
     */
    public String hashFingerprint(String fingerprint) {
        if (fingerprint == null)
            return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fingerprint.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Hash device secret using SHA-256 (Case Sensitive).
     */
    public String hashSecret(String secret) {
        if (secret == null)
            return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(secret.trim().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
