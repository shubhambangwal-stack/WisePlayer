package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.AdminInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminInviteRepository extends JpaRepository<AdminInvite, Long> {
    Optional<AdminInvite> findByToken(String token);

    Optional<AdminInvite> findByEmail(String email);
}
