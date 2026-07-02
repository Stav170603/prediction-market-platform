package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.LeaderboardResponse;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.TradeRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import com.virtualmarket.polymarket.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LeaderboardService {

    private static final int MONEY_SCALE = 4;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;

    public LeaderboardService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PositionRepository positionRepository,
            TradeRepository tradeRepository
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getLeaderboard() {
        List<LeaderboardEntry> entries = userRepository.findAll().stream()
                .map(this::toEntry)
                .sorted(Comparator.comparing(LeaderboardEntry::reliabilityScore).reversed()
                        .thenComparing(Comparator.comparing(LeaderboardEntry::portfolioValue).reversed()))
                .toList();

        AtomicInteger rank = new AtomicInteger(1);
        return entries.stream()
                .map(entry -> new LeaderboardResponse(
                        rank.getAndIncrement(),
                        entry.user().getId(),
                        entry.user().getUsername(),
                        entry.user().getEmail(),
                        entry.walletBalance(),
                        entry.openPositions(),
                        entry.totalTrades(),
                        entry.portfolioValue(),
                        entry.reliabilityScore()
                ))
                .toList();
    }

    private LeaderboardEntry toEntry(User user) {
        BigDecimal walletBalance = walletRepository.findByUser(user)
                .map(wallet -> normalizeMoney(wallet.getBalance()))
                .orElse(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));

        List<Position> positions = positionRepository.findByUser(user);
        List<Position> openPositions = positions.stream()
                .filter(position -> position.getQuantity() != null)
                .filter(position -> position.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal openPositionValue = openPositions.stream()
                .map(this::calculatePositionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal portfolioValue = normalizeMoney(walletBalance.add(openPositionValue));
        long totalTrades = tradeRepository.findByUser(user).size();

        return new LeaderboardEntry(
                user,
                walletBalance,
                openPositions.size(),
                totalTrades,
                portfolioValue,
                user.getReliabilityScore() == null ? BigDecimal.ZERO : user.getReliabilityScore()
        );
    }

    private BigDecimal calculatePositionValue(Position position) {
        BigDecimal quantity = position.getQuantity() == null ? BigDecimal.ZERO : position.getQuantity();
        BigDecimal currentPrice = position.getOutcome().getCurrentPrice() == null
                ? BigDecimal.ZERO
                : position.getOutcome().getCurrentPrice();

        return quantity.multiply(currentPrice);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record LeaderboardEntry(
            User user,
            BigDecimal walletBalance,
            long openPositions,
            long totalTrades,
            BigDecimal portfolioValue,
            BigDecimal reliabilityScore
    ) {
    }
}
