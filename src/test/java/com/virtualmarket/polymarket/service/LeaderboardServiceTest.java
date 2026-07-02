package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.LeaderboardResponse;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.TradeRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
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
class LeaderboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private TradeRepository tradeRepository;
    @InjectMocks
    private LeaderboardService leaderboardService;

    @Test
    void leaderboardIncludesReliabilityAndRanksItBeforePortfolioValue() {
        User accurateUser = user(1L, "accurate", "80.00");
        User wealthyUser = user(2L, "wealthy", "60.00");
        when(userRepository.findAll()).thenReturn(List.of(wealthyUser, accurateUser));
        stubEntry(accurateUser, "100.0000");
        stubEntry(wealthyUser, "1000.0000");

        List<LeaderboardResponse> leaderboard = leaderboardService.getLeaderboard();

        assertThat(leaderboard)
                .extracting(LeaderboardResponse::getUsername)
                .containsExactly("accurate", "wealthy");
        assertThat(leaderboard.getFirst().getReliabilityScore()).isEqualByComparingTo("80.00");
        assertThat(leaderboard.get(1).getReliabilityScore()).isEqualByComparingTo("60.00");
        assertThat(leaderboard.getFirst().getRank()).isEqualTo(1);
    }

    private void stubEntry(User user, String balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal(balance));
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(positionRepository.findByUser(user)).thenReturn(List.of());
        when(tradeRepository.findByUser(user)).thenReturn(List.of());
    }

    private User user(Long id, String username, String reliabilityScore) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setReliabilityScore(new BigDecimal(reliabilityScore));
        return user;
    }
}
