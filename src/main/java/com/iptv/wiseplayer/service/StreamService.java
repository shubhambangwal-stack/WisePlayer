package com.iptv.wiseplayer.service;

import java.util.UUID;

import java.util.concurrent.CompletableFuture;

/**
 * Service for authorizing and generating stream URLs.
 */
public interface StreamService {
    /**
     * Authorizes a stream play request and returns a short-lived URL.
     *
     * @param deviceId Device requesting play
     * @param streamId Stream ID (from provider)
     * @return Stream URL
     */
    String authorizeAndGetUrl(UUID deviceId, UUID playlistId, String streamId);

    /**
     * Generates a catchup/timeshift stream redirect URL asynchronously.
     *
     * @param deviceId Device requesting play
     * @param playlistId Playlist ID to lookup IPTV credentials
     * @param channelId IPTV provider's channel ID
     * @param timestamp Date-time requested in ISO-8601 format
     * @param duration Duration requested in minutes
     * @param extension Optional stream format extension (defaults to ts)
     * @return CompletableFuture resolving to the target redirect URL
     */
    CompletableFuture<String> getTimeshiftUrlAsync(UUID deviceId, UUID playlistId, String channelId, String timestamp, Integer duration, String extension);
}
