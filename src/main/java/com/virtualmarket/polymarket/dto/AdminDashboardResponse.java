package com.virtualmarket.polymarket.dto;

import com.virtualmarket.polymarket.enums.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminDashboardResponse(
        Summary summary,
        List<AttentionMarket> marketsRequiringAttention,
        List<RecentTrade> recentActivity,
        List<SharpMovement> sharpPriceMovements,
        BigDecimal sharpMovementThreshold,
        int sharpMovementWindowHours
) {
    public record Summary(
            long totalMarkets,
            long openMarkets,
            long closedMarkets,
            long waitingForResolution,
            long resolvedMarkets,
            long cancelledMarkets,
            long totalUsers,
            @Schema(description = "Distinct users with at least one trade in the preceding 24 hours", example = "12")
            long activeUsersLast24Hours,
            long totalTrades,
            BigDecimal totalTradingVolume
    ) {}

    public record AttentionMarket(
            Long marketId,
            String title,
            String category,
            LocalDateTime tradingCloseDate,
            LocalDateTime resolutionDate
    ) {}

    public record RecentTrade(
            Long tradeId,
            String username,
            Long marketId,
            String marketTitle,
            TradeType type,
            String outcome,
            BigDecimal quantity,
            BigDecimal totalValue,
            LocalDateTime createdAt
    ) {}

    public record SharpMovement(
            Long marketId,
            String marketTitle,
            BigDecimal previousYesPrice,
            BigDecimal currentYesPrice,
            BigDecimal absoluteChange,
            LocalDateTime windowStart,
            LocalDateTime windowEnd
    ) {}
}
