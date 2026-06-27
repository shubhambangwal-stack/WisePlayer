package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

import java.util.UUID;

public interface PlaylistService {
    PlaylistResponse saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request);

    PlaylistResponse saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request);

    PlaylistResponse savePublicM3uPlaylist(String deviceId, M3uPlaylistRequest request);

    java.util.List<PlaylistResponse> getPlaylists(UUID deviceId);

    java.util.List<PlaylistResponse> getPublicPlaylists(String deviceId);

    void deletePublicPlaylist(String deviceId, UUID playlistId);

    PlaylistResponse updatePublicM3uPlaylist(String deviceId, UUID playlistId, M3uPlaylistRequest request);
}

