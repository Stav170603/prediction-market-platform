package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.CreateMarketRequest;
import com.virtualmarket.polymarket.dto.MarketResponse;
import com.virtualmarket.polymarket.dto.PriceHistoryResponse;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.PriceHistory;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.repository.MarketOutcomeRepository;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.PriceHistoryRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class MarketService {

    private static final String YES_OUTCOME = "YES";
    private static final String NO_OUTCOME = "NO";
    private static final BigDecimal DEFAULT_LIQUIDITY = new BigDecimal("100.0000");
    private static final BigDecimal INITIAL_PRICE = new BigDecimal("0.5000");
    private static final BigDecimal INITIAL_SHARES = BigDecimal.ZERO;

    private final MarketRepository marketRepository;
    private final MarketOutcomeRepository marketOutcomeRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final UserRepository userRepository;

    public MarketService(
            MarketRepository marketRepository,
            MarketOutcomeRepository marketOutcomeRepository,
            PriceHistoryRepository priceHistoryRepository,
            UserRepository userRepository
    ) {
        this.marketRepository = marketRepository;
        this.marketOutcomeRepository = marketOutcomeRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MarketResponse createMarket(CreateMarketRequest request) {
        validateCreateMarketRequest(request);

        User admin = userRepository.findById(request.getAdminUserId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Admin user not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Only ADMIN users can create markets");
        }

        Market market = new Market();
        market.setTitle(request.getTitle().trim());
        market.setDescription(request.getDescription().trim());
        market.setCategory(request.getCategory().trim());
        market.setTradingCloseDate(request.getTradingCloseDate());
        market.setResolutionDate(request.getResolutionDate());
        market.setResolutionSource(request.getResolutionSource().trim());
        market.setStatus(MarketStatus.OPEN);
        market.setLiquidity(request.getLiquidity() != null ? request.getLiquidity() : DEFAULT_LIQUIDITY);

        Market savedMarket = marketRepository.save(market);

        MarketOutcome yesOutcome = createOutcome(savedMarket, YES_OUTCOME);
        MarketOutcome noOutcome = createOutcome(savedMarket, NO_OUTCOME);
        marketOutcomeRepository.saveAll(List.of(yesOutcome, noOutcome));

        return toMarketResponse(savedMarket, yesOutcome, noOutcome);
    }

    private void validateCreateMarketRequest(CreateMarketRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Title must not be blank");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Category must not be blank");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Description must not be blank");
        }
        if (request.getResolutionSource() == null || request.getResolutionSource().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Resolution source must not be blank");
        }
        if (request.getTradingCloseDate() == null || !request.getTradingCloseDate().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "Trading close date must be in the future");
        }
        if (request.getResolutionDate() == null || !request.getResolutionDate().isAfter(request.getTradingCloseDate())) {
            throw new ResponseStatusException(BAD_REQUEST, "Resolution date must be after trading close date");
        }
        if (request.getLiquidity() != null && request.getLiquidity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Liquidity must be greater than zero");
        }
    }

    @Transactional(readOnly = true)
    public List<MarketResponse> getAllMarkets() {
        return marketRepository.findAll().stream()
                .map(this::toMarketResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MarketResponse getMarketById(Long id) {
        Market market = marketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Market not found"));
        return toMarketResponse(market);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Market not found"));

        return priceHistoryRepository.findByMarketOrderByCreatedAtAsc(market).stream()
                .map(this::toPriceHistoryResponse)
                .toList();
    }

    private MarketOutcome createOutcome(Market market, String name) {
        MarketOutcome outcome = new MarketOutcome();
        outcome.setMarket(market);
        outcome.setName(name);
        outcome.setShares(INITIAL_SHARES);
        outcome.setCurrentPrice(INITIAL_PRICE);
        return outcome;
    }

    private MarketResponse toMarketResponse(Market market) {
        MarketOutcome yesOutcome = marketOutcomeRepository.findByMarketAndName(market, YES_OUTCOME)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "YES outcome not found"));
        MarketOutcome noOutcome = marketOutcomeRepository.findByMarketAndName(market, NO_OUTCOME)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "NO outcome not found"));

        return toMarketResponse(market, yesOutcome, noOutcome);
    }

    private MarketResponse toMarketResponse(Market market, MarketOutcome yesOutcome, MarketOutcome noOutcome) {
        return new MarketResponse(
                market.getId(),
                market.getTitle(),
                market.getDescription(),
                market.getCategory(),
                market.getStatus(),
                market.getTradingCloseDate(),
                market.getResolutionDate(),
                market.getResolutionSource(),
                yesOutcome.getId(),
                noOutcome.getId(),
                yesOutcome.getCurrentPrice(),
                noOutcome.getCurrentPrice()
        );
    }

    private PriceHistoryResponse toPriceHistoryResponse(PriceHistory priceHistory) {
        return new PriceHistoryResponse(
                priceHistory.getMarket().getId(),
                priceHistory.getCreatedAt(),
                priceHistory.getYesPrice(),
                priceHistory.getNoPrice()
        );
    }
}
