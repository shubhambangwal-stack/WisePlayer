package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AdminPaymentResponse {
    private UUID paymentId;
    private UUID deviceId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String planName;
    private String paypalOrderId;
    private LocalDateTime createdAt;
}
