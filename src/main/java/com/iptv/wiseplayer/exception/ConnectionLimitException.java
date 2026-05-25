package com.iptv.wiseplayer.exception;

/**
 * Thrown when the upstream IPTV provider reports that the user's connection
 * limit has been reached (active_cons >= max_connections).
 */
public class ConnectionLimitException extends RuntimeException {

    private final int active;
    private final int max;

    public ConnectionLimitException(int active, int max) {
        super("Maximum connection limit reached (" + active + "/" + max + "). "
                + "Please close other active streams before starting a new one.");
        this.active = active;
        this.max = max;
    }

    public int getActive() {
        return active;
    }

    public int getMax() {
        return max;
    }
}
