package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.TradeRequest;
import com.virtualmarket.polymarket.dto.TradeResponse;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.PriceHistory;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.TradeType;
import com.virtualmarket.polymarket.repository.MarketOutcomeRepository;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.PriceHistoryRepository;
import com.virtualmarket.polymarket.repository.TradeRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import com.virtualmarket.polymarket.repository.WalletRepository;
import com.virtualmarket.polymarket.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private MarketRepository marketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private MarketOutcomeRepository marketOutcomeRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private RealTimeEventService realTimeEventService;

    private TradeService tradeService;
    private Market market;
    private User user;
    private Wallet wallet;
    private MarketOutcome yesOutcome;
    private MarketOutcome noOutcome;

    @BeforeEach
    void setUp() {
        PricingService pricingService = new PricingService(marketOutcomeRepository, priceHistoryRepository);
        MarketLifecycleService marketLifecycleService = new MarketLifecycleService(marketRepository);
        tradeService = new TradeService(
                marketRepository,
                userRepository,
                walletRepository,
                marketOutcomeRepository,
                positionRepository,
                tradeRepository,
                walletTransactionRepository,
                pricingService,
                realTimeEventService,
                marketLifecycleService
        );

        market = new Market();
        market.setId(10L);
        market.setStatus(MarketStatus.OPEN);
        market.setLiquidity(new BigDecimal("100.0000"));
        market.setTradingCloseDate(LocalDateTime.now().plusDays(1));

        user = new User();
        user.setId(20L);

        wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("100.0000"));

        yesOutcome = outcome(30L, "YES");
        noOutcome = outcome(31L, "NO");

    }

    @Test
    void successfulBuyUpdatesBalancesPricesAndPriceHistory() {
        stubRequiredEntities();
        when(positionRepository.findByUserAndMarketAndOutcome(user, market, yesOutcome))
                .thenReturn(Optional.empty());
        when(marketOutcomeRepository.findByMarketAndName(market, "YES"))
                .thenReturn(Optional.of(yesOutcome));
        when(marketOutcomeRepository.findByMarketAndName(market, "NO"))
                .thenReturn(Optional.of(noOutcome));

        TradeResponse response = tradeService.executeTrade(request(TradeType.BUY, "10.0000"));

        assertThat(response.getTotalCost()).isEqualByComparingTo("5.0000");
        assertThat(response.getWalletBalanceAfterTrade()).isEqualByComparingTo("95.0000");
        assertThat(response.getPositionQuantityAfterTrade()).isEqualByComparingTo("10.0000");
        assertThat(yesOutcome.getShares()).isEqualByComparingTo("10.0000");
        assertThat(yesOutcome.getCurrentPrice()).isEqualByComparingTo("0.5238");
        assertThat(noOutcome.getCurrentPrice()).isEqualByComparingTo("0.4762");

        ArgumentCaptor<PriceHistory> historyCaptor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(historyCaptor.capture());
        PriceHistory history = historyCaptor.getValue();
        assertThat(history.getMarket()).isSameAs(market);
        assertThat(history.getYesPrice()).isEqualByComparingTo("0.5238");
        assertThat(history.getNoPrice()).isEqualByComparingTo("0.4762");

        verify(tradeRepository).save(any());
        verify(walletTransactionRepository).save(any());
        verify(marketOutcomeRepository).saveAll(any());
    }

    @Test
    void buyIsRejectedWhenWalletBalanceIsInsufficient() {
        stubRequiredEntities();
        wallet.setBalance(new BigDecimal("4.9999"));
        when(positionRepository.findByUserAndMarketAndOutcome(user, market, yesOutcome))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "10.0000")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient balance");

        verify(walletRepository, never()).save(any());
        verify(tradeRepository, never()).save(any());
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void sellIsRejectedWhenPositionQuantityIsInsufficient() {
        stubRequiredEntities();
        Position position = new Position();
        position.setUser(user);
        position.setMarket(market);
        position.setOutcome(yesOutcome);
        position.setQuantity(new BigDecimal("2.0000"));
        when(positionRepository.findByUserAndMarketAndOutcome(user, market, yesOutcome))
                .thenReturn(Optional.of(position));

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.SELL, "3.0000")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient position to sell");

        verify(walletRepository, never()).save(any());
        verify(tradeRepository, never()).save(any());
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void successfulSellCreditsWalletAndReducesOwnedPosition() {
        stubRequiredEntities();
        yesOutcome.setShares(new BigDecimal("5.0000"));

        Position position = new Position();
        position.setUser(user);
        position.setMarket(market);
        position.setOutcome(yesOutcome);
        position.setQuantity(new BigDecimal("5.0000"));

        when(positionRepository.findByUserAndMarketAndOutcome(user, market, yesOutcome))
                .thenReturn(Optional.of(position));
        when(marketOutcomeRepository.findByMarketAndName(market, "YES"))
                .thenReturn(Optional.of(yesOutcome));
        when(marketOutcomeRepository.findByMarketAndName(market, "NO"))
                .thenReturn(Optional.of(noOutcome));

        TradeResponse response = tradeService.executeTrade(request(TradeType.SELL, "3.0000"));

        assertThat(response.getTotalCost()).isEqualByComparingTo("1.5000");
        assertThat(response.getWalletBalanceAfterTrade()).isEqualByComparingTo("101.5000");
        assertThat(response.getPositionQuantityAfterTrade()).isEqualByComparingTo("2.0000");
        assertThat(yesOutcome.getShares()).isEqualByComparingTo("2.0000");
        assertThat(yesOutcome.getCurrentPrice()).isEqualByComparingTo("0.5050");
        assertThat(noOutcome.getCurrentPrice()).isEqualByComparingTo("0.4950");

        ArgumentCaptor<PriceHistory> historyCaptor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getYesPrice()).isEqualByComparingTo("0.5050");
        assertThat(historyCaptor.getValue().getNoPrice()).isEqualByComparingTo("0.4950");
        verify(tradeRepository).save(any());
        verify(walletTransactionRepository).save(any());
    }

    @Test
    void decimalQuantityIsRejected() {
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "1.0002")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Quantity must be a positive whole number");

        verify(walletRepository, never()).save(any());
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void zeroAndNegativeQuantitiesAreRejected() {
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "0")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Quantity must be a positive whole number");
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "-1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Quantity must be a positive whole number");

        verify(walletRepository, never()).save(any());
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void buyingInClosedMarketIsRejected() {
        market.setStatus(MarketStatus.CLOSED);
        when(marketRepository.findById(10L)).thenReturn(Optional.of(market));

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Market is closed for trading");

        verify(tradeRepository, never()).save(any());
    }

    @Test
    void sellingInClosedMarketIsRejected() {
        market.setStatus(MarketStatus.CLOSED);
        when(marketRepository.findById(10L)).thenReturn(Optional.of(market));

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.SELL, "1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Market is closed for trading");

        verify(tradeRepository, never()).save(any());
    }

    @Test
    void buyingInCancelledMarketIsRejected() {
        market.setStatus(MarketStatus.CANCELLED);
        when(marketRepository.findById(10L)).thenReturn(Optional.of(market));

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Market is closed for trading");

        verify(tradeRepository, never()).save(any());
    }

    private void stubRequiredEntities() {
        when(marketRepository.findById(10L)).thenReturn(Optional.of(market));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(20L)).thenReturn(Optional.of(wallet));
        when(marketOutcomeRepository.findById(30L)).thenReturn(Optional.of(yesOutcome));
    }

    private MarketOutcome outcome(Long id, String name) {
        MarketOutcome outcome = new MarketOutcome();
        outcome.setId(id);
        outcome.setMarket(market);
        outcome.setName(name);
        outcome.setShares(BigDecimal.ZERO);
        outcome.setCurrentPrice(new BigDecimal("0.5000"));
        return outcome;
    }

    private TradeRequest request(TradeType type, String quantity) {
        TradeRequest request = new TradeRequest();
        request.setUserId(user.getId());
        request.setMarketId(market.getId());
        request.setOutcomeId(yesOutcome.getId());
        request.setQuantity(new BigDecimal(quantity));
        request.setType(type);
        return request;
    }
}
