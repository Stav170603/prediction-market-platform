package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.MarketResolvedEvent;
import com.virtualmarket.polymarket.dto.ResolutionRequest;
import com.virtualmarket.polymarket.dto.ResolutionResponse;
import com.virtualmarket.polymarket.dto.UserPortfolioUpdatedEvent;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.MarketResolution;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.entity.WalletTransaction;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.repository.MarketOutcomeRepository;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.MarketResolutionRepository;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import com.virtualmarket.polymarket.repository.WalletRepository;
import com.virtualmarket.polymarket.repository.WalletTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketResolutionService {

    private final MarketRepository marketRepository;
    private final UserRepository userRepository;
    private final MarketOutcomeRepository marketOutcomeRepository;
    private final MarketResolutionRepository marketResolutionRepository;
    private final PositionRepository positionRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final RealTimeEventService realTimeEventService;

    public MarketResolutionService(
            MarketRepository marketRepository,
            UserRepository userRepository,
            MarketOutcomeRepository marketOutcomeRepository,
            MarketResolutionRepository marketResolutionRepository,
            PositionRepository positionRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            RealTimeEventService realTimeEventService
    ) {
        this.marketRepository = marketRepository;
        this.userRepository = userRepository;
        this.marketOutcomeRepository = marketOutcomeRepository;
        this.marketResolutionRepository = marketResolutionRepository;
        this.positionRepository = positionRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.realTimeEventService = realTimeEventService;
    }

    @Transactional
    public ResolutionResponse resolveMarket(ResolutionRequest request) {
        Long adminUserId = request.getAdminUserId();
        Long marketId = request.getMarketId();
        Long winningOutcomeId = request.getWinningOutcomeId();

        if (adminUserId == null || marketId == null || winningOutcomeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "adminUserId, marketId and winningOutcomeId are required");
        }

        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Market not found"));

        if (market.getStatus() == MarketStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled market cannot be resolved");
        }
        if (market.getStatus() == MarketStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market has already been resolved");
        }

        if (market.getResolutionDate() == null || LocalDateTime.now().isBefore(market.getResolutionDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market resolution date has not been reached");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authorized to resolve markets");
        }

        MarketOutcome winningOutcome = marketOutcomeRepository.findById(winningOutcomeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Winning outcome not found"));

        if (!winningOutcome.getMarket().getId().equals(market.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Winning outcome does not belong to the market");
        }

        if (marketResolutionRepository.findByMarket(market).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market has already been resolved");
        }

        // Settlement rules:
        // - Winning positions receive payout = quantity * 1.00 virtual points
        // - Losing positions receive 0
        // - Double settlement is prevented by checking if a MarketResolution already exists for the market
        List<Position> positions = positionRepository.findByMarket(market);
        updateReliabilityScores(positions, winningOutcome);
        int usersPaid = 0;
        BigDecimal totalPayout = BigDecimal.ZERO;

        for (Position position : positions) {
            if (position.getOutcome().getId().equals(winningOutcome.getId())) {
                BigDecimal payout = position.getQuantity();
                if (payout.compareTo(BigDecimal.ZERO) > 0) {
                    Wallet wallet = walletRepository.findByUserId(position.getUser().getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found for user"));
                    wallet.setBalance(wallet.getBalance().add(payout));
                    walletRepository.save(wallet);

                    WalletTransaction transaction = new WalletTransaction();
                    transaction.setUser(position.getUser());
                    transaction.setType("MARKET_PAYOUT");
                    transaction.setAmount(payout);
                    transaction.setBalanceAfter(wallet.getBalance());
                    transaction.setDescription("Payout for resolved market");
                    walletTransactionRepository.save(transaction);

                    realTimeEventService.publishAfterCommit(
                            "user-portfolio-updated",
                            new UserPortfolioUpdatedEvent(position.getUser().getId(), wallet.getBalance(), LocalDateTime.now())
                    );

                    totalPayout = totalPayout.add(payout);
                    usersPaid++;
                }
            }
        }

        MarketResolution resolution = new MarketResolution();
        resolution.setMarket(market);
        resolution.setWinningOutcome(winningOutcome);
        marketResolutionRepository.save(resolution);

        market.setStatus(MarketStatus.RESOLVED);
        marketRepository.save(market);
        LocalDateTime resolvedAt = LocalDateTime.now();
        realTimeEventService.publishAfterCommit(
                "market-resolved",
                new MarketResolvedEvent(market.getId(), winningOutcome.getName(), resolvedAt)
        );

        ResolutionResponse response = new ResolutionResponse();
        response.setMarketId(market.getId());
        response.setMarketTitle(market.getTitle());
        response.setWinningOutcomeId(winningOutcome.getId());
        response.setWinningOutcomeName(winningOutcome.getName());
        response.setTotalUsersPaid(usersPaid);
        response.setTotalPayoutAmount(totalPayout);

        return response;
    }

    private void updateReliabilityScores(List<Position> positions, MarketOutcome winningOutcome) {
        /*
         * Each market counts as one prediction for a participating user, regardless of
         * position size. Holding any positive quantity of the winning outcome counts
         * as correct. Reliability is lifetime accuracy:
         *     correct resolved predictions / total resolved predictions * 100
         * Users with only losing positions therefore receive an incorrect result,
         * which leaves a zero score unchanged or lowers an established score.
         */
        Map<User, Boolean> predictionResults = new LinkedHashMap<>();
        for (Position position : positions) {
            if (position.getQuantity() == null || position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            boolean holdsWinningOutcome = position.getOutcome().getId().equals(winningOutcome.getId());
            predictionResults.merge(position.getUser(), holdsWinningOutcome, Boolean::logicalOr);
        }

        predictionResults.forEach((user, correct) -> {
            long totalPredictions = user.getTotalPredictions() + 1;
            long correctPredictions = user.getCorrectPredictions() + (correct ? 1 : 0);
            BigDecimal reliabilityScore = BigDecimal.valueOf(correctPredictions)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalPredictions), 2, RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO)
                    .min(BigDecimal.valueOf(100));

            user.setTotalPredictions(totalPredictions);
            user.setCorrectPredictions(correctPredictions);
            user.setReliabilityScore(reliabilityScore);
            userRepository.save(user);
        });
    }
}
