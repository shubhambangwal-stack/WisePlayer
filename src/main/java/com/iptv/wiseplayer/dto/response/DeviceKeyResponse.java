package com.iptv.wiseplayer.dto.response;

import java.time.LocalDateTime;

/**
 * Response containing the generated activation key.
 * The key is provided ONLY here and never stored in plain text.
 */
public class DeviceKeyResponse {

    private String activationKey;
    private long expiresInSeconds;
    private LocalDateTime expiresAt;

    public DeviceKeyResponse() {
    }

    public DeviceKeyResponse(String activationKey, long expiresInSeconds, LocalDateTime expiresAt) {
        this.activationKey = activationKey;
        this.expiresInSeconds = expiresInSeconds;
        this.expiresAt = expiresAt;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
