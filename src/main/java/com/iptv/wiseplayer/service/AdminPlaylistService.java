package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.AssignPlaylistRequest;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.UpdatePlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

import java.util.List;
import java.util.UUID;

public interface AdminPlaylistService {

    List<PlaylistResponse> getAllPlaylists();

    PlaylistResponse getPlaylistById(UUID playlistId);

    PlaylistResponse createXtreamPlaylist(UUID adminId, XtreamPlaylistRequest request);

    PlaylistResponse createM3uPlaylist(UUID adminId, M3uPlaylistRequest request);

    PlaylistResponse updatePlaylist(UUID playlistId, UpdatePlaylistRequest request);

    PlaylistResponse assignPlaylist(UUID playlistId, AssignPlaylistRequest request);

    PlaylistResponse unassignPlaylist(UUID playlistId);

    PlaylistResponse togglePin(UUID playlistId);

    void deletePlaylist(UUID playlistId);
}
