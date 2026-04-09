package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.CreditTransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreditTransactionResponse {
    private UUID id;
    private BigDecimal amount;
    private CreditTransactionType type;
    private String notes;
    private UUID relatedRequestId;
    private LocalDateTime createdAt;
}
