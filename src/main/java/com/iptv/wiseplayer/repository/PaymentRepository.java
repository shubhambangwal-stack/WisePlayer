package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByStripeSessionId(String sessionId);

    Optional<Payment> findByStripeEventId(String eventId);
}
