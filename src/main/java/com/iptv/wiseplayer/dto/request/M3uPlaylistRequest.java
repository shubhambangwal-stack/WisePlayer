package com.iptv.wiseplayer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for saving an M3U playlist.
 * Enforces strict URL and PIN validation.
 */
@Schema(description = "Request payload for saving an M3U playlist")
public class M3uPlaylistRequest {

    @Schema(description = "Display name for the playlist", example = "My Sports Pack", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Playlist name is required")
    @Size(min = 2, max = 100, message = "Playlist name must be between 2 and 100 characters")
    private String name;

    @Schema(
        description = "Full HTTP/HTTPS URL to the M3U or M3U8 playlist file. "
            + "Must end with .m3u or .m3u8 (query parameters are allowed).",
        example = "http://provider.com:8080/get.php?username=user&password=pass&type=m3u_plus&output=ts",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "M3U URL is required")
    @Pattern(
        regexp = "^(https?)://[^\\s/$.?#].[^\\s]*(\\.m3u8?|type=m3u|type=m3u_plus|output=ts|get\\.php)(\\?[^\\s]*)?$",
        message = "M3U URL must be a valid HTTP/HTTPS URL pointing to an M3U or M3U8 playlist"
    )
    private String m3uUrl;

    @Schema(description = "Optional 4-digit numeric PIN to lock the playlist", example = "1234")
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 numeric digits")
    private String pin;

    public M3uPlaylistRequest() {
    }

    public M3uPlaylistRequest(String name, String m3uUrl) {
        this.name = name;
        this.m3uUrl = m3uUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getM3uUrl() {
        return m3uUrl;
    }

    public void setM3uUrl(String m3uUrl) {
        this.m3uUrl = m3uUrl;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
