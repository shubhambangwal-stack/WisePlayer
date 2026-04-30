package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

    @Value("${file.upload-dir:uploads/support-tickets}")
    private String uploadDir;

    @Value("${file.allowed-extensions:jpg,jpeg,png,pdf}")
    private String allowedExtensions;

    @Value("${file.max-size:5242880}") // 5MB default
    private long maxFileSize;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        try {
            if (uploadDir == null || uploadDir.trim().isEmpty()) {
                // Default fallback if not configured
                uploadDir = "uploads/support-tickets";
            }
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("CRITICAL ERROR: Could not create upload directory at {}. Exception: {}", uploadDir, ex.getMessage());
            throw new RuntimeException("Could not initialize file storage", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            // Extract and validate extension
            String fileExtension = "";
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFileName.substring(i + 1).toLowerCase();
            } else {
                throw new RuntimeException("File must have an extension");
            }

            java.util.List<String> allowedList = java.util.Arrays.asList(allowedExtensions.split(","));
            if (!allowedList.contains(fileExtension)) {
                throw new RuntimeException("File extension ." + fileExtension + " is not allowed. Allowed: " + allowedExtensions);
            }

            // Create a unique filename to prevent overwriting and directory traversal
            String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path for URL mapping
            return "/uploads/support-tickets/" + newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
}
