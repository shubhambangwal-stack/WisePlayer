package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaypalOrderId(String orderId);

    Optional<Payment> findByStripeSessionId(String sessionId);

    Optional<Payment> findByStripeEventId(String eventId);

    List<Payment> findAllByDeviceIdOrderByCreatedAtDesc(UUID deviceId);

    Optional<Payment> findTopByDeviceIdAndStatusOrderByCreatedAtDesc(UUID deviceId,
            com.iptv.wiseplayer.domain.enums.PaymentStatus status);
}
