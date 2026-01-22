package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecureCredentialStore {

    private final PlaylistRepository playlistRepository;
    private final EncryptionUtil encryptionUtil;

    public SecureCredentialStore(PlaylistRepository playlistRepository, EncryptionUtil encryptionUtil) {
        this.playlistRepository = playlistRepository;
        this.encryptionUtil = encryptionUtil;
    }

    public Credentials getCredentials(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        return new Credentials(
                encryptionUtil.decrypt(playlist.getServerUrl()),
                encryptionUtil.decrypt(playlist.getUsername()),
                encryptionUtil.decrypt(playlist.getPassword()));
    }

    public record Credentials(String serverUrl, String username, String password) {
    }
}
