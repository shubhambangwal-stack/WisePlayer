package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamUserInfo;
import com.iptv.wiseplayer.exception.AccountStatusException;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class XtreamAuthService {

    private static final Logger log = LoggerFactory.getLogger(XtreamAuthService.class);

    private final XtreamClient xtreamClient;
    private final SecureCredentialStore credentialStore;
    private final PlaylistRepository playlistRepository;

    public XtreamAuthService(XtreamClient xtreamClient, SecureCredentialStore credentialStore,
            PlaylistRepository playlistRepository) {
        this.xtreamClient = xtreamClient;
        this.credentialStore = credentialStore;
        this.playlistRepository = playlistRepository;
    }

    public XtreamAuthResponse checkAuth(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (playlist.getType() != PlaylistType.XTREAM) {
            throw new IllegalArgumentException("This operation is only available for XTREAM playlists.");
        }

        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);

        XtreamAuthResponse response = xtreamClient.authenticate(creds.serverUrl(), creds.username(), creds.password())
                .orElseThrow(() -> new RuntimeException("Authentication failed for Xtream Codes"));

        validateUserInfo(response.getUserInfo());

        return response;
    }

    private void validateUserInfo(XtreamUserInfo userInfo) {
        // Check status — guard against null/missing status field from provider
        String status = userInfo.getStatus();
        if (status == null || status.isBlank() || !"Active".equalsIgnoreCase(status)) {
            throw new AccountStatusException(status);
        }

        // Check expiry
        if (userInfo.getExpDate() != null && !userInfo.getExpDate().isEmpty()) {
            try {
                long expTimestamp = Long.parseLong(userInfo.getExpDate());
                if (expTimestamp > 0 && expTimestamp < Instant.now().getEpochSecond()) {
                    throw new RuntimeException("Account expired on " + Instant.ofEpochSecond(expTimestamp));
                }
            } catch (NumberFormatException e) {
                // Non-numeric exp_date — skip expiry check
            }
        }

        // Check connection limits — we intentionally do NOT throw here.
        // The upstream provider already enforces this at the stream (.ts) level.
        // Throwing here would lock users out of the app (browsing, EPG, settings)
        // even when they haven't started a stream yet, and would also incorrectly
        // block users during the brief stale-connection window after a stream ends.
        try {
            int active = Integer.parseInt(userInfo.getActiveCons());
            int max = Integer.parseInt(userInfo.getMaxConnections());
            if (max > 0 && active >= max) {
                log.warn("Upstream connection limit reached ({}/{}) for user '{}'. "
                        + "Stream playback will be rejected by the provider server.",
                        active, max, userInfo.getUsername());
            }
        } catch (NumberFormatException e) {
            // Non-numeric connection counts — skip limit check
        }
    }
}
