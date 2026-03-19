package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.config.SecurityProperties;
import com.iptv.wiseplayer.exception.DeviceAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
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

    private SecretKey getSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a standard JWT token.
     */
    public String generateToken(String deviceId, String fingerprintHash) {
        long expiryMillis = System.currentTimeMillis() + (securityProperties.getTokenTtlMinutes() * 60 * 1000);

        SecretKey primaryKey = getSigningKey(securityProperties.getTokenSecrets().get(0));

        return Jwts.builder()
                .setSubject(deviceId)
                .claim("fingerprintHash", fingerprintHash)
                .setIssuedAt(new Date())
                .setExpiration(new Date(expiryMillis))
                .signWith(primaryKey, SignatureAlgorithm.HS256)
                .compact();
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
        Claims claims = null;
        Exception lastException = null;

        // Try parsing with all available secrets (for rotation support)
        for (String secret : securityProperties.getTokenSecrets()) {
            try {
                claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey(secret))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                break; // Successfully parsed
            } catch (Exception e) {
                lastException = e;
            }
        }

        if (claims == null) {
            throw new DeviceAuthenticationException("Token verification failed, invalid signature or token.",
                    lastException);
        }

        try {
            String deviceId = claims.getSubject();
            String tokenFingerprint = claims.get("fingerprintHash", String.class);

            // Check expiry is already handled by Jwts.parserBuilder()

            // Match fingerprint (hash the raw input first)
            String requestFingerprintHash = hashFingerprint(requestFingerprint);
            if (!tokenFingerprint.equals(requestFingerprintHash)) {
                throw new DeviceAuthenticationException("Fingerprint mismatch");
            }

            Map<String, String> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("fingerprintHash", tokenFingerprint);
            return result;

        } catch (DeviceAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new DeviceAuthenticationException("Token verification failed", e);
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
