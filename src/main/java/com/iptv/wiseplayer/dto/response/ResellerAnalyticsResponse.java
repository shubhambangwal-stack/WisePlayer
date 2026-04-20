package com.iptv.wiseplayer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResellerAnalyticsResponse {
    private LocalDate date;
    private long activations;
    private BigDecimal revenue;
}
