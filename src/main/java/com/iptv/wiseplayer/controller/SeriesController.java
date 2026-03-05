package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/series")
@Tag(name = "Series", description = "Endpoints for browsing Series categories and content")
public class SeriesController {

    private final XtreamCatalogService catalogService;
    private final XtreamStreamResolver streamResolver;

    public SeriesController(XtreamCatalogService catalogService, XtreamStreamResolver streamResolver) {

        this.catalogService = catalogService;
        this.streamResolver = streamResolver;
    }

    @Operation(summary = "Handle Series Request", description = "Dispatches request based on parameters: categories or series list.")
    @GetMapping
    public ResponseEntity<?> handleRequest(
            @RequestParam UUID playlistId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Integer streamId){
        if (streamId != null) {
            String url = streamResolver.resolveStreamUrl(playlistId, streamId, XtreamStreamResolver.StreamType.VOD);
            return ResponseEntity.ok(Map.of("url", url));
        }

        // 1. Get Series List (if categoryId is present)
        if (categoryId != null) {
            List<XtreamSeries> series = catalogService.getSeries(playlistId, categoryId);
            return ResponseEntity.ok(series);
        }

        // 2. Default: Get Series Categories
        List<XtreamCategory> categories = catalogService.getSeriesCategories(playlistId);
        return ResponseEntity.ok(categories);
    }
}
