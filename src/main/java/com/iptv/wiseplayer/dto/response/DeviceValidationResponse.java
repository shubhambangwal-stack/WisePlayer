package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for device validation.
 * Returns device status, access permission, and optional denial message.
 */
public class DeviceValidationResponse {

    private UUID deviceId;
    private DeviceStatus status;
    private SubscriptionType subscriptionType;
    private String token;
    private boolean allowed;
    private String message;
    private LocalDateTime lastSeenAt;

    // Constructors
    public DeviceValidationResponse() {
    }

    public DeviceValidationResponse(UUID deviceId, DeviceStatus status, SubscriptionType subscriptionType, String token,
            boolean allowed, String message, LocalDateTime lastSeenAt) {
        this.deviceId = deviceId;
        this.status = status;
        this.subscriptionType = subscriptionType;
        this.token = token;
        this.allowed = allowed;
        this.message = message;
        this.lastSeenAt = lastSeenAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // Getters and Setters
    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
