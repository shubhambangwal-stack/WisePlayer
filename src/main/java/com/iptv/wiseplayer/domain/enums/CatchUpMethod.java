package com.iptv.wiseplayer.domain.enums;

/**
 * Catch-up / timeshift method used to build the archived stream URL.
 *
 * <p>The identifiers match the well-known {@code catchup} attribute values used
 * by IPTV providers in M3U_plus playlists, plus {@link #XC} which is how Xtream
 * Codes panels expose their TV archive.
 */
public enum CatchUpMethod {

    /** Xtream Codes TV archive: {@code /live/{user}/{pass}/{channel}.{ext}?start={utc}&end={utcend}}. */
    XC("xc"),

    /** Flussonic timeshift: {@code /flussonic/{user}/{pass}/timeshift_abs/{utc}/{channel}.ts}. */
    FLUSSONIC("flussonic"),

    /** SIPTV shift: {@code /shift/{user}/{pass}/{utc}.{utcend}.{channel}.{ext}}. */
    SHIFT("shift"),

    /** Default/simple method: {@code /{user}/{pass}/{channel}/{utc}.{utcend}.{ext}}. */
    DEFAULT("default"),

    /** Catch-up is not available / not playable. */
    NONE("none");

    private final String code;

    CatchUpMethod(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Normalizes a provider-supplied catch-up identifier (from {@code catchup=},
     * {@code timeshift=} or M3U header attributes) into a known method.
     *
     * @return the matching method, or {@link #NONE} when the value is unknown
     */
    public static CatchUpMethod fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NONE;
        }
        String normalized = code.trim().toLowerCase();
        switch (normalized) {
            case "xc":
            case "xtream":
            case "xtream-codes":
                return XC;
            case "flussonic":
            case "fs":
                return FLUSSONIC;
            case "shift":
                return SHIFT;
            case "default":
            case "append":
            case "timeshift":
            case "1":
            case "true":
            case "yes":
                return DEFAULT;
            default:
                return NONE;
        }
    }
}