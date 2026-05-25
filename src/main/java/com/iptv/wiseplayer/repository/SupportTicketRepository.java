package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.SupportTicket;
import com.iptv.wiseplayer.domain.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    @Query("SELECT t FROM SupportTicket t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:macAddress IS NULL OR :macAddress = '' OR LOWER(t.macAddress) LIKE LOWER(CONCAT('%', :macAddress, '%'))) AND " +
           "(:email IS NULL OR :email = '' OR LOWER(t.email) LIKE LOWER(CONCAT('%', :email, '%')))")
    Page<SupportTicket> searchTickets(
            @Param("status") TicketStatus status,
            @Param("macAddress") String macAddress,
            @Param("email") String email,
            Pageable pageable);

    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);
}
