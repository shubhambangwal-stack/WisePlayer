package com.iptv.wiseplayer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlanResponse {
    private UUID id;
    private String name;
    private int durationDays;
    private BigDecimal price;
    private BigDecimal credits;
    private String currency;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    public PlanResponse() {}

    public PlanResponse(UUID id, String name, int durationDays, BigDecimal price, BigDecimal credits,
                        String currency, String description, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.durationDays = durationDays;
        this.price = price;
        this.credits = credits;
        this.currency = currency;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
