package com.iptv.wiseplayer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminResponse(
    UUID id,
    String email,
    String username,
    String fullName,
    String role,
    boolean active,
    LocalDateTime createdAt,
    BigDecimal credits,
    boolean canCreate,
    boolean canRead,
    boolean canUpdate,
    boolean canDelete
) {}
