package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.PositionResponse;
import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.repository.MarketOutcomeRepository;
import com.virtualmarket.polymarket.repository.MarketRepository;
import com.virtualmarket.polymarket.repository.PositionRepository;
import com.virtualmarket.polymarket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PositionService {

    private final PositionRepository positionRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final MarketOutcomeRepository marketOutcomeRepository;

    public PositionService(
            PositionRepository positionRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            MarketOutcomeRepository marketOutcomeRepository
    ) {
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.marketOutcomeRepository = marketOutcomeRepository;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> getUserPositions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        return positionRepository.findByUser(user).stream()
                .filter(position -> position.getQuantity() != null)
                .filter(position -> position.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .filter(position -> position.getMarket().getStatus() == MarketStatus.OPEN)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> getMarketPositions(Long marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Market not found"));

        return positionRepository.findByMarket(market).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PositionResponse mapToResponse(Position position) {
        MarketOutcome outcome = position.getOutcome();
        BigDecimal quantity = position.getQuantity();
        BigDecimal currentPrice = outcome.getCurrentPrice();
        BigDecimal currentValue = quantity.multiply(currentPrice);
        BigDecimal unrealizedPnL = BigDecimal.ZERO; // TODO: calculate based on average cost basis when stored

        PositionResponse response = new PositionResponse();
        response.setPositionId(position.getId());
        response.setUserId(position.getUser().getId());
        response.setMarketId(position.getMarket().getId());
        response.setMarketTitle(position.getMarket().getTitle());
        response.setMarketStatus(position.getMarket().getStatus());
        response.setOutcomeId(outcome.getId());
        response.setOutcomeName(outcome.getName());
        response.setQuantity(quantity);
        response.setCurrentPrice(currentPrice);
        response.setCurrentValue(currentValue);
        response.setUnrealizedPnL(unrealizedPnL);

        return response;
    }
}
