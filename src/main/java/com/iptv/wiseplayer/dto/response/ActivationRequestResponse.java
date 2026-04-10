package com.iptv.wiseplayer.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ActivationRequestResponse {
    private UUID id;
    private UUID resellerId;
    private UUID deviceId;
    private String planName;
    private Double amount;
    private String currency;
    private String status;
    private BigDecimal creditsUsed;
    private String adminNotes;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Reseller Details
    private String resellerUsername;

    // Device Details
    private String deviceModel;
    private String platform;
    private String deviceStatus;
}
