package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreditTransferRequest {

    @NotNull(message = "Sub-reseller ID is required")
    private UUID subResellerId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
    private BigDecimal amount;
}
