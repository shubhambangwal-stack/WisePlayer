package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for device validation.
 * Returns device status, access permission, and optional denial message.
 */
public class DeviceValidationResponse {

    private UUID deviceId;
    private DeviceStatus status;
    /**
     * The actual plan name from subscription_plan_configs
     * (e.g., "TRIAL", "Gold Annual", "Weekly").
     * Replaces the old hard-coded subscriptionType enum.
     */
    private String planName;
    /**
     * Canonical subscription status: ACTIVE, TRIAL, EXPIRED, PAUSED, CANCELLED.
     * Used by the website's checkPlanExpired() logic.
     */
    private SubscriptionStatus subscriptionStatus;
    private String token;
    private boolean allowed;
    private String message;
    private String deviceSecret;
    private LocalDateTime lastSeenAt;
    /**
     * Plan expiry datetime. Used by checkPlanExpired() date-based check.
     * null means device was never activated (brand-new device).
     */
    private LocalDateTime expiresAt;

    // Constructors
    public DeviceValidationResponse() {
    }

    public DeviceValidationResponse(UUID deviceId, DeviceStatus status, String planName,
            String token, boolean allowed, String message, LocalDateTime lastSeenAt) {
        this.deviceId = deviceId;
        this.status = status;
        this.planName = planName;
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

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
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

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
