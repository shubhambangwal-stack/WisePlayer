package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    List<CreditTransaction> findAllByAdminIdOrderByCreatedAtDesc(UUID adminId);

    org.springframework.data.domain.Page<CreditTransaction> findAllByAdminIdOrderByCreatedAtDesc(UUID adminId,
                                                                                                 org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT * FROM credit_transactions t " +
                    "WHERE t.admin_id = :adminId " +
                    "AND (:type IS NULL OR t.type = :type) " +
                    "AND (:fromDate IS NULL OR t.created_at >= CAST(:fromDate AS timestamp)) " +
                    "AND (:toDate IS NULL OR t.created_at <= CAST(:toDate AS timestamp)) " +
                    "AND (:minAmount IS NULL OR ABS(t.amount) >= CAST(:minAmount AS numeric)) " +
                    "AND (:maxAmount IS NULL OR ABS(t.amount) <= CAST(:maxAmount AS numeric)) " +
                    "AND (:search IS NULL OR " +
                    "    t.notes ILIKE '%' || :search || '%' OR " +
                    "    t.type::text ILIKE '%' || :search || '%' OR " +
                    "    t.amount::text ILIKE '%' || :search || '%') " +
                    "ORDER BY t.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM credit_transactions t " +
                    "WHERE t.admin_id = :adminId " +
                    "AND (:type IS NULL OR t.type = :type) " +
                    "AND (:fromDate IS NULL OR t.created_at >= CAST(:fromDate AS timestamp)) " +
                    "AND (:toDate IS NULL OR t.created_at <= CAST(:toDate AS timestamp)) " +
                    "AND (:minAmount IS NULL OR ABS(t.amount) >= CAST(:minAmount AS numeric)) " +
                    "AND (:maxAmount IS NULL OR ABS(t.amount) <= CAST(:maxAmount AS numeric)) " +
                    "AND (:search IS NULL OR " +
                    "    t.notes ILIKE '%' || :search || '%' OR " +
                    "    t.type::text ILIKE '%' || :search || '%' OR " +
                    "    t.amount::text ILIKE '%' || :search || '%')",
            nativeQuery = true)
    org.springframework.data.domain.Page<CreditTransaction> searchTransactions(
            @org.springframework.data.repository.query.Param("adminId") UUID adminId,
            @org.springframework.data.repository.query.Param("type") String type,
            @org.springframework.data.repository.query.Param("fromDate") String fromDate,
            @org.springframework.data.repository.query.Param("toDate") String toDate,
            @org.springframework.data.repository.query.Param("minAmount") java.math.BigDecimal minAmount,
            @org.springframework.data.repository.query.Param("maxAmount") java.math.BigDecimal maxAmount,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable);
}