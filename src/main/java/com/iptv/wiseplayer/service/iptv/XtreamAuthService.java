package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamUserInfo;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class XtreamAuthService {

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
