package com.virtualmarket.polymarket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateMarketRequest {

    @NotNull(message = "Admin user id is required")
    private Long adminUserId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Trading close date is required")
    private LocalDateTime tradingCloseDate;

    @NotNull(message = "Resolution date is required")
    private LocalDateTime resolutionDate;

    @NotBlank(message = "Resolution source is required")
    private String resolutionSource;

    @DecimalMin(value = "0.0", inclusive = false, message = "Liquidity must be greater than 0")
    private BigDecimal liquidity;

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getTradingCloseDate() {
        return tradingCloseDate;
    }

    public void setTradingCloseDate(LocalDateTime tradingCloseDate) {
        this.tradingCloseDate = tradingCloseDate;
    }

    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getResolutionSource() {
        return resolutionSource;
    }

    public void setResolutionSource(String resolutionSource) {
        this.resolutionSource = resolutionSource;
    }

    public BigDecimal getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(BigDecimal liquidity) {
        this.liquidity = liquidity;
    }
}
