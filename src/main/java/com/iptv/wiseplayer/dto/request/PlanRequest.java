package com.iptv.wiseplayer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {
    private String name;
    private int durationDays;
    private BigDecimal price;
    private String currency;
    private String description;
}
