package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentHistoryResponse {
    private UUID paymentId;
    private LocalDateTime transactionDate;
    private String planName;
    private String planDisplayName;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;

    public PaymentHistoryResponse() {}

    public PaymentHistoryResponse(UUID paymentId, LocalDateTime transactionDate, String planName, 
                                  String planDisplayName, BigDecimal amount, String currency, 
                                  PaymentStatus status, String paymentMethod) {
        this.paymentId = paymentId;
        this.transactionDate = transactionDate;
        this.planName = planName;
        this.planDisplayName = planDisplayName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
    }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getPlanDisplayName() { return planDisplayName; }
    public void setPlanDisplayName(String planDisplayName) { this.planDisplayName = planDisplayName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
