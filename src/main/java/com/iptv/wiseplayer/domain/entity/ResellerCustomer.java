package com.iptv.wiseplayer.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a device (MAC address) claimed or created by a reseller.
 * Used to pre-authorize or link devices before they actually register via the app.
 */
@Entity
@Table(name = "reseller_customers")
public class ResellerCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "reseller_id", nullable = false)
    private UUID resellerId;

    @Column(name = "mac_address", nullable = false, length = 100)
    private String macAddress;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ResellerCustomer() {
    }

    public ResellerCustomer(UUID resellerId, String macAddress, String customerName) {
        this.resellerId = resellerId;
        this.macAddress = macAddress;
        this.customerName = customerName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResellerId() {
        return resellerId;
    }

    public void setResellerId(UUID resellerId) {
        this.resellerId = resellerId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
