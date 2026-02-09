package com.iptv.wiseplayer.dto.request;

import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;
import lombok.Getter;
import lombok.Setter;

public class CheckoutRequest {
    private String deviceId; // Fingerprint
    private SubscriptionPlan plan;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }
}
