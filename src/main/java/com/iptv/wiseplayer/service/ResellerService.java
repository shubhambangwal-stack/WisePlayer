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

    List<Device> getResellerUsers(UUID resellerId);

    void disableUser(UUID resellerId, UUID deviceId);

    Admin createSubReseller(UUID resellerId, SubResellerCreateRequest request);

    List<Admin> getSubResellers(UUID resellerId);

    ActivationRequest submitActivationRequest(UUID resellerId, ResellerActivationRequestDto request);

    List<ActivationRequest> getResellerRequests(UUID resellerId);
}
