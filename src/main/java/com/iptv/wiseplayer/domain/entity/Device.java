package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Device entity representing a registered TV device.
 * Stores hashed fingerprint for security - never stores raw MAC address.
 */
@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_fingerprint_hash", columnList = "fingerprint_hash", unique = true)
})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "device_id", updatable = false, nullable = false)
    private UUID deviceId;

    /**
     * SHA-256 hash of device fingerprint (MAC + hardware data).
     * NEVER store raw MAC address.
     */
    @Column(name = "fingerprint_hash", nullable = false, unique = true, length = 64)
    private String fingerprintHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_status", nullable = false, length = 20)
    private DeviceStatus deviceStatus;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Device() {
    }

    public Device(String fingerprintHash, DeviceStatus deviceStatus) {
        this.fingerprintHash = fingerprintHash;
        this.deviceStatus = deviceStatus;
        this.registeredAt = LocalDateTime.now();
        this.lastSeenAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getFingerprintHash() {
        return fingerprintHash;
    }

    public void setFingerprintHash(String fingerprintHash) {
        this.fingerprintHash = fingerprintHash;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
