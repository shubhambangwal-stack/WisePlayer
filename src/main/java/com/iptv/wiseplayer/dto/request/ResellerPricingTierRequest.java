package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResellerPricingTierRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Minimum quantity is required")
    private Integer minQuantity;

    private Integer maxQuantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;
}
