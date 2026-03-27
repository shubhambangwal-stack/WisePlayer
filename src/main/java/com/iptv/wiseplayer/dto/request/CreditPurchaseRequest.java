package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreditPurchaseRequest {
    @Min(value = 1, message = "Amount must be at least 1")
    private int creditAmount;
}
