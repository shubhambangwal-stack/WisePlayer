package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iptv.wiseplayer.domain.enums.CatchUpMethod;

import java.time.Instant;

/**
 * Playlist-level catch-up / archive availability, as detected from the
 * provider data (M3U attributes or Xtream Codes API).
 *
 * <p>{@code supported} is only ever {@code true} when the underlying provider
 * actually exposes archive data and a playable catch-up URL can be built.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatchUpStatus {

    private boolean supported;
    private CatchUpMethod method;
    private Integer days;
    private String source;
    private String provider;
    private Instant checkedAt;

    public CatchUpStatus() {
    }

    public CatchUpStatus(boolean supported, CatchUpMethod method, Integer days, String source,
            String provider, Instant checkedAt) {
        this.supported = supported;
        this.method = method;
        this.days = days;
        this.source = source;
        this.provider = provider;
        this.checkedAt = checkedAt;
    }

    public static CatchUpStatus unsupported(String provider) {
        return new CatchUpStatus(false, CatchUpMethod.NONE, null, null, provider, Instant.now());
    }

    public boolean isSupported() {
        return supported;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public CatchUpMethod getMethod() {
        return method;
    }

    public void setMethod(CatchUpMethod method) {
        this.method = method;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}