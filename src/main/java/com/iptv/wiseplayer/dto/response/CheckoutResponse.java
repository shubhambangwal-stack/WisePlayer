package com.iptv.wiseplayer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class CheckoutResponse {
    private String checkoutUrl;
    private String sessionId;

    public CheckoutResponse() {
    }

    public CheckoutResponse(String checkoutUrl, String sessionId) {
        this.checkoutUrl = checkoutUrl;
        this.sessionId = sessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
