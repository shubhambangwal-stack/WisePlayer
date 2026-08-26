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

    List<PlaylistResponse> getPlaylists(UUID deviceId, String pin);

    List<PlaylistResponse> getPublicPlaylists(String deviceId);

    /**
     * Retrieves public playlists after verifying the device PIN.
     * If the device has no PIN set, access is granted freely (backward-compatible).
     */
    List<PlaylistResponse> getPublicPlaylistsWithPin(String deviceId, String pin);

    /**
     * Sets (or replaces) the 4-digit public access PIN for the authenticated device.
     */
    void setDevicePin(UUID deviceId, String pin);

    /**
     * Removes the public access PIN from the authenticated device.
     */
    void removeDevicePin(UUID deviceId);

    /** Legacy delete without PIN (delegates to deletePublicPlaylistWithPin with null). */
    void deletePublicPlaylist(String deviceId, UUID playlistId);

    PlaylistResponse updatePublicM3uPlaylist(String deviceId, UUID playlistId, M3uPlaylistRequest request);

    // ── Playlist-level PIN management ─────────────────────────────────────────

    /** Sets or replaces the 4-digit PIN on a specific playlist owned by the device. */
    PlaylistResponse setPlaylistPin(UUID deviceId, UUID playlistId, String pin);

    /** Removes the PIN from a specific playlist owned by the device. */
    PlaylistResponse removePlaylistPin(UUID deviceId, UUID playlistId);

    /**
     * Verifies the provided PIN against the stored hash for a playlist.
     * Returns true if PIN matches, or if no PIN is set and pin is null/"0000".
     */
    boolean verifyPlaylistPin(UUID deviceId, UUID playlistId, String pin);

    /** Sets or replaces the 4-digit PIN on a specific public playlist. */
    PlaylistResponse setPublicPlaylistPin(String deviceId, UUID playlistId, String pin);

    /** Removes the PIN from a specific public playlist. */
    PlaylistResponse removePublicPlaylistPin(String deviceId, UUID playlistId);

    /** Verifies the PIN for a public playlist. */
    boolean verifyPublicPlaylistPin(String deviceId, UUID playlistId, String pin);

    /**
     * Deletes a public playlist after verifying its PIN.
     * If no PIN is set on the playlist, deletion proceeds freely.
     * If a PIN is set, the supplied pin must match.
     */
    void deletePublicPlaylistWithPin(String deviceId, UUID playlistId, String pin);

    // ── Playlist Pin/Unpin (favourite star) ───────────────────────────────────

    /** Pin a playlist for an authenticated device (by UUID). */
    PlaylistResponse pinPlaylist(UUID deviceId, UUID playlistId);

    /** Unpin a playlist for an authenticated device (by UUID). */
    PlaylistResponse unpinPlaylist(UUID deviceId, UUID playlistId);

    /** Get the currently pinned playlist for an authenticated device. */
    Optional<PlaylistResponse> getPinnedPlaylist(UUID deviceId);

    /** Pin a playlist via the public (MAC/fingerprint) device identifier. */
    PlaylistResponse pinPublicPlaylist(String deviceId, UUID playlistId);

    /** Unpin a playlist via the public (MAC/fingerprint) device identifier. */
    PlaylistResponse unpinPublicPlaylist(String deviceId, UUID playlistId);

    /** Get the currently pinned playlist via the public device identifier. */
    Optional<PlaylistResponse> getPublicPinnedPlaylist(String deviceId);
}
