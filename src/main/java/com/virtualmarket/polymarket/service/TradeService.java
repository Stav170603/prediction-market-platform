package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.MarketPriceUpdatedEvent;
import com.virtualmarket.polymarket.dto.TradeRequest;
import com.virtualmarket.polymarket.dto.TradeResponse;
import com.virtualmarket.polymarket.dto.TradeCreatedEvent;
import com.virtualmarket.polymarket.dto.UserPortfolioUpdatedEvent;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.Trade;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.entity.WalletTransaction;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.TradeType;
import com.virtualmarket.polymarket.repository.MarketOutcomeRepository;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.TradeRepository;
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
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TradeService {

    private static final int PRICE_SCALE = 4;
    private static final String YES_OUTCOME = "YES";
    private static final String NO_OUTCOME = "NO";

    private final MarketRepository marketRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MarketOutcomeRepository marketOutcomeRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PricingService pricingService;
    private final RealTimeEventService realTimeEventService;

    public TradeService(
            MarketRepository marketRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            MarketOutcomeRepository marketOutcomeRepository,
            PositionRepository positionRepository,
            TradeRepository tradeRepository,
            WalletTransactionRepository walletTransactionRepository,
            PricingService pricingService,
            RealTimeEventService realTimeEventService
    ) {
        this.marketRepository = marketRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.marketOutcomeRepository = marketOutcomeRepository;
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.pricingService = pricingService;
        this.realTimeEventService = realTimeEventService;
    }

    public TradeResponse buyShares(TradeRequest request) {
        request.setType(TradeType.BUY);
        return executeTrade(request);
    }

    public TradeResponse sellShares(TradeRequest request) {
        request.setType(TradeType.SELL);
        return executeTrade(request);
    }

    public List<TradeResponse> getTradesByUser(Long userId) {
        List<Trade> trades = tradeRepository.findByUserId(userId);
        return trades.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TradeResponse> getTradesByMarket(Long marketId) {
        List<Trade> trades = tradeRepository.findByMarketId(marketId);
        return trades.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public TradeResponse executeTrade(TradeRequest request) {
        Long userId = request.getUserId();
        Long marketId = request.getMarketId();
        Long outcomeId = request.getOutcomeId();
        BigDecimal quantity = request.getQuantity();
        TradeType type = request.getType();

        if (quantity == null
                || quantity.compareTo(BigDecimal.ZERO) <= 0
                || quantity.stripTrailingZeros().scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be a positive whole number");
        }

        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Market not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        MarketOutcome outcome = marketOutcomeRepository.findById(outcomeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outcome not found"));

        if (!outcome.getMarket().getId().equals(market.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outcome does not belong to market");
        }

        if (market.getStatus() != MarketStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market is not open for trading");
        }

        if (market.getTradingCloseDate() != null && LocalDateTime.now().isAfter(market.getTradingCloseDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trading window has closed");
        }

        BigDecimal price = outcome.getCurrentPrice().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = price.multiply(quantity).setScale(PRICE_SCALE, RoundingMode.HALF_UP);

        Position position = positionRepository.findByUserAndMarketAndOutcome(user, market, outcome)
                .orElseGet(() -> {
                    Position p = new Position();
                    p.setUser(user);
                    p.setMarket(market);
                    p.setOutcome(outcome);
                    p.setQuantity(BigDecimal.ZERO);
                    return p;
                });

        if (type == TradeType.BUY) {
            // check funds
            if (wallet.getBalance().compareTo(total) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
            }
            // deduct wallet
            wallet.setBalance(wallet.getBalance().subtract(total));
            // increase position
            position.setQuantity(position.getQuantity().add(quantity));
            // increase outcome shares
            outcome.setShares(outcome.getShares().add(quantity));

        } else if (type == TradeType.SELL) {
            if (position.getQuantity().compareTo(quantity) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient position to sell");
            }
            // decrease position
            position.setQuantity(position.getQuantity().subtract(quantity));
            // credit wallet
            wallet.setBalance(wallet.getBalance().add(total));
            // decrease outcome shares but do not allow negative
            BigDecimal newShares = outcome.getShares().subtract(quantity);
            if (newShares.compareTo(BigDecimal.ZERO) < 0) {
                outcome.setShares(BigDecimal.ZERO);
            } else {
                outcome.setShares(newShares);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trade type");
        }

        // persist wallet, position, outcome
        walletRepository.save(wallet);
        positionRepository.save(position);
        marketOutcomeRepository.save(outcome);

        // create trade record
        Trade trade = new Trade();
        trade.setUser(user);
        trade.setMarket(market);
        trade.setOutcome(outcome);
        trade.setType(type);
        trade.setQuantity(quantity);
        trade.setPricePerShare(price);
        trade.setTotalCost(total);
        trade.setBalanceAfterTrade(wallet.getBalance());
        tradeRepository.save(trade);

        // wallet transaction
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(type.name());
        tx.setAmount(total);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setDescription(type == TradeType.BUY ? "Buy shares" : "Sell shares");
        walletTransactionRepository.save(tx);

        // update market prices
        pricingService.updateMarketPrices(market);
        MarketOutcome yesOutcome = marketOutcomeRepository.findByMarketAndName(market, YES_OUTCOME)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "YES outcome not found"));
        MarketOutcome noOutcome = marketOutcomeRepository.findByMarketAndName(market, NO_OUTCOME)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO outcome not found"));
        LocalDateTime updatedAt = LocalDateTime.now();

        realTimeEventService.publishAfterCommit(
                "market-price-updated",
                new MarketPriceUpdatedEvent(market.getId(), yesOutcome.getCurrentPrice(), noOutcome.getCurrentPrice(), updatedAt)
        );
        realTimeEventService.publishAfterCommit(
                "trade-created",
                new TradeCreatedEvent(
                        trade.getId(),
                        market.getId(),
                        user.getId(),
                        outcome.getName(),
                        type,
                        quantity,
                        price,
                        total,
                        trade.getCreatedAt() != null ? trade.getCreatedAt() : updatedAt
                )
        );
        realTimeEventService.publishAfterCommit(
                "user-portfolio-updated",
                new UserPortfolioUpdatedEvent(user.getId(), wallet.getBalance(), updatedAt)
        );

        TradeResponse response = new TradeResponse();
        response.setTradeId(trade.getId());
        response.setUserId(user.getId());
        response.setMarketId(market.getId());
        response.setOutcomeId(outcome.getId());
        response.setOutcomeName(outcome.getName());
        response.setType(type);
        response.setQuantity(quantity);
        response.setPrice(price);
        response.setTotalCost(total);
        response.setWalletBalanceAfterTrade(wallet.getBalance());
        response.setPositionQuantityAfterTrade(position.getQuantity());
        response.setCreatedAt(trade.getCreatedAt());

        return response;
    }

    private TradeResponse mapToResponse(Trade trade) {
        TradeResponse r = new TradeResponse();
        r.setTradeId(trade.getId());
        r.setUserId(trade.getUser().getId());
        r.setMarketId(trade.getMarket().getId());
        r.setOutcomeId(trade.getOutcome().getId());
        r.setOutcomeName(trade.getOutcome().getName());
        r.setType(trade.getType());
        r.setQuantity(trade.getQuantity());
        r.setPrice(trade.getPricePerShare());
        r.setTotalCost(trade.getTotalCost());
        r.setWalletBalanceAfterTrade(trade.getBalanceAfterTrade());
        r.setCreatedAt(trade.getCreatedAt());
        // try to get current position quantity
        Optional<Position> pos = positionRepository.findByUserAndMarketAndOutcome(
                trade.getUser(), trade.getMarket(), trade.getOutcome()
        );
        pos.ifPresent(position -> r.setPositionQuantityAfterTrade(position.getQuantity()));
        return r;
    }
}
