package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.WatchProgressRequest;
import com.iptv.wiseplayer.dto.response.WatchProgressResponse;
import com.iptv.wiseplayer.service.WatchProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller that handles the client-side progress reporting for the
 * Catchup / Resume feature.
 *
 * <h3>Client integration guide</h3>
 * <ul>
 *   <li><b>While playing:</b> call {@code POST /api/progress?playlistId=...} every
 *       10–15 seconds with the current position + total duration.</li>
 *   <li><b>On pause / app exit:</b> call the same endpoint one final time
 *       so the exact position is persisted immediately.</li>
 *   <li><b>Fully watched (≥ 95 %):</b> the server returns {@code fully_watched: true}
 *       and {@code resume_position_seconds: 0}. The client should start from the
 *       beginning on the next play.</li>
 * </ul>
 *
 * <p>The frontend does <b>NOT</b> need a separate {@code GET /api/progress} call —
 * progress is automatically injected into the VOD stream list and Series info
 * responses by {@link com.iptv.wiseplayer.service.CatchupEnrichmentService}.</p>
 */
@RestController
@RequestMapping("/api/progress")
@Tag(name = "Watch Progress (Catchup)", description = "Endpoint for saving playback position. Progress is returned automatically inside VOD/Series responses.")
public class WatchProgressController {

    private final WatchProgressService watchProgressService;

    public WatchProgressController(WatchProgressService watchProgressService) {
        this.watchProgressService = watchProgressService;
    }

    /**
     * Saves (or updates) the current playback position for a VOD movie or
     * Series episode.
     *
     * <p>The client should call this endpoint periodically (every ~10 s) and
     * on any pause or close event. When the watched percentage reaches or
     * exceeds 95 %, the server clears the saved progress and returns
     * {@code fully_watched: true}.</p>
     *
     * @param playlistId the playlist the user is watching from
     * @param request    the progress payload
     * @return the persisted progress state
     */
    @Operation(
        summary = "Save watch progress",
        description = "Records the current playback position. Call every ~10 s and on pause/exit. " +
                      "Returns fully_watched=true when ≥ 95% is watched, with resume_position_seconds=0."
    )
    @PostMapping
    public ResponseEntity<WatchProgressResponse> saveProgress(
            @RequestParam UUID playlistId,
            @Valid @RequestBody WatchProgressRequest request) {

        WatchProgressResponse response = watchProgressService.saveProgress(playlistId, request);
        return ResponseEntity.ok(response);
    }
}
