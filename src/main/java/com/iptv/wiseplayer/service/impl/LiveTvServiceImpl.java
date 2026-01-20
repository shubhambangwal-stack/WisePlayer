package com.iptv.wiseplayer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.exception.PlaylistNotFoundException;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.LiveTvService;
import com.iptv.wiseplayer.service.iptv.XtreamClient;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LiveTvServiceImpl implements LiveTvService {

    private final PlaylistRepository playlistRepository;
    private final XtreamClient xtreamClient;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    public LiveTvServiceImpl(PlaylistRepository playlistRepository,
            XtreamClient xtreamClient,
            EncryptionUtil encryptionUtil,
            ObjectMapper objectMapper) {
        this.playlistRepository = playlistRepository;
        this.xtreamClient = xtreamClient;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode getCategories(UUID deviceId) {
        Playlist playlist = getPlaylist(deviceId);

        if (playlist.getType() == PlaylistType.XTREAM) {
            return xtreamClient.getLiveCategories(
                    encryptionUtil.decrypt(playlist.getServerUrl()),
                    encryptionUtil.decrypt(playlist.getUsername()),
                    encryptionUtil.decrypt(playlist.getPassword()));
        }

        // TODO: Support M3U
        return objectMapper.createArrayNode();
    }

    @Override
    public JsonNode getChannels(UUID deviceId, String categoryId) {
        Playlist playlist = getPlaylist(deviceId);

        if (playlist.getType() == PlaylistType.XTREAM) {
            return xtreamClient.getLiveStreams(
                    encryptionUtil.decrypt(playlist.getServerUrl()),
                    encryptionUtil.decrypt(playlist.getUsername()),
                    encryptionUtil.decrypt(playlist.getPassword()),
                    categoryId);
        }

        return objectMapper.createArrayNode();
    }

    private Playlist getPlaylist(UUID deviceId) {
        return playlistRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new PlaylistNotFoundException("No playlist found for your device"));
    }
}
