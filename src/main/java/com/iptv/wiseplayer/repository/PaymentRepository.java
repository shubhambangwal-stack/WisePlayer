package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Payments;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payments, UUID> {
    Optional<Payments> findByPaypalOrderId(String orderId);

    Optional<Payments> findByPaypalCaptureId(String captureId);

    Optional<Payments> findByStripeSessionId(String sessionId);

    Optional<Payments> findByStripeEventId(String eventId);

    List<Payments> findAllByDeviceIdOrderByCreatedAtDesc(UUID deviceId);

    @Query("SELECT p FROM payments p WHERE " +
           "(:paymentId IS NULL OR :paymentId = '' OR CAST(p.id AS string) LIKE LOWER(CONCAT('%', :paymentId, '%'))) AND " +
           "(:deviceId IS NULL OR :deviceId = '' OR CAST(p.deviceId AS string) LIKE LOWER(CONCAT('%', :deviceId, '%'))) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Payments> searchPayments(
            @Param("paymentId") String paymentId,
            @Param("deviceId") String deviceId,
            @Param("status") PaymentStatus status,
            Pageable pageable);

    Optional<Payments> findTopByDeviceIdAndStatusOrderByCreatedAtDesc(UUID deviceId,
            com.iptv.wiseplayer.domain.enums.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM payments p WHERE p.status = com.iptv.wiseplayer.domain.enums.PaymentStatus.SUCCESS")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM payments p WHERE p.status = com.iptv.wiseplayer.domain.enums.PaymentStatus.SUCCESS AND p.createdAt >= :from AND p.createdAt <= :to")
    BigDecimal sumTotalRevenueBetween(@Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    long countByStatus(com.iptv.wiseplayer.domain.enums.PaymentStatus status);

    long countByStatusAndCreatedAtBetween(com.iptv.wiseplayer.domain.enums.PaymentStatus status,
            java.time.LocalDateTime from, java.time.LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM payments p WHERE p.resellerId = :resellerId AND p.status = :status AND p.createdAt >= :from AND p.createdAt <= :to")
    java.math.BigDecimal sumTotalRevenueByResellerIdAndStatusAndCreatedAtBetween(
            @Param("resellerId") java.util.UUID resellerId,
            @Param("status") com.iptv.wiseplayer.domain.enums.PaymentStatus status,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    @Query(value = "SELECT * FROM payments p WHERE CAST(p.payment_id AS TEXT) LIKE :prefix%", nativeQuery = true)
    List<Payments> findByIdPrefix(@Param("prefix") String prefix);
}
