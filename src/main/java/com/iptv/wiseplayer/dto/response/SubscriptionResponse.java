package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response containing subscription details.
 */
public class SubscriptionResponse {

    private UUID subscriptionId;
    private UUID deviceId;
    private String planName;
    private SubscriptionStatus status;
    private SubscriptionType type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public SubscriptionResponse() {
    }

    public SubscriptionResponse(UUID subscriptionId, UUID deviceId, String planName, SubscriptionStatus status,
            SubscriptionType type, LocalDateTime startDate, LocalDateTime endDate) {
        this.subscriptionId = subscriptionId;
        this.deviceId = deviceId;
        this.planName = planName;
        this.status = status;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
