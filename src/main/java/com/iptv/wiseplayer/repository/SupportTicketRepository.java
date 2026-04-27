package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.SupportTicket;
import com.iptv.wiseplayer.domain.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);
}
