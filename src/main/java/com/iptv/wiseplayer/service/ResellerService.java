package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ResellerService {
    Map<String, Object> login(String username, String password);

    Map<String, Object> register(String username, String password, String fullName);

    ResellerDashboardResponse getDashboardOverview(UUID resellerId);

    DeviceRegistrationResponse createEndUser(UUID resellerId, DeviceRegistrationRequest request);

    List<Device> getResellerUsers(UUID resellerId);

    void disableUser(UUID resellerId, UUID deviceId);

    Admin createSubReseller(UUID resellerId, String username, String password, String fullName);

    List<Admin> getSubResellers(UUID resellerId);

    ActivationRequest submitActivationRequest(UUID resellerId, UUID deviceId, String planName, String status);

    List<ActivationRequest> getResellerRequests(UUID resellerId);
}
