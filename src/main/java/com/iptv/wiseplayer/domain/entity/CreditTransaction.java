package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.CreditTransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_transactions", indexes = {
        @Index(name = "idx_credit_transaction_admin", columnList = "admin_id")
})
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CreditTransactionType type;

    @Column(name = "related_request_id")
    private UUID relatedRequestId; // ID of ActivationRequest if type is DEDUCTION or REFUND

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CreditTransaction() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAdminId() { return adminId; }
    public void setAdminId(UUID adminId) { this.adminId = adminId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public CreditTransactionType getType() { return type; }
    public void setType(CreditTransactionType type) { this.type = type; }

    public UUID getRelatedRequestId() { return relatedRequestId; }
    public void setRelatedRequestId(UUID relatedRequestId) { this.relatedRequestId = relatedRequestId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
