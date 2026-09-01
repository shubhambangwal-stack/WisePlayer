package com.iptv.wiseplayer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for saving an Xtream Codes playlist.
 * Enforces strict URL, credential, and PIN validation.
 */
@Schema(description = "Request payload for saving an Xtream Codes playlist")
public class XtreamPlaylistRequest {

    @Schema(description = "Display name for the playlist", example = "My IPTV Pack", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Playlist name is required")
    @Size(min = 2, max = 100, message = "Playlist name must be between 2 and 100 characters")
    private String name;

    @Schema(
        description = "Full HTTP/HTTPS base URL of the Xtream Codes server. Must not contain a path — only scheme + host + optional port.",
        example = "http://xtream-provider.com:8080",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Server URL is required")
    @Pattern(
        regexp = "^(https?)://[^\\s/$.?#][^\\s]*(:\\d{1,5})?$",
        message = "Server URL must be a valid HTTP/HTTPS URL (e.g. http://provider.com:8080)"
    )
    private String serverUrl;

    @Schema(description = "Xtream Codes account username", example = "my_user", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 100, message = "Username must be between 1 and 100 characters")
    @Pattern(regexp = "^[^\\s]+$", message = "Username must not contain spaces")
    private String username;

    @Schema(description = "Xtream Codes account password", example = "my_pass", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 100, message = "Password must be between 1 and 100 characters")
    @Pattern(regexp = "^[^\\s]+$", message = "Password must not contain spaces")
    private String password;

    @Schema(description = "Optional 4-digit numeric PIN to lock the playlist", example = "1234")
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 numeric digits")
    private String pin;

    public XtreamPlaylistRequest() {
    }

    public XtreamPlaylistRequest(String name, String serverUrl, String username, String password) {
        this.name = name;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
