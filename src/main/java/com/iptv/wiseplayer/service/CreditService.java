package com.iptv.wiseplayer.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreditService {

    /**
     * Deducts credits from a reseller for an activation request.
     */
    void deductCredits(UUID resellerId, String planName, UUID requestId);

    /**
     * Refunds credits to a reseller for a rejected/cancelled activation request.
     */
    void refundCredits(UUID resellerId, UUID requestId);

    /**
     * Adds credits to a reseller after a successful purchase.
     */
    void addCredits(UUID resellerId, int amount, String paymentId);

    /**
     * Calculates the unit price for a credit purchase based on quantity.
     */
    BigDecimal calculateUnitPrice(int quantity);

    /**
     * Calculates the total cost for a credit activation based on plan name.
     */
    BigDecimal getActivationCost(String planName);

    /**
     * Gets the current credit balance for a reseller.
     */
    BigDecimal getBalance(UUID resellerId);

    /**
     * Gets the transaction history for a reseller.
     */
    org.springframework.data.domain.Page<com.iptv.wiseplayer.dto.response.CreditTransactionResponse> getTransactionHistory(
            UUID resellerId, String search, String type, org.springframework.data.domain.Pageable pageable);

    /**
     * Transfers credits from one reseller to another (typically parent to child).
     */
    void transferCredits(UUID fromId, UUID toId, BigDecimal amount);
}
