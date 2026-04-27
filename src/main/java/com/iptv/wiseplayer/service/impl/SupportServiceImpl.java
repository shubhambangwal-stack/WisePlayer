package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.SupportTicket;
import com.iptv.wiseplayer.domain.enums.TicketStatus;
import com.iptv.wiseplayer.dto.request.SupportTicketRequest;
import com.iptv.wiseplayer.dto.response.SupportTicketResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.SupportTicketRepository;
import com.iptv.wiseplayer.service.FileStorageService;
import com.iptv.wiseplayer.service.SupportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupportServiceImpl implements SupportService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SupportServiceImpl.class);

    private final SupportTicketRepository supportTicketRepository;
    private final FileStorageService fileStorageService;

    public SupportServiceImpl(SupportTicketRepository supportTicketRepository,
                              FileStorageService fileStorageService) {
        this.supportTicketRepository = supportTicketRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public SupportTicketResponse createTicket(SupportTicketRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setFirstName(request.getFirstName());
        ticket.setLastName(request.getLastName());
        ticket.setEmail(request.getEmail());
        ticket.setMacAddress(request.getMacAddress());
        ticket.setInquiryType(request.getInquiryType());
        ticket.setMessage(request.getMessage());

        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            String attachmentUrl = fileStorageService.storeFile(request.getAttachment());
            ticket.setAttachmentUrl(attachmentUrl);
        }

        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Created support ticket with ID: {}", savedTicket.getId());
        return convertToResponse(savedTicket);
    }

    @Override
    public Page<SupportTicketResponse> getAllTickets(TicketStatus status, Pageable pageable) {
        if (status != null) {
            return supportTicketRepository.findByStatus(status, pageable)
                    .map(this::convertToResponse);
        }
        return supportTicketRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    @Override
    public SupportTicketResponse getTicketById(UUID id) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found: " + id));
        return convertToResponse(ticket);
    }

    @Override
    @Transactional
    public SupportTicketResponse updateTicketStatus(UUID id, TicketStatus status) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found: " + id));

        ticket.setStatus(status);
        SupportTicket updatedTicket = supportTicketRepository.save(ticket);
        log.info("Updated support ticket {} status to {}", id, status);
        return convertToResponse(updatedTicket);
    }

    private SupportTicketResponse convertToResponse(SupportTicket ticket) {
        SupportTicketResponse response = new SupportTicketResponse();
        response.setId(ticket.getId());
        response.setFirstName(ticket.getFirstName());
        response.setLastName(ticket.getLastName());
        response.setEmail(ticket.getEmail());
        response.setMacAddress(ticket.getMacAddress());
        response.setInquiryType(ticket.getInquiryType());
        response.setMessage(ticket.getMessage());
        response.setAttachmentUrl(ticket.getAttachmentUrl());
        response.setStatus(ticket.getStatus());
        response.setCreatedAt(ticket.getCreatedAt());
        return response;
    }
}
