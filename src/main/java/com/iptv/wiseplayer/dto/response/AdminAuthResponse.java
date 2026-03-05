package com.iptv.wiseplayer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminAuthResponse {
    private boolean success;
    private String token;
    private String username;
    private String fullName;
}
