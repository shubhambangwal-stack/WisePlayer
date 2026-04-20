package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class CreditTransferRequest {

    @NotNull(message = "Sub-reseller ID is required")
    private UUID subResellerId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
    private BigDecimal amount;

    public CreditTransferRequest() {}

    public UUID getSubResellerId() { return subResellerId; }
    public void setSubResellerId(UUID subResellerId) { this.subResellerId = subResellerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
