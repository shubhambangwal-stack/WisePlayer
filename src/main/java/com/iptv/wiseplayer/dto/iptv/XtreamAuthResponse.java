package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class XtreamAuthResponse {
    @JsonProperty("user_info")
    private XtreamUserInfo userInfo;
    @JsonProperty("server_info")
    private XtreamServerInfo serverInfo;

    public XtreamUserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(XtreamUserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public XtreamServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(XtreamServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }
}
