package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubResellerUpdateRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String password;
}
