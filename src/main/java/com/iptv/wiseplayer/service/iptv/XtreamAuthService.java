package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamUserInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class XtreamAuthService {

    private final XtreamClient xtreamClient;
    private final SecureCredentialStore credentialStore;

    public XtreamAuthService(XtreamClient xtreamClient, SecureCredentialStore credentialStore) {
        this.xtreamClient = xtreamClient;
        this.credentialStore = credentialStore;
    }

    public XtreamAuthResponse checkAuth(UUID playlistId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);

        XtreamAuthResponse response = xtreamClient.authenticate(creds.serverUrl(), creds.username(), creds.password())
                .orElseThrow(() -> new RuntimeException("Authentication failed for Xtream Codes"));

        validateUserInfo(response.getUserInfo());

        return response;
    }

    private void validateUserInfo(XtreamUserInfo userInfo) {
        // Check status
        if (!"Active".equalsIgnoreCase(userInfo.getStatus())) {
            throw new RuntimeException("Account is " + userInfo.getStatus());
        }

        // Check expiry
        if (userInfo.getExpDate() != null && !userInfo.getExpDate().isEmpty()) {
            try {
                long expTimestamp = Long.parseLong(userInfo.getExpDate());
                if (expTimestamp > 0 && expTimestamp < Instant.now().getEpochSecond()) {
                    throw new RuntimeException("Account expired on " + Instant.ofEpochSecond(expTimestamp));
                }
            } catch (NumberFormatException e) {
                // Handle non-numeric exp_date if necessary
            }
        }

        // Check connection limits
        try {
            int active = Integer.parseInt(userInfo.getActiveCons());
            int max = Integer.parseInt(userInfo.getMaxConnections());
            if (max > 0 && active >= max) {
                throw new RuntimeException("Maximum connection limit reached (" + active + "/" + max + ")");
            }
        } catch (NumberFormatException e) {
            // Handle parsing errors
        }
    }
}
