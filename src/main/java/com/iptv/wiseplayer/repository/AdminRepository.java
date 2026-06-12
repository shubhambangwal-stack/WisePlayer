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

//    @Query("SELECT a FROM Admin a WHERE a.parentId = :parentId AND " +
//            "(:search IS NULL OR :search = '' OR " +
//            "LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//            "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//            "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
//            "(:status IS NULL OR a.active = :status)")
//    Page<Admin> searchSubResellers(
//            @Param("parentId") UUID parentId,
//            @Param("search") String search,
//            @Param("status") Boolean status,
//            Pageable pageable);
@Query(value = "SELECT a.* FROM admins a WHERE a.parent_id = :parentId " +
        "AND (:status IS NULL OR a.is_active = :status) " +
        "AND (:fromDate IS NULL OR a.created_at >= CAST(:fromDate AS timestamp)) " +
        "AND (:toDate IS NULL OR a.created_at <= CAST(:toDate AS timestamp)) " +
        "AND (:minCredits IS NULL OR a.credits >= CAST(:minCredits AS numeric)) " +
        "AND (:maxCredits IS NULL OR a.credits <= CAST(:maxCredits AS numeric)) " +
        "AND (:search IS NULL OR " +
        "    a.username ILIKE '%' || :search || '%' OR " +
        "    a.full_name ILIKE '%' || :search || '%' OR " +
        "    a.admin_id::text ILIKE '%' || :search || '%') " +
        "ORDER BY a.created_at DESC",
        countQuery = "SELECT COUNT(*) FROM admins a WHERE a.parent_id = :parentId " +
                "AND (:status IS NULL OR a.is_active = :status) " +
                "AND (:fromDate IS NULL OR a.created_at >= CAST(:fromDate AS timestamp)) " +
                "AND (:toDate IS NULL OR a.created_at <= CAST(:toDate AS timestamp)) " +
                "AND (:minCredits IS NULL OR a.credits >= CAST(:minCredits AS numeric)) " +
                "AND (:maxCredits IS NULL OR a.credits <= CAST(:maxCredits AS numeric)) " +
                "AND (:search IS NULL OR " +
                "    a.username ILIKE '%' || :search || '%' OR " +
                "    a.full_name ILIKE '%' || :search || '%' OR " +
                "    a.id::text ILIKE '%' || :search || '%')",
        nativeQuery = true)
Page<Admin> searchSubResellers(
        @Param("parentId") UUID parentId,
        @Param("search") String search,
        @Param("status") Boolean status,
        @Param("fromDate") String fromDate,
        @Param("toDate") String toDate,
        @Param("minCredits") java.math.BigDecimal minCredits,
        @Param("maxCredits") java.math.BigDecimal maxCredits,
        Pageable pageable);
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Admin> findByUsername(String username);

    List<Admin> findAllByParentId(UUID parentId);

    Page<Admin> findAllByParentId(UUID parentId, Pageable pageable);

    Page<Admin> findAllByRoleIn(List<AdminRole> roles, Pageable pageable);

    List<Admin> findAllByRoleIn(List<AdminRole> roles);
}
