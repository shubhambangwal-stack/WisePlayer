package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for device registration.
 * Returns device ID, status, and registration timestamp.
 */
public class DeviceRegistrationResponse {

    private UUID deviceId;
    private DeviceStatus status;
    private SubscriptionType subscriptionType;
    private String token;
    private String deviceSecret;
    private LocalDateTime registeredAt;

    // Constructors
    public DeviceRegistrationResponse() {
    }

    public DeviceRegistrationResponse(UUID deviceId, DeviceStatus status, SubscriptionType subscriptionType,
            String token, String deviceSecret,
            LocalDateTime registeredAt) {
        this.deviceId = deviceId;
        this.status = status;
        this.subscriptionType = subscriptionType;
        this.token = token;
        this.deviceSecret = deviceSecret;
        this.registeredAt = registeredAt;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}
