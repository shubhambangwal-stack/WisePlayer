package com.iptv.wiseplayer.dto.request;

import com.iptv.wiseplayer.domain.enums.PlaylistType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing playlist.
 * All fields are optional; only non-null values will be applied.
 * URL fields are validated when present.
 */
@Schema(description = "Request payload for partially updating a playlist (only send fields you want to change)")
public class UpdatePlaylistRequest {

    @Schema(description = "New display name", example = "My Updated Sports Pack")
    @Size(min = 2, max = 100, message = "Playlist name must be between 2 and 100 characters")
    private String name;

    // ── Xtream-only fields ───────────────────────────────────────────────────

    @Schema(
        description = "New Xtream Codes server base URL (HTTP/HTTPS, no path). Only applies to XTREAM playlists.",
        example = "http://new-provider.com:8080"
    )
    @Pattern(
        regexp = "^(https?)://[^\\s/$.?#][^\\s]*(:\\d{1,5})?$",
        message = "Server URL must be a valid HTTP/HTTPS base URL (e.g. http://provider.com:8080)"
    )
    private String serverUrl;

    @Schema(description = "New Xtream Codes username. Must not contain spaces.")
    @Size(min = 1, max = 100, message = "Username must be between 1 and 100 characters")
    @Pattern(regexp = "^[^\\s]+$", message = "Username must not contain spaces")
    private String username;

    @Schema(description = "New Xtream Codes password. Must not contain spaces.")
    @Size(min = 1, max = 100, message = "Password must be between 1 and 100 characters")
    @Pattern(regexp = "^[^\\s]+$", message = "Password must not contain spaces")
    private String password;

    // ── M3U-only field ───────────────────────────────────────────────────────

    @Schema(
        description = "New M3U/M3U8 playlist URL. Must point to a valid M3U resource. Only applies to M3U playlists.",
        example = "http://provider.com:8080/get.php?username=user&password=pass&type=m3u_plus"
    )
    @Pattern(
        regexp = "^(https?)://[^\\s/$.?#].[^\\s]*(\\.m3u8?|type=m3u|type=m3u_plus|output=ts|get\\.php)(\\?[^\\s]*)?$",
        message = "M3U URL must be a valid HTTP/HTTPS URL pointing to an M3U or M3U8 playlist"
    )
    private String m3uUrl;

    public UpdatePlaylistRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getM3uUrl() {
        return m3uUrl;
    }

    public void setM3uUrl(String m3uUrl) {
        this.m3uUrl = m3uUrl;
    }
}
