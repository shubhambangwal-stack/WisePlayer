package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.CreditTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CreditTransactionResponse {
    private UUID id;
    private BigDecimal amount;
    private CreditTransactionType type;
    private String notes;
    private UUID relatedRequestId;
    private LocalDateTime createdAt;

    public CreditTransactionResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public CreditTransactionType getType() { return type; }
    public void setType(CreditTransactionType type) { this.type = type; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getRelatedRequestId() { return relatedRequestId; }
    public void setRelatedRequestId(UUID relatedRequestId) { this.relatedRequestId = relatedRequestId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
