package com.iptv.wiseplayer.dto.request;

import java.math.BigDecimal;

public class PlanRequest {
    private String name;
    private int durationDays;
    private BigDecimal price;
    private String currency;
    private String description;

    public PlanRequest() {}

    public PlanRequest(String name, int durationDays, BigDecimal price, String currency, String description) {
        this.name = name;
        this.durationDays = durationDays;
        this.price = price;
        this.currency = currency;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
