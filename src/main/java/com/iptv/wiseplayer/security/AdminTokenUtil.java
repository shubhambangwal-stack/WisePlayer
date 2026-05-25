package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.config.SecurityProperties;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AdminTokenUtil {

    private final SecurityProperties securityProperties;

    public AdminTokenUtil(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    private SecretKey getSigningKey() {
        String secret = securityProperties.getTokenSecrets().get(0);
        // Ensure the secret is long enough for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Pad secret if too short (not ideal, but safer than runtime failure)
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, AdminRole role) {
        Date issuedAt = new Date();
       // Date expiration = new Date(issuedAt.getTime() + (60 * 1000L)); // 1 minute
       Date expiration = new Date(issuedAt.getTime() + (24 * 60 * 60 * 1000L)); // 24 hours

        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public String[] verifyAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            long expiry = claims.getExpiration().getTime();

            return new String[] { username, role, String.valueOf(expiry) };
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired admin token", e);
        }
    }
}
