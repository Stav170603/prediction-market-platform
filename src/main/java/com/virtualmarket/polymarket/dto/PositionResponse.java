package com.virtualmarket.polymarket.dto;

import com.virtualmarket.polymarket.enums.MarketStatus;

import java.math.BigDecimal;

public class PositionResponse {

    private Long positionId;
    private Long userId;
    private Long marketId;
    private String marketTitle;
    private MarketStatus marketStatus;
    private Long outcomeId;
    private String outcomeName;
    private BigDecimal quantity;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal unrealizedPnL;

    public PositionResponse() {
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMarketId() {
        return marketId;
    }

    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }

    public String getMarketTitle() {
        return marketTitle;
    }

    public void setMarketTitle(String marketTitle) {
        this.marketTitle = marketTitle;
    }

    public MarketStatus getMarketStatus() {
        return marketStatus;
    }

    public void setMarketStatus(MarketStatus marketStatus) {
        this.marketStatus = marketStatus;
    }

    public Long getOutcomeId() {
        return outcomeId;
    }

    public void setOutcomeId(Long outcomeId) {
        this.outcomeId = outcomeId;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getUnrealizedPnL() {
        return unrealizedPnL;
    }

    public void setUnrealizedPnL(BigDecimal unrealizedPnL) {
        this.unrealizedPnL = unrealizedPnL;
    }
}
