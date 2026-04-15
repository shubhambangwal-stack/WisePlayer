package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.CheckoutRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.dto.response.InvoiceResponse;
import com.iptv.wiseplayer.dto.response.PaymentHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    CheckoutResponse createCheckoutSession(CheckoutRequest request);

    void handleWebhook(String payload, String sigHeader);

    void handlePaypalWebhook(java.util.Map<String, Object> payload, java.util.Map<String, String> headers);

    com.iptv.wiseplayer.domain.entity.Payments captureOrder(String orderId);

    List<InvoiceResponse> getAllInvoicesByDevice(String deviceId);

    CheckoutResponse createCreditCheckoutSession(java.util.UUID resellerId, int creditAmount);

    InvoiceResponse getCurrentInvoice(String deviceId);

    List<com.iptv.wiseplayer.dto.response.PlanResponse> getActivePlans();

    byte[] generateInvoicePdf(String invoiceNumber, String deviceId);
}
