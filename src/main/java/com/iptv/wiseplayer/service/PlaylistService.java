package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistService {
    PlaylistResponse saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request);

    PlaylistResponse saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request);

    PlaylistResponse savePublicM3uPlaylist(String deviceId, M3uPlaylistRequest request);

    List<PlaylistResponse> getPlaylists(UUID deviceId);

    List<PlaylistResponse> getPublicPlaylists(String deviceId);

    void deletePublicPlaylist(String deviceId, UUID playlistId);

    PlaylistResponse updatePublicM3uPlaylist(String deviceId, UUID playlistId, M3uPlaylistRequest request);

    // ── Pin / Unpin ──────────────────────────────────────────────────────────

    /** Pin a playlist for an authenticated (token-based) device. Automatically unpins any previously pinned playlist. */
    PlaylistResponse pinPlaylist(UUID deviceId, UUID playlistId);

    /** Unpin a playlist for an authenticated (token-based) device. */
    PlaylistResponse unpinPlaylist(UUID deviceId, UUID playlistId);

    /** Return the currently pinned playlist for an authenticated device, if any. */
    Optional<PlaylistResponse> getPinnedPlaylist(UUID deviceId);

    /** Pin a playlist for a public / website device (resolved by MAC / fingerprint / UUID string). */
    PlaylistResponse pinPublicPlaylist(String deviceId, UUID playlistId);

    /** Unpin a playlist for a public / website device. */
    PlaylistResponse unpinPublicPlaylist(String deviceId, UUID playlistId);

    /** Return the pinned playlist for a public / website device, if any. */
    Optional<PlaylistResponse> getPublicPinnedPlaylist(String deviceId);
}
