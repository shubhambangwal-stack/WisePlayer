package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.SupportTicketRequest;
import com.iptv.wiseplayer.dto.response.SupportTicketResponse;
import com.iptv.wiseplayer.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/support")
@Tag(name = "Public Support", description = "Endpoints for public users to submit support tickets")
public class PublicSupportController {

    private final SupportService supportService;

    public PublicSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping(value = "/ticket", consumes = { "multipart/form-data" })
    @Operation(summary = "Submit a Support Ticket", description = "Submits a new support ticket with optional attachment")
    public ResponseEntity<SupportTicketResponse> submitTicket(@Valid @ModelAttribute SupportTicketRequest request) {
        return ResponseEntity.ok(supportService.createTicket(request));
    }
}
