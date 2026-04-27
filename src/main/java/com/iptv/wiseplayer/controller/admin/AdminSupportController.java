package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.enums.TicketStatus;
import com.iptv.wiseplayer.dto.response.SupportTicketResponse;
import com.iptv.wiseplayer.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/support/tickets")
@Tag(name = "Admin Support Management", description = "Endpoints for admins to manage support tickets")
public class AdminSupportController {

    private final SupportService supportService;

    public AdminSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping
    @Operation(summary = "List All Support Tickets", description = "Retrieves a paginated list of support tickets")
    public ResponseEntity<Page<SupportTicketResponse>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(supportService.getAllTickets(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Ticket Details", description = "Retrieves detailed information for a specific support ticket")
    public ResponseEntity<SupportTicketResponse> getTicketById(@PathVariable UUID id) {
        return ResponseEntity.ok(supportService.getTicketById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Ticket Status", description = "Updates the status of a specific support ticket")
    public ResponseEntity<SupportTicketResponse> updateTicketStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        TicketStatus status = TicketStatus.valueOf(payload.get("status").toUpperCase());
        return ResponseEntity.ok(supportService.updateTicketStatus(id, status));
    }
}
