package com.iptv.wiseplayer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for setting or updating a device's 4-digit public access PIN.
 */
@Schema(description = "Request payload for setting a 4-digit public playlist PIN")
public class SetDevicePinRequest {

    @Schema(
        description = "A 4-digit numeric PIN to protect public playlist access",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1234"
    )
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 numeric digits")
    private String pin;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
