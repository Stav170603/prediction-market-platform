package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.DashboardSummaryResponse;
import com.virtualmarket.polymarket.dto.MarketStatisticsResponse;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.repository.MarketOutcomeRepository;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.TradeRepository;
import com.virtualmarket.polymarket.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private MarketRepository marketRepository;
    @Mock
    private MarketOutcomeRepository marketOutcomeRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PositionRepository positionRepository;
    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void marketStatisticsReturnRepositoryAggregates() {
        Market market = market(7L, "200.0000");
        when(marketRepository.findById(7L)).thenReturn(Optional.of(market));
        when(tradeRepository.countByMarketId(7L)).thenReturn(12L);
        when(tradeRepository.sumTotalCostByMarketId(7L)).thenReturn(new BigDecimal("345.67891"));
        when(tradeRepository.countDistinctUsersByMarketId(7L)).thenReturn(4L);

        MarketStatisticsResponse result = statisticsService.getMarketStatistics(7L);

        assertThat(result.getMarketId()).isEqualTo(7L);
        assertThat(result.getTotalTrades()).isEqualTo(12);
        assertThat(result.getTotalVolume()).isEqualByComparingTo("345.6789");
        assertThat(result.getLiquidity()).isEqualByComparingTo("200.0000");
        assertThat(result.getActiveTraders()).isEqualTo(4);
    }

    @Test
    void dashboardSummaryIncludesWalletAndCurrentPositionValue() {
        User user = new User();
        user.setId(9L);
        Market firstMarket = market(1L, "100.0000");
        firstMarket.setStatus(MarketStatus.OPEN);
        Market secondMarket = market(2L, "100.0000");
        secondMarket.setStatus(MarketStatus.RESOLVED);
        MarketOutcome firstYes = outcome(firstMarket, "0.6000");
        MarketOutcome secondYes = outcome(secondMarket, "0.4000");

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("900.0000"));

        Position openPosition = new Position();
        openPosition.setUser(user);
        openPosition.setMarket(firstMarket);
        openPosition.setOutcome(firstYes);
        openPosition.setQuantity(new BigDecimal("10.0000"));

        Position emptyPosition = new Position();
        emptyPosition.setQuantity(BigDecimal.ZERO);
        emptyPosition.setOutcome(secondYes);

        when(marketRepository.countByStatus(MarketStatus.OPEN)).thenReturn(2L);
        when(marketRepository.count()).thenReturn(3L);
        when(marketRepository.findAll()).thenReturn(List.of(firstMarket, secondMarket));
        when(marketOutcomeRepository.findByMarketAndName(firstMarket, "YES")).thenReturn(Optional.of(firstYes));
        when(marketOutcomeRepository.findByMarketAndName(secondMarket, "YES")).thenReturn(Optional.of(secondYes));
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(positionRepository.findByUser(user)).thenReturn(List.of(openPosition, emptyPosition));
        when(tradeRepository.countByUser(user)).thenReturn(5L);

        DashboardSummaryResponse result = statisticsService.getDashboardSummary(user);

        assertThat(result.getOpenMarkets()).isEqualTo(2);
        assertThat(result.getTotalMarkets()).isEqualTo(3);
        assertThat(result.getAveragePrice()).isEqualByComparingTo("0.5000");
        assertThat(result.getWalletBalance()).isEqualByComparingTo("900.0000");
        assertThat(result.getPortfolioValue()).isEqualByComparingTo("906.0000");
        assertThat(result.getTotalTrades()).isEqualTo(5);
        assertThat(result.getOpenPositions()).isEqualTo(1);
    }

    private Market market(Long id, String liquidity) {
        Market market = new Market();
        market.setId(id);
        market.setLiquidity(new BigDecimal(liquidity));
        return market;
    }

    private MarketOutcome outcome(Market market, String price) {
        MarketOutcome outcome = new MarketOutcome();
        outcome.setMarket(market);
        outcome.setName("YES");
        outcome.setCurrentPrice(new BigDecimal(price));
        return outcome;
    }
}
