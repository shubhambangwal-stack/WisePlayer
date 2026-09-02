package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for device registration.
 * Returns device ID, status, and registration timestamp.
 */
public class DeviceRegistrationResponse {

    private UUID deviceId;
    private DeviceStatus status;
    private String planName;
    private String token;
    private String deviceSecret;
    private LocalDateTime registeredAt;

    // Constructors
    public DeviceRegistrationResponse() {
    }

    public DeviceRegistrationResponse(UUID deviceId, DeviceStatus status, String planName,
            String token, String deviceSecret,
            LocalDateTime registeredAt) {
        this.deviceId = deviceId;
        this.status = status;
        this.planName = planName;
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

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
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
