package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.dto.response.ResellerResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iptv.wiseplayer.security.CrudOperation;
import com.iptv.wiseplayer.security.CrudPermissionGuard;
import com.iptv.wiseplayer.security.RequiresCrud;
import com.iptv.wiseplayer.repository.AdminAuditLogRepository;
import com.iptv.wiseplayer.dto.response.CrudPermissionViewResponse;
import com.iptv.wiseplayer.dto.response.RolePermissionViewDTO;
import com.iptv.wiseplayer.dto.response.AdminPermissionViewDTO;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class AdminResellerService {

    private final AdminRepository adminRepository;
    private final DeviceRepository deviceRepository;
    private final com.iptv.wiseplayer.repository.CreditTransactionRepository creditTransactionRepository;
    private final com.iptv.wiseplayer.repository.ActivationRequestRepository activationRequestRepository;
    private final com.iptv.wiseplayer.repository.PaymentRepository paymentRepository;
    private final com.iptv.wiseplayer.repository.SubscriptionRepository subscriptionRepository;
    private final com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository;
    private final com.iptv.wiseplayer.repository.RolePermissionRepository rolePermissionRepository;
    private final CrudPermissionGuard crudPermissionGuard;
    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminResellerService(AdminRepository adminRepository,
                                DeviceRepository deviceRepository,
                                com.iptv.wiseplayer.repository.CreditTransactionRepository creditTransactionRepository,
                                com.iptv.wiseplayer.repository.ActivationRequestRepository activationRequestRepository,
                                com.iptv.wiseplayer.repository.PaymentRepository paymentRepository,
                                com.iptv.wiseplayer.repository.SubscriptionRepository subscriptionRepository,
                                com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository,
                                com.iptv.wiseplayer.repository.RolePermissionRepository rolePermissionRepository,
                                CrudPermissionGuard crudPermissionGuard,
                                AdminAuditLogRepository adminAuditLogRepository) {
        this.adminRepository = adminRepository;
        this.deviceRepository = deviceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.activationRequestRepository = activationRequestRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.superAdminRepository = superAdminRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.crudPermissionGuard = crudPermissionGuard;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    public Page<ResellerResponse> getAllResellers(
            String username,
            String fullName,
            String email,
            Pageable pageable) {
        List<AdminRole> roles = Arrays.asList(AdminRole.RESELLER, AdminRole.SUB_RESELLER);
        return adminRepository.searchResellers(roles, username, fullName, email, pageable)
                .map(this::convertToResponse);
    }

    public com.iptv.wiseplayer.dto.response.ResellerStatsResponse getResellerStats(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        long totalUsers = deviceRepository.countByResellerId(id);
        long activeSubs = subscriptionRepository.countActiveByResellerId(id);

        // Growth calculation (this month vs last month)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        java.time.LocalDateTime startLastMonth = startThisMonth.minusMonths(1);

        long thisMonthActivations = activationRequestRepository.findAllByResellerIdAndStatusAndCreatedAtBetween(
                id, "APPROVED", startThisMonth, now).size();
        long lastMonthActivations = activationRequestRepository.findAllByResellerIdAndStatusAndCreatedAtBetween(
                id, "APPROVED", startLastMonth, startThisMonth).size();

        double growth = lastMonthActivations == 0 ? (thisMonthActivations > 0 ? 100.0 : 0.0)
                : ((double) (thisMonthActivations - lastMonthActivations) / lastMonthActivations) * 100.0;

        // Peak Activation Time
        List<com.iptv.wiseplayer.domain.entity.ActivationRequest> allActivations = activationRequestRepository.findAllByResellerId(id);
        String peakTime = calculatePeakActivationTime(allActivations);

        return com.iptv.wiseplayer.dto.response.ResellerStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeSubscriptions(activeSubs)
                .growthPercentage(Math.round(growth * 10.0) / 10.0)
                .remainingCredits(admin.getCredits())
                .partnerLevel(admin.getPartnerLevel())
                .peakActivationTime(peakTime)
                .build();
    }

    public List<com.iptv.wiseplayer.dto.response.ResellerAnalyticsResponse> getResellerAnalytics(UUID id, String period, java.time.LocalDate startDate) {
        java.time.LocalDateTime start = startDate.atStartOfDay();
        java.time.LocalDateTime end;
        
        if ("WEEK".equalsIgnoreCase(period)) {
            end = start.plusDays(7);
        } else if ("MONTH".equalsIgnoreCase(period)) {
            end = start.plusMonths(1);
        } else {
            end = start.plusDays(1);
        }

        List<com.iptv.wiseplayer.domain.entity.ActivationRequest> activations = activationRequestRepository
                .findAllByResellerIdAndStatusAndCreatedAtBetween(id, "APPROVED", start, end);
        
        // Grouping logic for analytics
        java.util.Map<java.time.LocalDate, Long> activationMap = activations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate(),
                        java.util.stream.Collectors.counting()
                ));

        List<com.iptv.wiseplayer.dto.response.ResellerAnalyticsResponse> results = new java.util.ArrayList<>();
        java.time.LocalDate current = startDate;
        java.time.LocalDate stopDate = end.toLocalDate();

        while (current.isBefore(stopDate) || current.equals(stopDate)) {
            java.time.LocalDateTime dayStart = current.atStartOfDay();
            java.time.LocalDateTime dayEnd = current.atTime(23, 59, 59);

            java.math.BigDecimal revenue = paymentRepository.sumTotalRevenueByResellerIdAndStatusAndCreatedAtBetween(
                    id, com.iptv.wiseplayer.domain.enums.PaymentStatus.SUCCESS, dayStart, dayEnd);

            results.add(com.iptv.wiseplayer.dto.response.ResellerAnalyticsResponse.builder()
                    .date(current)
                    .activations(activationMap.getOrDefault(current, 0L))
                    .revenue(revenue != null ? revenue : java.math.BigDecimal.ZERO)
                    .build());
            
            current = current.plusDays(1);
        }

        return results;
    }

    public Page<com.iptv.wiseplayer.dto.response.SubResellerResponse> getSubResellers(UUID id, Pageable pageable) {
        return adminRepository.findAllByParentId(id, pageable)
                .map(sub -> com.iptv.wiseplayer.dto.response.SubResellerResponse.builder()
                        .id(sub.getId())
                        .username(sub.getUsername())
                        .fullName(sub.getFullName())
                        .activeUsers(deviceRepository.countByResellerId(sub.getId()))
                        .status(sub.isActive() ? "ACTIVE" : "INACTIVE")
                        .joinedAt(sub.getCreatedAt())
                        .build());
    }

    private String calculatePeakActivationTime(List<com.iptv.wiseplayer.domain.entity.ActivationRequest> requests) {
        if (requests.isEmpty()) return "N/A";
        
        int[] hours = new int[24];
        for (com.iptv.wiseplayer.domain.entity.ActivationRequest req : requests) {
            hours[req.getCreatedAt().getHour()]++;
        }

        int maxWindow = 0;
        int peakHour = 0;
        for (int i = 0; i < 22; i++) {
            int window = hours[i] + hours[i+1] + hours[i+2];
            if (window > maxWindow) {
                maxWindow = window;
                peakHour = i;
            }
        }

        return String.format("%02d:00 - %02d:00", peakHour, (peakHour + 3) % 24);
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
    @RequiresCrud(CrudOperation.UPDATE)
    public void updateReseller(UUID id, com.iptv.wiseplayer.dto.request.UpdateResellerRequest request,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        // Ensure we are only updating resellers or sub-resellers
        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new ResourceNotFoundException("Admin is not a reseller");
        }

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

        java.util.Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository.findByUsername(currentUsername);
        if (!superAdminOpt.isPresent()) {
            Admin currentAdmin = adminRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));

            if (!currentAdmin.getRole().canManage(admin.getRole())) {
                throw new com.iptv.wiseplayer.exception.AccessDeniedException("You cannot modify an equal or higher-ranked role.");
            }

            crudPermissionGuard.checkEscalation(currentAdmin, request);
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

        if (request.getCanCreate() != null) admin.setCanCreate(request.getCanCreate());
        if (request.getCanRead() != null) admin.setCanRead(request.getCanRead());
        if (request.getCanUpdate() != null) admin.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) admin.setCanDelete(request.getCanDelete());

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

    @Transactional
    @RequiresCrud(CrudOperation.DELETE)
    public void deleteReseller(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found"));

        if (admin.getRole() != AdminRole.RESELLER && admin.getRole() != AdminRole.SUB_RESELLER) {
            throw new ResourceNotFoundException("Admin is not a reseller");
        }

        adminRepository.delete(admin);
    }

    @Transactional
    @RequiresCrud(CrudOperation.UPDATE)
    public void updateRolePermissions(AdminRole targetRole, com.iptv.wiseplayer.dto.request.UpdateResellerRequest request) {
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        
        java.util.Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository.findByUsername(currentUsername);
        if (superAdminOpt.isPresent()) {
            // 1. Update all existing admins of this role
            adminRepository.updatePermissionsByRole(targetRole, request.getCanCreate(), request.getCanRead(), request.getCanUpdate(), request.getCanDelete());
            // 2. Sync the role_permissions defaults table so future registrations are consistent
            syncRolePermissionsTable(targetRole, request.getCanCreate(), request.getCanRead(), request.getCanUpdate(), request.getCanDelete());
            return;
        }

        Admin currentAdmin = adminRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));

        if (!currentAdmin.getRole().canManage(targetRole)) {
            throw new com.iptv.wiseplayer.exception.AccessDeniedException("You cannot bulk modify an equal or higher-ranked role.");
        }

        crudPermissionGuard.checkEscalation(currentAdmin, request);

        // 1. Update all existing admins of this role
        adminRepository.updatePermissionsByRole(targetRole, request.getCanCreate(), request.getCanRead(), request.getCanUpdate(), request.getCanDelete());
        // 2. Sync the role_permissions defaults table so future registrations are consistent
        syncRolePermissionsTable(targetRole, request.getCanCreate(), request.getCanRead(), request.getCanUpdate(), request.getCanDelete());

        // 3. Log the audit action
        com.iptv.wiseplayer.domain.entity.AdminAuditLog log = new com.iptv.wiseplayer.domain.entity.AdminAuditLog(
                currentAdmin.getId(), "ROLE_" + targetRole.name(), "CRUD_UPDATE", null
        );
        adminAuditLogRepository.save(log);
    }

    /**
     * Update CRUD permission flags for a SINGLE admin identified by UUID.
     * Only the caller's higher-ranked role can modify; cannot grant flags they don't have.
     */
    @Transactional
    @RequiresCrud(CrudOperation.UPDATE)
    public void updateAdminPermissionsById(UUID targetAdminId,
                                           com.iptv.wiseplayer.dto.request.UpdateRolePermissionRequest request) {
        Admin target = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + targetAdminId));

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

        // SuperAdmin can update anyone
        java.util.Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt =
                superAdminRepository.findByUsername(currentUsername);

        if (superAdminOpt.isEmpty()) {
            Admin currentAdmin = adminRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));

            // Must be higher-ranked
            if (!currentAdmin.getRole().canManage(target.getRole())) {
                throw new com.iptv.wiseplayer.exception.AccessDeniedException(
                        "You cannot modify an equal or higher-ranked admin.");
            }

            // Cannot grant permissions the caller doesn't have
            crudPermissionGuard.checkEscalation(currentAdmin, request);
        }

        if (request.getCanCreate() != null) target.setCanCreate(request.getCanCreate());
        if (request.getCanRead()   != null) target.setCanRead(request.getCanRead());
        if (request.getCanUpdate() != null) target.setCanUpdate(request.getCanUpdate());
        if (request.getCanDelete() != null) target.setCanDelete(request.getCanDelete());

        adminRepository.save(target);

        if (superAdminOpt.isEmpty()) {
            Admin currentAdmin = adminRepository.findByUsername(currentUsername).orElse(null);
            if (currentAdmin != null) {
                com.iptv.wiseplayer.domain.entity.AdminAuditLog log = new com.iptv.wiseplayer.domain.entity.AdminAuditLog(
                        currentAdmin.getId(), target.getEmail(), "CRUD_UPDATE", null
                );
                adminAuditLogRepository.save(log);
            }
        }
    }

    /**
     * Internal helper: upsert the role_permissions row so defaults stay in sync
     * whenever a bulk permission change is made.
     */
    private void syncRolePermissionsTable(AdminRole role,
                                          Boolean canCreate, Boolean canRead,
                                          Boolean canUpdate, Boolean canDelete) {
        com.iptv.wiseplayer.domain.entity.RolePermission rp =
                rolePermissionRepository.findByRole(role)
                        .orElse(com.iptv.wiseplayer.domain.entity.RolePermission.allTrue(role));
        if (canCreate != null) rp.setCanCreate(canCreate);
        if (canRead   != null) rp.setCanRead(canRead);
        if (canUpdate != null) rp.setCanUpdate(canUpdate);
        if (canDelete != null) rp.setCanDelete(canDelete);
        rolePermissionRepository.save(rp);
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

    @Transactional(readOnly = true)
    public CrudPermissionViewResponse getCrudPermissionsView() {
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        
        AdminRole currentRole;
        UUID currentAdminId = null;
        
        java.util.Optional<com.iptv.wiseplayer.domain.entity.SuperAdmin> superAdminOpt = superAdminRepository.findByUsername(currentUsername);
        if (superAdminOpt.isPresent()) {
            currentRole = AdminRole.SUPER_ADMIN;
        } else {
            Admin currentAdmin = adminRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Current admin not found"));
            currentRole = currentAdmin.getRole();
            currentAdminId = currentAdmin.getId();
        }

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        final UUID performerId = currentAdminId;

        // 1. Role Defaults for lower roles
        List<RolePermissionViewDTO> rolePermissions = Arrays.stream(AdminRole.values())
                .filter(role -> currentRole.canManage(role))
                .map(role -> {
                    com.iptv.wiseplayer.domain.entity.RolePermission rp = rolePermissionRepository.findByRole(role)
                            .orElse(com.iptv.wiseplayer.domain.entity.RolePermission.allTrue(role));
                    
                    RolePermissionViewDTO dto = new RolePermissionViewDTO();
                    dto.setRole(role);
                    dto.setCanCreate(rp.isCanCreate());
                    dto.setCanRead(rp.isCanRead());
                    dto.setCanUpdate(rp.isCanUpdate());
                    dto.setCanDelete(rp.isCanDelete());
                    dto.setUpdatedAt(rp.getUpdatedAt());
                    
                    if (performerId != null) {
                        dto.setRecentlyChangedByMe(adminAuditLogRepository.hasRecentAction(performerId, "ROLE_" + role.name(), "CRUD_UPDATE", sevenDaysAgo));
                    }
                    return dto;
                }).collect(Collectors.toList());

        // 2. Individual Admin Permissions for lower roles
        List<Admin> lowerAdmins;
        if (currentRole == AdminRole.SUPER_ADMIN) {
            lowerAdmins = adminRepository.findAll();
        } else {
            lowerAdmins = adminRepository.findAll().stream()
                .filter(a -> currentRole.canManage(a.getRole()) && (a.getParentId() != null && a.getParentId().equals(performerId)))
                .collect(Collectors.toList());
        }

        List<AdminPermissionViewDTO> individualPermissions = lowerAdmins.stream().map(admin -> {
            AdminPermissionViewDTO dto = new AdminPermissionViewDTO();
            dto.setAdminId(admin.getId());
            dto.setEmail(admin.getEmail());
            dto.setUsername(admin.getUsername());
            dto.setRole(admin.getRole());
            dto.setCanCreate(admin.isCanCreate());
            dto.setCanRead(admin.isCanRead());
            dto.setCanUpdate(admin.isCanUpdate());
            dto.setCanDelete(admin.isCanDelete());
            dto.setUpdatedAt(admin.getUpdatedAt());
            
            if (performerId != null) {
                dto.setRecentlyChangedByMe(adminAuditLogRepository.hasRecentAction(performerId, admin.getEmail(), "CRUD_UPDATE", sevenDaysAgo));
            }
            return dto;
        }).collect(Collectors.toList());

        return new CrudPermissionViewResponse(rolePermissions, individualPermissions);
    }
}
