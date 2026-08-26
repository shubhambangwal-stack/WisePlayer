package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.response.GlobalSearchResponse;
import com.iptv.wiseplayer.service.iptv.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Global Search Controller.
 * Provides a single endpoint to search across all content types
 * (Live TV, VOD / Movies, Series) for a given Xtream playlist.
 *
 * Protected by DeviceSecurityFilter (requires X-Device-Token + X-Device-Fingerprint).
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Global Search", description = "Search across the full playlist — Live TV, Movies (VOD), and Series — in one call")
public class SearchController {

    private final GlobalSearchService globalSearchService;

    public SearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    /**
     * GET /api/search?playlistId=&q=batman&type=ALL&page=0&size=20
     *
     * @param playlistId UUID of the Xtream playlist to search in
     * @param q          Search query (minimum 1 character)
     * @param type       Content type filter — ALL (default), LIVE, VOD, SERIES
     * @param page       0-based page index (default 0)
     * @param size       Page size 1–100 (default 20)
     */
    @Operation(
        summary = "Global Playlist Search",
        description = "Searches across Live TV channels, VOD movies, and Series in a single request. "
            + "Results are scored by relevance (exact match > starts-with > contains) and paginated. "
            + "Use 'type' to narrow to a specific content category. "
            + "Requires an active device token."
    )
    @GetMapping
    public ResponseEntity<GlobalSearchResponse> search(
            @Parameter(description = "UUID of the Xtream playlist", required = true)
            @RequestParam("playlistId") UUID playlistId,

            @Parameter(description = "Search query string", required = true)
            @RequestParam("q") String q,

            @Parameter(description = "Content type filter: ALL, LIVE, VOD, SERIES (default: ALL)")
            @RequestParam(value = "type", defaultValue = "ALL") String type,

            @Parameter(description = "Page index (0-based, default: 0)")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Page size 1–100 (default: 20)")
            @RequestParam(value = "size", defaultValue = "20") int size) {

        GlobalSearchResponse response = globalSearchService.search(playlistId, q, type, page, size);
        return ResponseEntity.ok(response);
    }
}
