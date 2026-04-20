package com.iptv.wiseplayer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activation_requests")
public class ActivationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "reseller_id", nullable = false)
    private UUID resellerId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "plan_name", nullable = false, length = 50)
    private String planName;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "credits_used", precision = 10, scale = 2)
    private BigDecimal creditsUsed;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ActivationRequest() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getResellerId() { return resellerId; }
    public void setResellerId(UUID resellerId) { this.resellerId = resellerId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCreditsUsed() { return creditsUsed; }
    public void setCreditsUsed(BigDecimal creditsUsed) { this.creditsUsed = creditsUsed; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
