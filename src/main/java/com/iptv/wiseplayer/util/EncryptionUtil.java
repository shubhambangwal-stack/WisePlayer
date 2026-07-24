package com.iptv.wiseplayer.util;

import org.springframework.stereotype.Component;

@Component
public class EncryptionUtil {

    public EncryptionUtil() {
    }

    public String encrypt(String plainText) {
        if (plainText == null)
            return null;
        return plainText; // Storing as plain text per request
    }

    public String decrypt(String cipherText) {
        if (cipherText == null)
            return null;
        return cipherText; // Returning plain text per request
    }
}
