package com.iptv.wiseplayer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for setting or verifying a playlist-level 4-digit PIN.
 */
@Schema(description = "Request payload for setting or verifying a 4-digit playlist PIN")
public class PlaylistPinRequest {

    @Schema(
        description = "A 4-digit numeric PIN to protect this playlist",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1234"
    )
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 numeric digits")
    private String pin;

    @Schema(
        description = "Confirm the 4-digit numeric PIN",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "1234"
    )
    private String confirmPin;

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getConfirmPin() { return confirmPin; }
    public void setConfirmPin(String confirmPin) { this.confirmPin = confirmPin; }
}
