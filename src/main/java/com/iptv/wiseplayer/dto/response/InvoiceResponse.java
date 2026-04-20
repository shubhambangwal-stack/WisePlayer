package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class InvoiceResponse {
    private String invoiceNumber;
    private UUID paymentId;
    private UUID deviceId;
    private LocalDateTime transactionDate;
    private PaymentStatus status;
    private String planName;
    private String planDisplayName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paypalOrderId;
    private String paypalCaptureId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InvoiceResponse() {}

    public InvoiceResponse(String invoiceNumber, UUID paymentId, UUID deviceId, LocalDateTime transactionDate, 
                           PaymentStatus status, String planName, String planDisplayName, BigDecimal amount, 
                           String currency, String paymentMethod, String paypalOrderId, String paypalCaptureId, 
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.invoiceNumber = invoiceNumber;
        this.paymentId = paymentId;
        this.deviceId = deviceId;
        this.transactionDate = transactionDate;
        this.status = status;
        this.planName = planName;
        this.planDisplayName = planDisplayName;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.paypalOrderId = paypalOrderId;
        this.paypalCaptureId = paypalCaptureId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getPlanDisplayName() { return planDisplayName; }
    public void setPlanDisplayName(String planDisplayName) { this.planDisplayName = planDisplayName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaypalOrderId() { return paypalOrderId; }
    public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }

    public String getPaypalCaptureId() { return paypalCaptureId; }
    public void setPaypalCaptureId(String paypalCaptureId) { this.paypalCaptureId = paypalCaptureId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
