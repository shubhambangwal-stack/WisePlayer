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
    private final com.iptv.wiseplayer.repository.CreditTransactionRepository creditTransactionRepository;

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

    @Transactional
    public void updateReseller(UUID id, com.iptv.wiseplayer.dto.request.UpdateResellerRequest request,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        // Ensure we are only updating resellers or sub-resellers
        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new ResourceNotFoundException("Admin is not a reseller");
        }

        if (request.getFullName() != null)
            admin.setFullName(request.getFullName());
        if (request.getEmail() != null)
            admin.setEmail(request.getEmail());
        if (request.getRole() != null)
            admin.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getCredits() != null) {
            java.math.BigDecimal oldCredits = admin.getCredits() == null ? java.math.BigDecimal.ZERO
                    : admin.getCredits();
            java.math.BigDecimal newCredits = request.getCredits();

            if (oldCredits.compareTo(newCredits) != 0) {
                admin.setCredits(newCredits);

                com.iptv.wiseplayer.domain.entity.CreditTransaction tx = new com.iptv.wiseplayer.domain.entity.CreditTransaction();
                tx.setAdminId(admin.getId());
                tx.setAmount(newCredits.subtract(oldCredits));
                tx.setType(com.iptv.wiseplayer.domain.enums.CreditTransactionType.MANUAL_ADJUSTMENT);
                tx.setNotes("Manual adjustment by admin. Old balance: " + oldCredits + ", New balance: " + newCredits);
                creditTransactionRepository.save(tx);
            }
        }

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
        response.setCredits(admin.getCredits());
        return response;
    }
}
