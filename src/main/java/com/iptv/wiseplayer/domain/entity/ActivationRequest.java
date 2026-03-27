package com.iptv.wiseplayer.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activation_requests")
@Getter
@Setter
@NoArgsConstructor
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
    private java.math.BigDecimal creditsUsed;

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
}
