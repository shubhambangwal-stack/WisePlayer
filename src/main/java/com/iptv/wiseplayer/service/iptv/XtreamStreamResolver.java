package com.iptv.wiseplayer.service.iptv;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class XtreamStreamResolver {

    private final SecureCredentialStore credentialStore;
    private final XtreamAuthService authService;

    public enum StreamType {
        LIVE, VOD
    }

    public XtreamStreamResolver(SecureCredentialStore credentialStore, XtreamAuthService authService) {
        this.credentialStore = credentialStore;
        this.authService = authService;
    }

    public String resolveStreamUrl(UUID playlistId, int streamId, StreamType type) {
        // Enforce play permission by checking auth first
        authService.checkAuth(playlistId);

        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);

        // Format:
        // Live: /live/{username}/{password}/{stream_id}.ts
        // VOD: /movie/{username}/{password}/{stream_id}.{ext} (usually mp4/mkv, but
        // generic player often handles without ext or we default)
        // Actually, Xtream play url for VOD is usually /movie/user/pass/id.mp4

        String baseUrl = creds.serverUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String typePath = (type == StreamType.LIVE) ? "live" : "movie";
        String extension = (type == StreamType.LIVE) ? "ts" : "mp4"; // Defaulting VOD to mp4, though it might differ.

        return String.format("%s/%s/%s/%s/%d.%s",
                baseUrl,
                typePath,
                creds.username(),
                creds.password(),
                streamId,
                extension);
    }
}
