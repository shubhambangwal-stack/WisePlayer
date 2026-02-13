package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XtreamServerInfo {
    @JsonProperty("url")
    private String url;
    @JsonProperty("port")
    private String port;
    @JsonProperty("https_port")
    private String httpsPort;
    @JsonProperty("server_protocol")
    private String serverProtocol;
    @JsonProperty("rtmp_port")
    private String rtmpPort;
    @JsonProperty("timezone")
    private String timezone;
    @JsonProperty("timestamp_now")
    private int timestampNow;
    @JsonProperty("time_now")
    private String timeNow;
    @JsonProperty("process")
    private boolean process;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(String httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getServerProtocol() {
        return serverProtocol;
    }

    public void setServerProtocol(String serverProtocol) {
        this.serverProtocol = serverProtocol;
    }

    public String getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(String rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getTimestampNow() {
        return timestampNow;
    }

    public void setTimestampNow(int timestampNow) {
        this.timestampNow = timestampNow;
    }

    public String getTimeNow() {
        return timeNow;
    }

    public void setTimeNow(String timeNow) {
        this.timeNow = timeNow;
    }

    public boolean isProcess() {
        return process;
    }

    public void setProcess(boolean process) {
        this.process = process;
    }
}
