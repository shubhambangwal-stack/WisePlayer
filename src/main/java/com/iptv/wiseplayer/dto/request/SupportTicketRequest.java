package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

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

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getInquiryType() { return inquiryType; }
    public void setInquiryType(String inquiryType) { this.inquiryType = inquiryType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public MultipartFile getAttachment() { return attachment; }
    public void setAttachment(MultipartFile attachment) { this.attachment = attachment; }
}
