package com.iptv.wiseplayer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private UUID id;
    private String name;
    private int durationDays;
    private BigDecimal price;
    private String currency;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
}
