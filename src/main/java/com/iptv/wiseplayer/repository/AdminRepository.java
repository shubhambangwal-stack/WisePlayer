package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {
    @Query("SELECT a FROM Admin a WHERE a.role IN :roles AND " +
           "(:username IS NULL OR :username = '' OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:fullName IS NULL OR :fullName = '' OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))) AND " +
           "(:email IS NULL OR :email = '' OR LOWER(a.email) LIKE LOWER(CONCAT('%', :email, '%')))")
    Page<Admin> searchResellers(
            @Param("roles") List<AdminRole> roles,
            @Param("username") String username,
            @Param("fullName") String fullName,
            @Param("email") String email,
            Pageable pageable);

    @Query("SELECT a FROM Admin a WHERE a.parentId = :parentId AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:status IS NULL OR a.active = :status)")
    Page<Admin> searchSubResellers(
            @Param("parentId") UUID parentId,
            @Param("search") String search,
            @Param("status") Boolean status,
            Pageable pageable);
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Admin> findByUsername(String username);

    List<Admin> findAllByParentId(UUID parentId);

    Page<Admin> findAllByParentId(UUID parentId, Pageable pageable);

    Page<Admin> findAllByRoleIn(List<AdminRole> roles, Pageable pageable);

    List<Admin> findAllByRoleIn(List<AdminRole> roles);
}
