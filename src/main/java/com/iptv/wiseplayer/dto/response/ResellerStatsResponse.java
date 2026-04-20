package com.iptv.wiseplayer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResellerStatsResponse {
    private long totalUsers;
    private long activeSubscriptions;
    private double growthPercentage;
    private BigDecimal remainingCredits;
    private String partnerLevel;
    private String peakActivationTime;
}
