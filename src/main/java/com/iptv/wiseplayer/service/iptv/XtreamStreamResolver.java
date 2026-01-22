package com.iptv.wiseplayer.service.iptv;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class XtreamStreamResolver {

    private final SecureCredentialStore credentialStore;
    private final XtreamAuthService authService;

    public XtreamStreamResolver(SecureCredentialStore credentialStore, XtreamAuthService authService) {
        this.credentialStore = credentialStore;
        this.authService = authService;
    }

    public String resolveStreamUrl(UUID playlistId, int streamId) {
        // Enforce play permission by checking auth first
        authService.checkAuth(playlistId);

        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);

        // Format: /live/{username}/{password}/{stream_id}.ts
        String baseUrl = creds.serverUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return String.format("%s/live/%s/%s/%d.ts",
                baseUrl,
                creds.username(),
                creds.password(),
                streamId);
    }
}
