package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getAuth() {
        return auth;
    }

    public void setAuth(int auth) {
        this.auth = auth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpDate() {
        return expDate;
    }

    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }

    public String getIsTrial() {
        return isTrial;
    }

    public void setIsTrial(String isTrial) {
        this.isTrial = isTrial;
    }

    public String getActiveCons() {
        return activeCons;
    }

    public void setActiveCons(String activeCons) {
        this.activeCons = activeCons;
    }

    public String getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(String maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
