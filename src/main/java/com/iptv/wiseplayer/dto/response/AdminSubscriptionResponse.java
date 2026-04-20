package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class AdminSubscriptionResponse {
    private UUID subscriptionId;
    private UUID deviceId;
    private String planName;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}
