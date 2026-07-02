package com.virtualmarket.polymarket.dto;

import java.math.BigDecimal;

public class LeaderboardResponse {

    private int rank;
    private Long userId;
    private String username;
    private String email;
    private BigDecimal walletBalance;
    private long openPositions;
    private long totalTrades;
    private BigDecimal portfolioValue;
    private BigDecimal reliabilityScore;

    public LeaderboardResponse() {
    }

    public LeaderboardResponse(
            int rank,
            Long userId,
            String username,
            String email,
            BigDecimal walletBalance,
            long openPositions,
            long totalTrades,
            BigDecimal portfolioValue,
            BigDecimal reliabilityScore
    ) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.walletBalance = walletBalance;
        this.openPositions = openPositions;
        this.totalTrades = totalTrades;
        this.portfolioValue = portfolioValue;
        this.reliabilityScore = reliabilityScore;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public long getOpenPositions() {
        return openPositions;
    }

    public void setOpenPositions(long openPositions) {
        this.openPositions = openPositions;
    }

    public long getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(long totalTrades) {
        this.totalTrades = totalTrades;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(BigDecimal portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public BigDecimal getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(BigDecimal reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
    }
}
