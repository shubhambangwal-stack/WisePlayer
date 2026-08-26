package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.response.GlobalSearchResponse;
import com.iptv.wiseplayer.dto.response.GlobalSearchResult;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Global Search Service.
 * Searches across ALL content types (Live TV, VOD, Series) for a given playlist
 * in parallel for maximum performance, then applies fuzzy/prefix matching and pagination.
 */
@Service
public class GlobalSearchService {

    private static final Logger log = LoggerFactory.getLogger(GlobalSearchService.class);

    private final XtreamCatalogService catalogService;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public GlobalSearchService(XtreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Perform a global search across a playlist.
     *
     * @param playlistId  UUID of the playlist (must be Xtream type)
     * @param query       The search query (e.g. "Batman")
     * @param type        Filter: "ALL", "LIVE", "VOD", "SERIES" (case-insensitive)
     * @param page        0-indexed page number
     * @param size        Page size (default 20, max 100)
     * @return paginated search results sorted by relevance score
     */
    public GlobalSearchResponse search(UUID playlistId, String query, String type, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            return new GlobalSearchResponse(query, type, page, size, 0, List.of());
        }

        // Clamp size
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        String normalizedType = (type == null || type.isBlank()) ? "ALL" : type.trim().toUpperCase();
        String normalizedQuery = query.trim().toLowerCase();
        String[] queryTokens = normalizedQuery.split("\\s+");

        List<GlobalSearchResult> allResults = new ArrayList<>();

        try {
            // ── Parallel fetch ──────────────────────────────────────────────
            CompletableFuture<List<GlobalSearchResult>> liveFuture = CompletableFuture.supplyAsync(
                    () -> fetchLive(playlistId, queryTokens, normalizedType), executor);

            CompletableFuture<List<GlobalSearchResult>> vodFuture = CompletableFuture.supplyAsync(
                    () -> fetchVod(playlistId, queryTokens, normalizedType), executor);

            CompletableFuture<List<GlobalSearchResult>> seriesFuture = CompletableFuture.supplyAsync(
                    () -> fetchSeries(playlistId, queryTokens, normalizedType), executor);

            CompletableFuture.allOf(liveFuture, vodFuture, seriesFuture).join();

            allResults.addAll(liveFuture.get());
            allResults.addAll(vodFuture.get());
            allResults.addAll(seriesFuture.get());

        } catch (Exception e) {
            log.error("Global search error for playlistId={}, query='{}': {}", playlistId, query, e.getMessage(), e);
            // Return empty results gracefully
            return new GlobalSearchResponse(query, normalizedType, page, size, 0, List.of());
        }

        // ── Sort by relevance score (exact > starts-with > contains) ────────
        allResults.sort((a, b) -> {
            int scoreA = relevanceScore(a.getName(), normalizedQuery, queryTokens);
            int scoreB = relevanceScore(b.getName(), normalizedQuery, queryTokens);
            return Integer.compare(scoreB, scoreA); // descending
        });

        long total = allResults.size();
        int fromIndex = page * size;
        int toIndex = (int) Math.min(fromIndex + size, total);

        List<GlobalSearchResult> pageResults = fromIndex >= total
                ? List.of()
                : allResults.subList(fromIndex, toIndex);

        return new GlobalSearchResponse(query, normalizedType, page, size, total, pageResults);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private List<GlobalSearchResult> fetchLive(UUID playlistId, String[] tokens, String type) {
        if (!"ALL".equals(type) && !"LIVE".equals(type)) return List.of();
        try {
            List<XtreamLiveStream> all = catalogService.getLiveStreams(playlistId, null);
            return all.stream()
                    .filter(s -> s.getName() != null && matchesTokens(s.getName().toLowerCase(), tokens))
                    .map(s -> {
                        GlobalSearchResult r = new GlobalSearchResult();
                        r.setContentType(GlobalSearchResult.ContentType.LIVE);
                        r.setStreamId(s.getStreamId());
                        r.setName(s.getName());
                        r.setStreamIcon(s.getStreamIcon());
                        r.setCategoryId(s.getCategoryId());
                        return r;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch live streams for global search: {}", e.getMessage());
            return List.of();
        }
    }

    private List<GlobalSearchResult> fetchVod(UUID playlistId, String[] tokens, String type) {
        if (!"ALL".equals(type) && !"VOD".equals(type)) return List.of();
        try {
            List<XtreamVodStream> all = catalogService.getVodStreams(playlistId, null);
            return all.stream()
                    .filter(s -> s.getName() != null && matchesTokens(s.getName().toLowerCase(), tokens))
                    .map(s -> {
                        GlobalSearchResult r = new GlobalSearchResult();
                        r.setContentType(GlobalSearchResult.ContentType.VOD);
                        r.setStreamId(s.getStreamId());
                        r.setName(s.getName());
                        r.setStreamIcon(s.getStreamIcon());
                        r.setCategoryId(s.getCategoryId());
                        r.setRating(s.getRating());
                        r.setContainerExtension(s.getContainerExtension());
                        return r;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch VOD streams for global search: {}", e.getMessage());
            return List.of();
        }
    }

    private List<GlobalSearchResult> fetchSeries(UUID playlistId, String[] tokens, String type) {
        if (!"ALL".equals(type) && !"SERIES".equals(type)) return List.of();
        try {
            List<XtreamSeries> all = catalogService.getSeries(playlistId, null);
            return all.stream()
                    .filter(s -> s.getName() != null && matchesTokens(s.getName().toLowerCase(), tokens))
                    .map(s -> {
                        GlobalSearchResult r = new GlobalSearchResult();
                        r.setContentType(GlobalSearchResult.ContentType.SERIES);
                        r.setStreamId(s.getSeriesId());
                        r.setName(s.getName());
                        r.setStreamIcon(s.getCover());
                        r.setCategoryId(s.getCategoryId());
                        r.setRating(s.getRating());
                        r.setGenre(s.getGenre());
                        r.setPlot(s.getPlot());
                        r.setCast(s.getCast());
                        r.setDirector(s.getDirector());
                        r.setReleaseDate(s.getReleaseDate());
                        return r;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch series for global search: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns true if ALL query tokens appear somewhere in the name (AND logic).
     * This is the primary filter — relevance score handles ordering.
     */
    private boolean matchesTokens(String nameLower, String[] tokens) {
        for (String token : tokens) {
            if (!nameLower.contains(token)) return false;
        }
        return true;
    }

    /**
     * Relevance scoring (higher = better match):
     * 100 — exact match
     *  80 — name starts with the full query
     *  60 — any word in name starts with full query
     *  40 — all tokens appear but name doesn't start with query
     *  20 — partial contains only
     */
    private int relevanceScore(String name, String fullQuery, String[] tokens) {
        if (name == null) return 0;
        String nameLower = name.toLowerCase();

        if (nameLower.equals(fullQuery)) return 100;
        if (nameLower.startsWith(fullQuery)) return 80;

        // Check if any word in name starts with query
        for (String word : nameLower.split("\\s+")) {
            if (word.startsWith(fullQuery)) return 60;
        }

        // All tokens match somewhere
        boolean allMatch = true;
        for (String token : tokens) {
            if (!nameLower.contains(token)) { allMatch = false; break; }
        }
        if (allMatch) return 40;

        return 20;
    }
}
