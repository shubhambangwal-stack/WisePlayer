package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class XtreamUserInfo {
    private String username;
    private String password;
    private String message;
    @JsonProperty("auth")
    private int auth;
    private String status;
    @JsonProperty("exp_date")
    private String expDate;
    @JsonProperty("is_trial")
    private String isTrial;
    @JsonProperty("active_cons")
    private String activeCons;
    @JsonProperty("max_connections")
    private String maxConnections;
    @JsonProperty("created_at")
    private String createdAt;
}
