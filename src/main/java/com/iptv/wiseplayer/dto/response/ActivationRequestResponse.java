package com.iptv.wiseplayer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ActivationRequestResponse {
    private UUID id;
    private UUID resellerId;
    private UUID deviceId;
    private String planName;
    private Double amount;
    private String currency;
    private String status;
    private BigDecimal creditsUsed;
    private String adminNotes;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Reseller Details
    private String resellerUsername;

    // Device Details
    private String deviceModel;
    private String platform;
    private String deviceStatus;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getResellerId() { return resellerId; }
    public void setResellerId(UUID resellerId) { this.resellerId = resellerId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCreditsUsed() { return creditsUsed; }
    public void setCreditsUsed(BigDecimal creditsUsed) { this.creditsUsed = creditsUsed; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getResellerUsername() { return resellerUsername; }
    public void setResellerUsername(String resellerUsername) { this.resellerUsername = resellerUsername; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(String deviceStatus) { this.deviceStatus = deviceStatus; }
}
