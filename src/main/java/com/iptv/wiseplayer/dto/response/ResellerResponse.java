package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResellerResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private AdminRole role;
    private boolean active;
    private LocalDateTime createdAt;
    private long totalUsers;
}
