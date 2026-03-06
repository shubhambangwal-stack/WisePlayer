package com.iptv.wiseplayer.security;

import com.iptv.wiseplayer.config.SecurityProperties;
import org.springframework.stereotype.Component;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.StringJoiner;

@Component
public class AdminTokenUtil {

    private final SecurityProperties securityProperties;

    public AdminTokenUtil(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String generateToken(String username, AdminRole role) {
        long expiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours for admin

        String payload = new StringJoiner("|")
                .add(username)
                .add(role.name())
                .add(String.valueOf(expiry))
                .toString();

        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signature = sign(encodedPayload, securityProperties.getTokenSecrets().get(0));

        return encodedPayload + "." + signature;
    }

    public String[] verifyAndExtract(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("Invalid admin token format");
        }

        String encodedPayload = parts[0];
        String providedSignature = parts[1];

        boolean validSignature = false;
        for (String secret : securityProperties.getTokenSecrets()) {
            if (sign(encodedPayload, secret).equals(providedSignature)) {
                validSignature = true;
                break;
            }
        }

        if (!validSignature) {
            throw new RuntimeException("Invalid admin token signature");
        }

        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        String[] data = payload.split("\\|");
        if (data.length != 3) {
            throw new RuntimeException("Invalid admin token payload");
        }

        long expiry = Long.parseLong(data[2]);
        if (System.currentTimeMillis() > expiry) {
            throw new RuntimeException("Admin token has expired");
        }

        return data; // [username, role, expiry]
    }

    private String sign(String data, String secret) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);

            byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign admin data", e);
        }
    }
}
