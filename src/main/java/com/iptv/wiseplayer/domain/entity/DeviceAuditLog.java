package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_audit_logs")
public class DeviceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private DeviceStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private DeviceStatus newStatus;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public DeviceAuditLog() {
    }

    public DeviceAuditLog(UUID deviceId, DeviceStatus oldStatus, DeviceStatus newStatus, String action, String reason) {
        this.deviceId = deviceId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.action = action;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(DeviceStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public DeviceStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(DeviceStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
