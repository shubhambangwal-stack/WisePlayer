package com.iptv.wiseplayer.dto.response;

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
    private LocalDateTime registeredAt;

    // Constructors
    public DeviceRegistrationResponse() {
    }

    public DeviceRegistrationResponse(UUID deviceId, DeviceStatus status, LocalDateTime registeredAt) {
        this.deviceId = deviceId;
        this.status = status;
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

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}
