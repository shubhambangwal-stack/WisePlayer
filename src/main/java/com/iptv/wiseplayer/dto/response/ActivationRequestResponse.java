package com.iptv.wiseplayer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivationRequestResponse {
    private UUID id;
    private UUID resellerId;
    private String resellerUsername;
    private UUID deviceId;
    private String deviceStatus;
    private String planName;
    private Double amount;
    private String currency;
    private String status;
    private String adminNotes;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
