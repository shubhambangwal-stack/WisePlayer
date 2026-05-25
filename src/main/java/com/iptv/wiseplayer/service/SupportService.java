package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.enums.TicketStatus;
import com.iptv.wiseplayer.dto.request.SupportTicketRequest;
import com.iptv.wiseplayer.dto.response.SupportTicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SupportService {
    SupportTicketResponse createTicket(SupportTicketRequest request);

    Page<SupportTicketResponse> getAllTickets(TicketStatus status, String macAddress, String email, Pageable pageable);

    SupportTicketResponse getTicketById(UUID id);

    SupportTicketResponse updateTicketStatus(UUID id, TicketStatus status);
}
