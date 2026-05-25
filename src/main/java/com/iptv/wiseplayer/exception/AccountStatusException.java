package com.iptv.wiseplayer.exception;

/**
 * Thrown when the upstream IPTV provider returns a status that is not "Active",
 * or when the status field is missing/null in the provider's response.
 */
public class AccountStatusException extends RuntimeException {

    private final String status;

    public AccountStatusException(String status) {
        super(buildMessage(status));
        this.status = status;
    }

    private static String buildMessage(String status) {
        if (status == null || status.isBlank()) {
            return "Account status could not be determined from the IPTV provider response. "
                    + "Please verify your credentials or contact your provider.";
        }
        return "Account is not active. Current status reported by provider: " + status;
    }

    public String getStatus() {
        return status;
    }
}
