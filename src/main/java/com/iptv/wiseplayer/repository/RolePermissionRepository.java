package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.RolePermission;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, AdminRole> {
    Optional<RolePermission> findByRole(AdminRole role);
}
