package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.AdminDashboardResponse;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.PriceHistory;
import com.virtualmarket.polymarket.entity.Trade;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.PriceHistoryRepository;
import com.virtualmarket.polymarket.repository.TradeRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminDashboardService {

    public static final BigDecimal SHARP_MOVEMENT_THRESHOLD = new BigDecimal("0.1000");
    public static final int SHARP_MOVEMENT_WINDOW_HOURS = 24;

    private final MarketRepository marketRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public AdminDashboardService(
            MarketRepository marketRepository,
            UserRepository userRepository,
            TradeRepository tradeRepository,
            PriceHistoryRepository priceHistoryRepository
    ) {
        this.marketRepository = marketRepository;
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(SHARP_MOVEMENT_WINDOW_HOURS);
        List<Market> attention = marketRepository
                .findByStatusAndResolutionDateLessThanEqualOrderByResolutionDateAsc(MarketStatus.CLOSED, now);

        AdminDashboardResponse.Summary summary = new AdminDashboardResponse.Summary(
                marketRepository.count(),
                marketRepository.countByStatus(MarketStatus.OPEN),
                marketRepository.countByStatus(MarketStatus.CLOSED),
                attention.size(),
                marketRepository.countByStatus(MarketStatus.RESOLVED),
                marketRepository.countByStatus(MarketStatus.CANCELLED),
                userRepository.count(),
                tradeRepository.countDistinctUsersWithTradesSince(windowStart),
                tradeRepository.count(),
                normalize(tradeRepository.sumTotalCost())
        );

        List<AdminDashboardResponse.AttentionMarket> attentionItems = attention.stream()
                .map(market -> new AdminDashboardResponse.AttentionMarket(
                        market.getId(), market.getTitle(), market.getCategory(),
                        market.getTradingCloseDate(), market.getResolutionDate()))
                .toList();

        List<AdminDashboardResponse.RecentTrade> recentTrades = tradeRepository
                .findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::toRecentTrade)
                .toList();

        List<AdminDashboardResponse.SharpMovement> movements = marketRepository.findAll().stream()
                .map(market -> movementFor(market, windowStart))
                .filter(movement -> movement != null)
                .sorted(Comparator.comparing(AdminDashboardResponse.SharpMovement::absoluteChange).reversed())
                .toList();

        return new AdminDashboardResponse(
                summary, attentionItems, recentTrades, movements,
                SHARP_MOVEMENT_THRESHOLD, SHARP_MOVEMENT_WINDOW_HOURS);
    }

    private AdminDashboardResponse.RecentTrade toRecentTrade(Trade trade) {
        return new AdminDashboardResponse.RecentTrade(
                trade.getId(), trade.getUser().getUsername(), trade.getMarket().getId(),
                trade.getMarket().getTitle(), trade.getType(), trade.getOutcome().getName(),
                trade.getQuantity(), trade.getTotalCost(), trade.getCreatedAt());
    }

    private AdminDashboardResponse.SharpMovement movementFor(Market market, LocalDateTime windowStart) {
        List<PriceHistory> history = priceHistoryRepository
                .findByMarketAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(market, windowStart);
        if (history.size() < 2) return null;

        PriceHistory first = history.getFirst();
        PriceHistory last = history.getLast();
        BigDecimal change = last.getYesPrice().subtract(first.getYesPrice()).abs().setScale(4, RoundingMode.HALF_UP);
        if (change.compareTo(SHARP_MOVEMENT_THRESHOLD) < 0) return null;

        return new AdminDashboardResponse.SharpMovement(
                market.getId(), market.getTitle(), first.getYesPrice(), last.getYesPrice(),
                change, first.getCreatedAt(), last.getCreatedAt());
    }

    private BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }
}
