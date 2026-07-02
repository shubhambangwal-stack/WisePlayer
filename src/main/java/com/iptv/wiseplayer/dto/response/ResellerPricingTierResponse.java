package com.iptv.wiseplayer.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ResellerPricingTierResponse {
    private UUID id;
    private String name;
    private Integer minQuantity;
    private Integer maxQuantity;
    private BigDecimal unitPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
