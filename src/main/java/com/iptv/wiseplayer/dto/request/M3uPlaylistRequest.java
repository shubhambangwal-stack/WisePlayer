package com.iptv.wiseplayer.dto.request;

public class M3uPlaylistRequest {

    private String name;
    private String m3uUrl;

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
}
