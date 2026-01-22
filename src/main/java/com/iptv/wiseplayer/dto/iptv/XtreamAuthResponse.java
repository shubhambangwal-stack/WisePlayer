package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class XtreamAuthResponse {
    @JsonProperty("user_info")
    private XtreamUserInfo userInfo;
    @JsonProperty("server_info")
    private XtreamServerInfo serverInfo;
}
