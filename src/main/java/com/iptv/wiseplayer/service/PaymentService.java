package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.CheckoutRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;

public interface PaymentService {
    CheckoutResponse createCheckoutSession(CheckoutRequest request);

    void handleWebhook(String payload, String sigHeader);
}
