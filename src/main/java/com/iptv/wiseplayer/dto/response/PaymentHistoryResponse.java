package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
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
    private String planName;
    private String planDisplayName;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
}
