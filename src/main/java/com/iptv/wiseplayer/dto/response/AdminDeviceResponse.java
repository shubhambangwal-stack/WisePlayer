package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;

import java.time.LocalDateTime;
import java.util.UUID;

public class AdminDeviceResponse {
    private UUID deviceId;
    private String fingerprintHash;
    private DeviceStatus deviceStatus;
    private SubscriptionType subscriptionType;
    private String deviceModel;
    private String osVersion;
    private String platform;
    private LocalDateTime registeredAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime expiresAt;

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getFingerprintHash() { return fingerprintHash; }
    public void setFingerprintHash(String fingerprintHash) { this.fingerprintHash = fingerprintHash; }

    public DeviceStatus getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(DeviceStatus deviceStatus) { this.deviceStatus = deviceStatus; }

    public SubscriptionType getSubscriptionType() { return subscriptionType; }
    public void setSubscriptionType(SubscriptionType subscriptionType) { this.subscriptionType = subscriptionType; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
