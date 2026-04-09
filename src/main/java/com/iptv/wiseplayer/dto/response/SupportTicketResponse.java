package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.TicketStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SupportTicketResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String macAddress;
    private String inquiryType;
    private String message;
    private String attachmentUrl;
    private TicketStatus status;
    private LocalDateTime createdAt;
}
