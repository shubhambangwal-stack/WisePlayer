package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.SuperAdmin;
import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.ActivationRequestResponse;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.ActivationRequestRepository;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminActivationRequestService {

    private final ActivationRequestRepository activationRequestRepository;
    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;
    private final DeviceRepository deviceRepository;
    private final AdminSubscriptionService adminSubscriptionService;
    private final com.iptv.wiseplayer.service.CreditService creditService;

    public Page<ActivationRequestResponse> getAllRequests(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return activationRequestRepository.findAllByStatus(status.toUpperCase(), pageable)
                    .map(this::convertToResponse);
        }
        return activationRequestRepository.findAll(pageable).map(this::convertToResponse);
    }

    public ActivationRequestResponse getRequestById(UUID id) {
        return activationRequestRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Activation request not found"));
    }

    @Transactional
    public void approveRequest(UUID requestId, String adminNotes) {
        ActivationRequest request = activationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activation request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("Only pending requests can be approved");
        }

        UUID adminId = getCurrentAdminId();

        // Trigger subscription activation
        SubscriptionActivationRequest activationDto = new SubscriptionActivationRequest();
        activationDto.setDeviceId(request.getDeviceId().toString());

        // Find corresponding SubscriptionPlan enum
        try {
            activationDto.setPlan(SubscriptionPlan.valueOf(request.getPlanName().toUpperCase()));
        } catch (IllegalArgumentException e) {
            // Default or handle error if dynamic plans are used (but manualActivate
            // currently uses enum)
            throw new BadRequestException("Invalid plan name in request: " + request.getPlanName());
        }

        adminSubscriptionService.manualActivate(activationDto);

        request.setStatus("APPROVED");
        request.setAdminNotes(adminNotes);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());

        activationRequestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(UUID requestId, String adminNotes) {
        ActivationRequest request = activationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activation request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("Only pending requests can be rejected");
        }

        UUID adminId = getCurrentAdminId();

        request.setStatus("REJECTED");
        request.setAdminNotes(adminNotes);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());

        activationRequestRepository.save(request);

        // Refund credits if it was a reseller request
        if (request.getResellerId() != null) {
            creditService.refundCredits(request.getResellerId(), request.getId());
        }
    }

    private ActivationRequestResponse convertToResponse(ActivationRequest request) {
        ActivationRequestResponse response = new ActivationRequestResponse();
        response.setId(request.getId());
        response.setResellerId(request.getResellerId());

        adminRepository.findById(request.getResellerId())
                .map(Admin::getUsername)
                .ifPresent(response::setResellerUsername);

        response.setDeviceId(request.getDeviceId());
        deviceRepository.findByDeviceId(request.getDeviceId())
                .map(d -> d.getDeviceStatus().name())
                .ifPresent(response::setDeviceStatus);

        response.setPlanName(request.getPlanName());
        response.setAmount(request.getAmount());
        response.setCurrency(request.getCurrency());
        response.setStatus(request.getStatus());
        response.setAdminNotes(request.getAdminNotes());
        response.setReviewedBy(request.getReviewedBy());
        response.setReviewedAt(request.getReviewedAt());
        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());
        return response;
    }

    private UUID getCurrentAdminId() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return superAdminRepository.findByUsername(username)
                .map(SuperAdmin::getId)
                .or(() -> adminRepository.findByUsername(username).map(Admin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));
    }
}
