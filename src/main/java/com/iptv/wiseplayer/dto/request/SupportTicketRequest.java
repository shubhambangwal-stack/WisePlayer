package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SupportTicketRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "MAC Address is required")
    private String macAddress;

    @NotBlank(message = "Inquiry type is required")
    private String inquiryType;

    @NotBlank(message = "Message detail is required")
    private String message;

    private MultipartFile attachment;
}
