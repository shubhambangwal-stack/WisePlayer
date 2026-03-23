package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.response.ResellerResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminResellerService {

    private final AdminRepository adminRepository;
    private final DeviceRepository deviceRepository;

    public Page<ResellerResponse> getAllResellers(Pageable pageable) {
        List<AdminRole> roles = Arrays.asList(AdminRole.RESELLER, AdminRole.SUB_RESELLER);
        return adminRepository.findAllByRoleIn(roles, pageable)
                .map(this::convertToResponse);
    }

    public ResellerResponse getResellerById(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));
        
        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new ResourceNotFoundException("Admin is not a reseller");
        }
        
        return convertToResponse(admin);
    }

    @Transactional
    public void toggleResellerStatus(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));
        admin.setActive(!admin.isActive());
        adminRepository.save(admin);
    }

    private ResellerResponse convertToResponse(Admin admin) {
        ResellerResponse response = new ResellerResponse();
        response.setId(admin.getId());
        response.setUsername(admin.getUsername());
        response.setFullName(admin.getFullName());
        response.setEmail(admin.getEmail());
        response.setRole(admin.getRole());
        response.setActive(admin.isActive());
        response.setCreatedAt(admin.getCreatedAt());
        response.setTotalUsers(deviceRepository.countByResellerId(admin.getId()));
        return response;
    }
}
