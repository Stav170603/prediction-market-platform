package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.ResolutionRequest;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.repository.MarketOutcomeRepository;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.MarketResolutionRepository;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import com.virtualmarket.polymarket.repository.WalletRepository;
import com.virtualmarket.polymarket.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketResolutionServiceTest {

    @Mock
    private MarketRepository marketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MarketOutcomeRepository marketOutcomeRepository;
    @Mock
    private MarketResolutionRepository marketResolutionRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private RealTimeEventService realTimeEventService;

    private MarketResolutionService service;
    private Market market;
    private MarketOutcome winningOutcome;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new MarketResolutionService(
                marketRepository,
                userRepository,
                marketOutcomeRepository,
                marketResolutionRepository,
                positionRepository,
                walletRepository,
                walletTransactionRepository,
                realTimeEventService
        );

        market = new Market();
        market.setId(10L);
        market.setStatus(MarketStatus.OPEN);
        market.setResolutionDate(LocalDateTime.now().minusMinutes(1));

        winningOutcome = outcome(20L, "YES");

        admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        when(marketRepository.findById(10L)).thenReturn(Optional.of(market));
    }

    @Test
    void settlementImprovesWinnerAndDecreasesLosingUsersReliability() {
        stubResolutionDependencies();
        User winner = user(2L, 1, 2, "50.00");
        User loser = user(3L, 1, 1, "100.00");
        MarketOutcome losingOutcome = outcome(21L, "NO");
        when(positionRepository.findByMarket(market)).thenReturn(List.of(
                position(winner, winningOutcome, "4.0000"),
                position(loser, losingOutcome, "3.0000")
        ));
        when(marketResolutionRepository.findByMarket(market)).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(wallet(winner)));

        service.resolveMarket(request());

        assertThat(winner.getCorrectPredictions()).isEqualTo(2);
        assertThat(winner.getTotalPredictions()).isEqualTo(3);
        assertThat(winner.getReliabilityScore()).isEqualByComparingTo("66.67");

        assertThat(loser.getCorrectPredictions()).isEqualTo(1);
        assertThat(loser.getTotalPredictions()).isEqualTo(2);
        assertThat(loser.getReliabilityScore()).isEqualByComparingTo("50.00");
    }

    @Test
    void resolvingSameMarketTwiceDoesNotDoubleCountReliability() {
        stubResolutionDependencies();
        User winner = user(2L, 0, 0, "0.00");
        when(positionRepository.findByMarket(market))
                .thenReturn(List.of(position(winner, winningOutcome, "2.0000")));
        when(marketResolutionRepository.findByMarket(market)).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(wallet(winner)));

        service.resolveMarket(request());

        assertThatThrownBy(() -> service.resolveMarket(request()))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(winner.getCorrectPredictions()).isEqualTo(1);
        assertThat(winner.getTotalPredictions()).isEqualTo(1);
        assertThat(winner.getReliabilityScore()).isEqualByComparingTo("100.00");
    }

    @Test
    void resolvingCancelledMarketIsRejected() {
        market.setStatus(MarketStatus.CANCELLED);

        assertThatThrownBy(() -> service.resolveMarket(request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cancelled market cannot be resolved");
    }

    private void stubResolutionDependencies() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(marketOutcomeRepository.findById(20L)).thenReturn(Optional.of(winningOutcome));
    }

    private ResolutionRequest request() {
        ResolutionRequest request = new ResolutionRequest();
        request.setAdminUserId(1L);
        request.setMarketId(10L);
        request.setWinningOutcomeId(20L);
        return request;
    }

    private User user(Long id, long correct, long total, String score) {
        User user = new User();
        user.setId(id);
        user.setCorrectPredictions(correct);
        user.setTotalPredictions(total);
        user.setReliabilityScore(new BigDecimal(score));
        return user;
    }

    private MarketOutcome outcome(Long id, String name) {
        MarketOutcome outcome = new MarketOutcome();
        outcome.setId(id);
        outcome.setName(name);
        outcome.setMarket(market);
        return outcome;
    }

    private Position position(User user, MarketOutcome outcome, String quantity) {
        Position position = new Position();
        position.setUser(user);
        position.setMarket(market);
        position.setOutcome(outcome);
        position.setQuantity(new BigDecimal(quantity));
        return position;
    }

    private Wallet wallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("100.0000"));
        return wallet;
    }
}
