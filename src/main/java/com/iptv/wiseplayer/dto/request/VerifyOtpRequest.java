package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {
    @NotBlank
    private String otp;

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}