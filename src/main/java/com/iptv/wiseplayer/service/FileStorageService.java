package com.iptv.wiseplayer.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Stores a file locally (or in cloud) and returns the relative/absolute url or
     * path.
     * 
     * @param file The multipart file to store
     * @return The stored path or URL identifier
     */
    String storeFile(MultipartFile file);
}
