package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.ResellerActivationRequestDto;
import com.iptv.wiseplayer.dto.request.ResellerLoginRequest;
import com.iptv.wiseplayer.dto.request.ResellerRegisterRequest;
import com.iptv.wiseplayer.dto.request.SubResellerCreateRequest;
import com.iptv.wiseplayer.dto.response.AdminAuthResponse;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;

import java.util.List;
import java.util.UUID;

public interface ResellerService {
    AdminAuthResponse login(ResellerLoginRequest request);

    AdminAuthResponse register(ResellerRegisterRequest request);

    ResellerDashboardResponse getDashboardOverview(UUID resellerId);

    DeviceRegistrationResponse createEndUser(UUID resellerId, DeviceRegistrationRequest request);

    org.springframework.data.domain.Page<Device> getResellerUsers(UUID resellerId,
            String search,
            com.iptv.wiseplayer.domain.enums.DeviceStatus status,
            org.springframework.data.domain.Pageable pageable);

    void disableUser(UUID resellerId, UUID deviceId);

    Admin createSubReseller(UUID resellerId, SubResellerCreateRequest request);

    org.springframework.data.domain.Page<Admin> getSubResellers(UUID resellerId,
            org.springframework.data.domain.Pageable pageable);

    void updateSubReseller(UUID resellerId, UUID subResellerId,
            com.iptv.wiseplayer.dto.request.SubResellerUpdateRequest request);

    void toggleSubResellerStatus(UUID resellerId, UUID subResellerId);

    ActivationRequest submitActivationRequest(UUID resellerId, ResellerActivationRequestDto request);

    org.springframework.data.domain.Page<com.iptv.wiseplayer.dto.response.ActivationRequestResponse> getResellerRequests(
            UUID resellerId, String status, org.springframework.data.domain.Pageable pageable);
}
