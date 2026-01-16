package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response containing subscription details.
 */
public class SubscriptionResponse {

    private UUID subscriptionId;
    private UUID deviceId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public SubscriptionResponse() {
    }

    public SubscriptionResponse(UUID subscriptionId, UUID deviceId, SubscriptionPlan plan, SubscriptionStatus status,
            LocalDateTime startDate, LocalDateTime endDate) {
        this.subscriptionId = subscriptionId;
        this.deviceId = deviceId;
        this.plan = plan;
        this.status = status;
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

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
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
