package com.iptv.wiseplayer.domain.entity;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "payments")
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_session_id", columnList = "stripe_session_id"),
        @Index(name = "idx_payment_event_id", columnList = "stripe_event_id", unique = true),
        @Index(name = "idx_paypal_order_id", columnList = "paypal_order_id")
})
public class Payments {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "reseller_id")
    private UUID resellerId;

    @Column(name = "credit_amount")
    private Integer creditAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "stripe_session_id", length = 255)
    private String stripeSessionId;

    @Column(name = "stripe_event_id", length = 255, unique = true)
    private String stripeEventId;

    @Column(name = "paypal_order_id", length = 255)
    private String paypalOrderId;

    @Column(name = "paypal_capture_id", length = 255)
    private String paypalCaptureId;

    @Column(name = "paypal_fee", precision = 10, scale = 2)
    private BigDecimal paypalFee;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "plan", nullable = false, length = 100)
    private String planName;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "EUR";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Payments() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public void setStripeEventId(String stripeEventId) {
        this.stripeEventId = stripeEventId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPaypalOrderId() {
        return paypalOrderId;
    }

    public void setPaypalOrderId(String paypalOrderId) {
        this.paypalOrderId = paypalOrderId;
    }

    public String getPaypalCaptureId() {
        return paypalCaptureId;
    }

    public void setPaypalCaptureId(String paypalCaptureId) {
        this.paypalCaptureId = paypalCaptureId;
    }

    public BigDecimal getPaypalFee() {
        return paypalFee;
    }

    public void setPaypalFee(BigDecimal paypalFee) {
        this.paypalFee = paypalFee;
    }

    public UUID getResellerId() {
        return resellerId;
    }

    public void setResellerId(UUID resellerId) {
        this.resellerId = resellerId;
    }

    public Integer getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(Integer creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
