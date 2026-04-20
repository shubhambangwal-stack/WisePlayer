package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.Min;

public class CreditPurchaseRequest {
    @Min(value = 1, message = "Amount must be at least 1")
    private int creditAmount;

    public int getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(int creditAmount) {
        this.creditAmount = creditAmount;
    }
}
