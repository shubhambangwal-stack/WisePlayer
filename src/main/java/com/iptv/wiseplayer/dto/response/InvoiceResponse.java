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
public class InvoiceResponse {
    private String invoiceNumber;
    private UUID paymentId;
    private UUID deviceId;
    private LocalDateTime transactionDate;
    private PaymentStatus status;
    private SubscriptionPlan plan;
    private String planDisplayName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paypalOrderId;
    private String paypalCaptureId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
