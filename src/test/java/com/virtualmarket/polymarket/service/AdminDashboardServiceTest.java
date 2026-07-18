package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.AdminDashboardResponse;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.PriceHistory;
import com.virtualmarket.polymarket.entity.Trade;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.TradeType;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.PriceHistoryRepository;
import com.virtualmarket.polymarket.repository.TradeRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock MarketRepository marketRepository;
    @Mock UserRepository userRepository;
    @Mock TradeRepository tradeRepository;
    @Mock PriceHistoryRepository priceHistoryRepository;
    @InjectMocks AdminDashboardService service;

    @Test
    void returnsCountsAttentionRecentTradesAndSharpMovements() {
        Market market = market(1L, "Election result");
        User user = new User();
        user.setUsername("alex");
        MarketOutcome yes = new MarketOutcome();
        yes.setName("YES");
        Trade trade = new Trade();
        trade.setId(8L);
        trade.setUser(user);
        trade.setMarket(market);
        trade.setOutcome(yes);
        trade.setType(TradeType.BUY);
        trade.setQuantity(new BigDecimal("4.0000"));
        trade.setTotalCost(new BigDecimal("2.4000"));
        trade.setCreatedAt(LocalDateTime.now());

        when(marketRepository.count()).thenReturn(4L);
        when(marketRepository.countByStatus(MarketStatus.OPEN)).thenReturn(1L);
        when(marketRepository.countByStatus(MarketStatus.CLOSED)).thenReturn(1L);
        when(marketRepository.countByStatus(MarketStatus.RESOLVED)).thenReturn(1L);
        when(marketRepository.countByStatus(MarketStatus.CANCELLED)).thenReturn(1L);
        when(userRepository.count()).thenReturn(3L);
        when(tradeRepository.countDistinctUsersWithTradesSince(any(LocalDateTime.class))).thenReturn(2L);
        when(tradeRepository.count()).thenReturn(10L);
        when(tradeRepository.sumTotalCost()).thenReturn(new BigDecimal("42.12345"));
        when(marketRepository.findByStatusAndResolutionDateLessThanEqualOrderByResolutionDateAsc(
                eq(MarketStatus.CLOSED), any(LocalDateTime.class))).thenReturn(List.of(market));
        when(tradeRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(trade));
        when(marketRepository.findAll()).thenReturn(List.of(market));
        when(priceHistoryRepository.findByMarketAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                eq(market), any(LocalDateTime.class))).thenReturn(List.of(
                history(market, "0.4000", LocalDateTime.now().minusHours(2)),
                history(market, "0.5100", LocalDateTime.now().minusHours(1))));

        AdminDashboardResponse result = service.getDashboard();

        assertThat(result.summary().totalMarkets()).isEqualTo(4);
        assertThat(result.summary().waitingForResolution()).isEqualTo(1);
        assertThat(result.summary().activeUsersLast24Hours()).isEqualTo(2);
        assertThat(result.summary().totalTradingVolume()).isEqualByComparingTo("42.1235");
        assertThat(result.recentActivity()).singleElement().satisfies(item -> {
            assertThat(item.username()).isEqualTo("alex");
            assertThat(item.totalValue()).isEqualByComparingTo("2.4000");
        });
        assertThat(result.sharpPriceMovements()).singleElement()
                .extracting(AdminDashboardResponse.SharpMovement::absoluteChange)
                .isEqualTo(new BigDecimal("0.1100"));
        assertThat(result.sharpMovementThreshold()).isEqualByComparingTo("0.1000");
        assertThat(result.sharpMovementWindowHours()).isEqualTo(24);
    }

    @Test
    void oneUserWithSeveralRecentTradesCountsOnce() {
        stubEmptyDashboard();
        when(tradeRepository.countDistinctUsersWithTradesSince(any(LocalDateTime.class))).thenReturn(1L);

        assertThat(service.getDashboard().summary().activeUsersLast24Hours()).isEqualTo(1);
    }

    @Test
    void severalRecentUsersAreCountedCorrectly() {
        stubEmptyDashboard();
        when(tradeRepository.countDistinctUsersWithTradesSince(any(LocalDateTime.class))).thenReturn(4L);

        assertThat(service.getDashboard().summary().activeUsersLast24Hours()).isEqualTo(4);
    }

    @Test
    void tradesOlderThanTwentyFourHoursAreExcluded() {
        stubEmptyDashboard();
        when(tradeRepository.countDistinctUsersWithTradesSince(any(LocalDateTime.class))).thenReturn(0L);

        assertThat(service.getDashboard().summary().activeUsersLast24Hours()).isZero();
    }

    @Test
    void noRecentTradesReturnsZero() {
        stubEmptyDashboard();

        assertThat(service.getDashboard().summary().activeUsersLast24Hours()).isZero();
    }

    private void stubEmptyDashboard() {
        when(marketRepository.findByStatusAndResolutionDateLessThanEqualOrderByResolutionDateAsc(
                eq(MarketStatus.CLOSED), any(LocalDateTime.class))).thenReturn(List.of());
        when(tradeRepository.sumTotalCost()).thenReturn(BigDecimal.ZERO);
        when(tradeRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(marketRepository.findAll()).thenReturn(List.of());
    }

    private Market market(Long id, String title) {
        Market market = new Market();
        market.setId(id);
        market.setTitle(title);
        market.setCategory("Politics");
        market.setStatus(MarketStatus.CLOSED);
        market.setTradingCloseDate(LocalDateTime.now().minusDays(1));
        market.setResolutionDate(LocalDateTime.now().minusHours(1));
        return market;
    }

    private PriceHistory history(Market market, String yesPrice, LocalDateTime createdAt) {
        PriceHistory history = new PriceHistory();
        history.setMarket(market);
        history.setYesPrice(new BigDecimal(yesPrice));
        history.setNoPrice(BigDecimal.ONE.subtract(new BigDecimal(yesPrice)));
        history.setCreatedAt(createdAt);
        return history;
    }
}
