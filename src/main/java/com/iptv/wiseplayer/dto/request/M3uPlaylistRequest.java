package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;

public class M3uPlaylistRequest {

    @NotBlank(message = "Playlist name is required")
    private String name;

    @NotBlank(message = "M3U URL is required")
    private String m3uUrl;

    private String pin;

    public M3uPlaylistRequest() {
    }

    public M3uPlaylistRequest(String name, String m3uUrl) {
        this.name = name;
        this.m3uUrl = m3uUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getM3uUrl() {
        return m3uUrl;
    }

    public void setM3uUrl(String m3uUrl) {
        this.m3uUrl = m3uUrl;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
