package com.iptv.wiseplayer.dto.request;

import com.iptv.wiseplayer.domain.enums.AdminRole;
import lombok.Data;

@Data
public class UpdateResellerRequest {
    private String fullName;
    private String email;
    private AdminRole role;
    private String password;
}
