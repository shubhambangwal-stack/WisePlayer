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

    public AdminResellerService(AdminRepository adminRepository,
                               DeviceRepository deviceRepository,
                               com.iptv.wiseplayer.repository.CreditTransactionRepository creditTransactionRepository,
                               com.iptv.wiseplayer.repository.ActivationRequestRepository activationRequestRepository,
                               com.iptv.wiseplayer.repository.PaymentRepository paymentRepository,
                               com.iptv.wiseplayer.repository.SubscriptionRepository subscriptionRepository) {
        this.adminRepository = adminRepository;
        this.deviceRepository = deviceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.activationRequestRepository = activationRequestRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public Page<ResellerResponse> getAllResellers(String username, String email, String fullName, Pageable pageable) {
        List<AdminRole> roles = Arrays.asList(AdminRole.RESELLER, AdminRole.SUB_RESELLER);
        return adminRepository.searchResellersWithFilters(roles, username, email, fullName, pageable)
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

    public Page<com.iptv.wiseplayer.dto.response.SubResellerResponse> getSubResellers(UUID id, String search, Pageable pageable) {
        Page<Admin> subs;
        if (search != null && !search.trim().isEmpty()) {
            subs = adminRepository.searchSubResellers(id, search.trim(), pageable);
        } else {
            subs = adminRepository.findAllByParentId(id, pageable);
        }
        
        return subs.map(sub -> com.iptv.wiseplayer.dto.response.SubResellerResponse.builder()
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
