package com.iptv.wiseplayer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubResellerResponse {
    private UUID id;
    private String username;
    private String fullName;
    private long activeUsers;
    private String status;
    private LocalDateTime joinedAt;
}
