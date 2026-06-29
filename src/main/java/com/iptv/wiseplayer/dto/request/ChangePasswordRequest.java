package com.iptv.wiseplayer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for changing an administrator's password.
 */
@Schema(description = "Request payload for changing an admin password")
public class ChangePasswordRequest {

    @Schema(description = "The current password of the administrator", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(description = "The new password, must be at least 8 characters", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Schema(description = "Confirmation of the new password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
