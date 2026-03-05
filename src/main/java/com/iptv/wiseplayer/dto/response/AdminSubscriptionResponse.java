package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AdminSubscriptionResponse {
    private UUID subscriptionId;
    private UUID deviceId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
