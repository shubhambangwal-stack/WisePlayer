package com.iptv.wiseplayer.dto.request;

import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    private String deviceId; // Fingerprint
    private SubscriptionPlan plan;
}
