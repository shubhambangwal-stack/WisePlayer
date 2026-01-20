package com.iptv.wiseplayer.service;

import java.util.UUID;

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
    String authorizeAndGetUrl(UUID deviceId, String streamId);
}
