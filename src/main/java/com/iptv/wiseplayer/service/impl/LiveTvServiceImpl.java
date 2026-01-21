package com.iptv.wiseplayer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.exception.PlaylistNotFoundException;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.LiveTvService;
import com.iptv.wiseplayer.service.iptv.M3uService;
import com.iptv.wiseplayer.service.iptv.XtreamClient;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LiveTvServiceImpl implements LiveTvService {

    private final PlaylistRepository playlistRepository;
    private final XtreamClient xtreamClient;
    private final M3uService m3uService;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    public LiveTvServiceImpl(PlaylistRepository playlistRepository,
            XtreamClient xtreamClient,
            M3uService m3uService,
            EncryptionUtil encryptionUtil,
            ObjectMapper objectMapper) {
        this.playlistRepository = playlistRepository;
        this.xtreamClient = xtreamClient;
        this.m3uService = m3uService;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode getCategories(UUID deviceId, UUID playlistId) {
        Playlist playlist = getPlaylist(deviceId, playlistId);

        if (playlist.getType() == PlaylistType.XTREAM) {
            return xtreamClient.getLiveCategories(
                    encryptionUtil.decrypt(playlist.getServerUrl()),
                    encryptionUtil.decrypt(playlist.getUsername()),
                    encryptionUtil.decrypt(playlist.getPassword()));
        } else if (playlist.getType() == PlaylistType.M3U) {
            return m3uService.getCategories(encryptionUtil.decrypt(playlist.getM3uUrl()));
        }

        return objectMapper.createArrayNode();
    }

    @Override
    public JsonNode getChannels(UUID deviceId, UUID playlistId, String categoryId) {
        Playlist playlist = getPlaylist(deviceId, playlistId);

        if (playlist.getType() == PlaylistType.XTREAM) {
            return xtreamClient.getLiveStreams(
                    encryptionUtil.decrypt(playlist.getServerUrl()),
                    encryptionUtil.decrypt(playlist.getUsername()),
                    encryptionUtil.decrypt(playlist.getPassword()),
                    categoryId);
        } else if (playlist.getType() == PlaylistType.M3U) {
            return m3uService.getChannels(encryptionUtil.decrypt(playlist.getM3uUrl()), categoryId);
        }

        return objectMapper.createArrayNode();
    }

    private Playlist getPlaylist(UUID deviceId, UUID playlistId) {
        return playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getId().equals(playlistId))
                .findFirst()
                .orElseThrow(
                        () -> new PlaylistNotFoundException("Playlist not found or does not belong to your device"));
    }
}
