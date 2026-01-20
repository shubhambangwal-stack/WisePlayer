package com.iptv.wiseplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/**
 * Service for browsing live TV content.
 */
public interface LiveTvService {
    /**
     * Get live categories for the device's playlist.
     */
    JsonNode getCategories(UUID deviceId);

    /**
     * Get live channels for a specific category.
     */
    JsonNode getChannels(UUID deviceId, String categoryId);
}
