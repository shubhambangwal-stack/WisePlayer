package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ResellerActivationRequestDto {
    @NotNull(message = "Device ID is required")
    private UUID deviceId;

    @NotBlank(message = "Plan name is required")
    private String planName;

    private Double amount;
    private String currency;
    private String status;

    public ResellerActivationRequestDto() {}

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
}
