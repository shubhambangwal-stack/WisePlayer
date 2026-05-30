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
    @Query("SELECT r FROM ActivationRequest r LEFT JOIN Device d ON d.deviceId = r.deviceId " +
            "WHERE r.resellerId = :resellerId AND " +
            "(:status IS NULL OR :status = '' OR LOWER(r.status) = LOWER(:status)) AND " +
            "(:planName IS NULL OR :planName = '' OR LOWER(r.planName) LIKE LOWER(CONCAT('%', :planName, '%'))) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "CAST(r.deviceId AS string) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.planName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.macAddress) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ActivationRequest> searchResellerRequests(
            @Param("resellerId") UUID resellerId,
            @Param("status") String status,
            @Param("planName") String planName,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT r FROM ActivationRequest r LEFT JOIN Device d ON d.deviceId = r.deviceId " +
            "WHERE (:status IS NULL OR :status = '' OR LOWER(r.status) = LOWER(:status)) AND " +
            "(:planName IS NULL OR :planName = '' OR LOWER(r.planName) LIKE LOWER(CONCAT('%', :planName, '%'))) AND " +
            "(:deviceId IS NULL OR :deviceId = '' OR CAST(r.deviceId AS string) LIKE LOWER(CONCAT('%', :deviceId, '%')))")
    Page<ActivationRequest> searchActivationRequests(
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
