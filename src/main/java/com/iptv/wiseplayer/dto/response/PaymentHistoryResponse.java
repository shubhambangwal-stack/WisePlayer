package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {
    private UUID paymentId;
    private LocalDateTime transactionDate;
    private SubscriptionPlan plan;
    private String planDisplayName;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
}
