package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.WatchProgressRequest;
import com.iptv.wiseplayer.dto.response.WatchProgressResponse;
import com.iptv.wiseplayer.service.WatchProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Catchup / Resume feature.
 *
 * <h3>Client integration guide</h3>
 * <ul>
 * <li><b>While playing:</b> call {@code POST /api/progress} every 10–15 seconds
 * with the current position + total duration.</li>
 * <li><b>On pause / app exit:</b> call one final time to persist the exact
 * position.</li>
 * <li><b>No playlistId needed:</b> the device is identified automatically from
 * the
 * authenticated JWT (X-Device-Token header). The client does NOT need to supply
 * any device or playlist identifier — the server resolves it from the
 * token.</li>
 * <li><b>Fully watched (≥ 95 %):</b> the server returns
 * {@code fully_watched: true}
 * and {@code resume_position_seconds: 0}. The next play starts from the
 * beginning.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/progress")
@Tag(name = "Watch Progress (Catchup)", description = "Saves playback position per device. Progress is embedded automatically in VOD/Series responses.")
public class WatchProgressController {

    private final WatchProgressService watchProgressService;

    public WatchProgressController(WatchProgressService watchProgressService) {
        this.watchProgressService = watchProgressService;
    }

    /**
     * Saves (or updates) the current playback position for a VOD movie or Series
     * episode.
     *
     * <p>
     * The device identity is resolved automatically from the JWT token
     * (X-Device-Token
     * header). No playlistId or deviceId is needed from the client — this makes the
     * tracking 100 % accurate: even if 1000 devices share the same playlist, each
     * device
     * gets its own independent watch position.
     * </p>
     *
     * @param request the progress payload
     * @return the persisted progress state
     */
    @Operation(summary = "Save watch progress", description = "Records the current playback position. Call every ~10 s and on pause/exit. "
            +
            "Device is identified from JWT — no playlistId needed. " +
            "Returns fully_watched=true when ≥ 95% watched, with resume_position_seconds=0.")
    @PostMapping
    public ResponseEntity<WatchProgressResponse> saveProgress(
            @Valid @RequestBody WatchProgressRequest request) {

        WatchProgressResponse response = watchProgressService.saveProgress(request);
        return ResponseEntity.ok(response);
    }
}
