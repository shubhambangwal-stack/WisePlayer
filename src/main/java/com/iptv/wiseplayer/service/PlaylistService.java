package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

public interface PlaylistService {
    void saveXtreamPlaylist(XtreamPlaylistRequest request);

    void saveM3uPlaylist(M3uPlaylistRequest request);

    PlaylistResponse getPlaylist(String deviceIdFingerprint);
}
