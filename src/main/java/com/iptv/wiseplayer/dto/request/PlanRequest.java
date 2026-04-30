package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class PlanRequest {
    @NotBlank(message = "Plan name is required")
    private String name;

    @Positive(message = "Duration must be positive")
    private int durationDays;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Credits value is required")
    @Positive(message = "Credits must be positive")
    private BigDecimal credits;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String description;

    public PlanRequest() {}

    public PlanRequest(String name, int durationDays, BigDecimal price, BigDecimal credits, String currency, String description) {
        this.name = name;
        this.durationDays = durationDays;
        this.price = price;
        this.credits = credits;
        this.currency = currency;
        this.description = description;
    }

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
}
