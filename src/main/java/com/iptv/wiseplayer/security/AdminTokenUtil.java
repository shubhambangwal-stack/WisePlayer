package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.config.SecurityProperties;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
        long expiryMillis = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(expiryMillis))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String[] verifyAndExtract(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            long expiry = claims.getExpiration().getTime();

            return new String[] { username, role, String.valueOf(expiry) };
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired admin token", e);
        }
    }
}
