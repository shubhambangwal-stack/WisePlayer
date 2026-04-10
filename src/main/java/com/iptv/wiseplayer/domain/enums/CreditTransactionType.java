package com.iptv.wiseplayer.domain.enums;

/**
 * Enum for types of credit transactions.
 */
public enum CreditTransactionType {
    PURCHASE, // When a reseller buys credits
    DEDUCTION, // When credits are used for an activation request
    REFUND, // When a rejected activation request returns credits
    MANUAL_ADJUSTMENT, // When an admin manually modifies the credit amount
    TRANSFER_IN, // When a reseller receives credits from another reseller
    TRANSFER_OUT // When a reseller transfers credits to another reseller
}
