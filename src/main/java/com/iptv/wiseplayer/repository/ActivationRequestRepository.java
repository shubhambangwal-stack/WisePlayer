package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivationRequestRepository extends JpaRepository<ActivationRequest, UUID> {
    @Query(
            value = "SELECT r.* FROM activation_requests r " +
                    "LEFT JOIN devices d ON d.device_id = r.device_id " +
                    "WHERE r.reseller_id = :resellerId " +
                    "AND (:status IS NULL OR LOWER(r.status) = LOWER(:status)) " +
                    "AND (:planName IS NULL OR LOWER(r.plan_name) LIKE LOWER(CONCAT('%', :planName, '%'))) " +
                    "AND (:fromDate IS NULL OR r.created_at >= CAST(:fromDate AS timestamp)) " +
                    "AND (:toDate IS NULL OR r.created_at <= CAST(:toDate AS timestamp)) " +
                    "AND (:minCredits IS NULL OR r.credits_used >= CAST(:minCredits AS numeric)) " +
                    "AND (:maxCredits IS NULL OR r.credits_used <= CAST(:maxCredits AS numeric)) " +
                    "AND (:search IS NULL OR " +
                    "    r.request_id::text ILIKE '%' || :search || '%' OR " +
                    "    r.device_id::text ILIKE '%' || :search || '%' OR " +
                    "    r.plan_name ILIKE '%' || :search || '%' OR " +
                    "    d.mac_address ILIKE '%' || :search || '%') " +
                    "ORDER BY r.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM activation_requests r " +
                    "LEFT JOIN devices d ON d.device_id = r.device_id " +
                    "WHERE r.reseller_id = :resellerId " +
                    "AND (:status IS NULL OR LOWER(r.status) = LOWER(:status)) " +
                    "AND (:planName IS NULL OR LOWER(r.plan_name) LIKE LOWER(CONCAT('%', :planName, '%'))) " +
                    "AND (:fromDate IS NULL OR r.created_at >= CAST(:fromDate AS timestamp)) " +
                    "AND (:toDate IS NULL OR r.created_at <= CAST(:toDate AS timestamp)) " +
                    "AND (:minCredits IS NULL OR r.credits_used >= CAST(:minCredits AS numeric)) " +
                    "AND (:maxCredits IS NULL OR r.credits_used <= CAST(:maxCredits AS numeric)) " +
                    "AND (:search IS NULL OR " +
                    "    r.request_id::text ILIKE '%' || :search || '%' OR " +
                    "    r.device_id::text ILIKE '%' || :search || '%' OR " +
                    "    r.plan_name ILIKE '%' || :search || '%' OR " +
                    "    d.mac_address ILIKE '%' || :search || '%')",
            nativeQuery = true)
    Page<ActivationRequest> searchResellerRequests(
            @Param("resellerId") UUID resellerId,
            @Param("status") String status,
            @Param("planName") String planName,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            @Param("minCredits") java.math.BigDecimal minCredits,
            @Param("maxCredits") java.math.BigDecimal maxCredits,
            @Param("search") String search,
            Pageable pageable);

    @Query(
            value = "SELECT r.* FROM activation_requests r " +
                    "LEFT JOIN devices d ON d.device_id = r.device_id " +
                    "WHERE (:resellerId IS NULL OR r.reseller_id = CAST(:resellerId AS uuid)) " +
                    "AND (:status IS NULL OR LOWER(r.status) = LOWER(:status)) " +
                    "AND (:planName IS NULL OR LOWER(r.plan_name) LIKE LOWER(CONCAT('%', :planName, '%'))) " +
                    "AND (:deviceId IS NULL OR r.device_id::text ILIKE '%' || :deviceId || '%') " +
                    "ORDER BY r.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM activation_requests r " +
                    "LEFT JOIN devices d ON d.device_id = r.device_id " +
                    "WHERE (:resellerId IS NULL OR r.reseller_id = CAST(:resellerId AS uuid)) " +
                    "AND (:status IS NULL OR LOWER(r.status) = LOWER(:status)) " +
                    "AND (:planName IS NULL OR LOWER(r.plan_name) LIKE LOWER(CONCAT('%', :planName, '%'))) " +
                    "AND (:deviceId IS NULL OR r.device_id::text ILIKE '%' || :deviceId || '%')",
            nativeQuery = true)
    Page<ActivationRequest> searchActivationRequests(
            @Param("resellerId") String resellerId,
            @Param("status") String status,
            @Param("deviceId") String deviceId,
            @Param("planName") String planName,
            Pageable pageable);

    void deleteAllByDeviceId(UUID deviceId);

    List<ActivationRequest> findAllByResellerId(UUID resellerId);

    org.springframework.data.domain.Page<ActivationRequest> findAllByResellerId(UUID resellerId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<ActivationRequest> findAllByResellerIdAndStatus(UUID resellerId,
            String status, org.springframework.data.domain.Pageable pageable);

    long countByResellerIdAndStatus(UUID resellerId, String status);

    boolean existsByDeviceIdAndStatus(UUID deviceId, String status);

    org.springframework.data.domain.Page<ActivationRequest> findAllByStatus(String status,
            org.springframework.data.domain.Pageable pageable);

    java.util.List<ActivationRequest> findAllByStatus(String status);

    long countByStatus(String status);

    java.util.Optional<ActivationRequest> findTopByDeviceIdOrderByCreatedAtDesc(UUID deviceId);

    java.util.List<ActivationRequest> findAllByResellerIdAndStatusAndCreatedAtBetween(
            UUID resellerId, String status, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
