package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ResellerActivationRequestDto {
    @NotNull(message = "Device ID is required")
    private UUID deviceId;

    @NotBlank(message = "Plan name is required")
    private String planName;

    private Double amount;

    private String currency;

    private String status;
}
