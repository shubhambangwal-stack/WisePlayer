package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivationRequestRepository extends JpaRepository<ActivationRequest, UUID> {
    List<ActivationRequest> findAllByResellerId(UUID resellerId);

    org.springframework.data.domain.Page<ActivationRequest> findAllByResellerId(UUID resellerId,
            org.springframework.data.domain.Pageable pageable);

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
