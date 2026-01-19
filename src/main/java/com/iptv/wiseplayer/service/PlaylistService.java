package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

import java.util.UUID;

public interface PlaylistService {
    void saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request);

    void saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request);

    PlaylistResponse getPlaylist(UUID deviceId);
}
