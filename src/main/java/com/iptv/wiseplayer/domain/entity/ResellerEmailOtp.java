package com.iptv.wiseplayer.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reseller_email_otps")
public class ResellerEmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getAdminId() { return adminId; }
    public void setAdminId(UUID adminId) { this.adminId = adminId; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}