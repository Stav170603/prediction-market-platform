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
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MarketRepository marketRepository;
    @Mock
    private MarketOutcomeRepository marketOutcomeRepository;
    @InjectMocks
    private PositionService positionService;

    @Test
    void activePortfolioIncludesOpenPositionsAndExcludesResolvedPositions() {
        User user = new User();
        user.setId(7L);
        Position openPosition = position(user, 10L, MarketStatus.OPEN, "5");
        Position resolvedPosition = position(user, 11L, MarketStatus.RESOLVED, "8");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(positionRepository.findByUser(user)).thenReturn(List.of(openPosition, resolvedPosition));

        List<PositionResponse> result = positionService.getUserPositions(7L);

        assertThat(result)
                .extracting(PositionResponse::getMarketId)
                .containsExactly(10L);
        assertThat(result.getFirst().getMarketStatus()).isEqualTo(MarketStatus.OPEN);
    }

    private Position position(User user, Long marketId, MarketStatus status, String quantity) {
        Market market = new Market();
        market.setId(marketId);
        market.setTitle("Market " + marketId);
        market.setStatus(status);

        MarketOutcome outcome = new MarketOutcome();
        outcome.setId(marketId * 10);
        outcome.setMarket(market);
        outcome.setName("YES");
        outcome.setCurrentPrice(new BigDecimal("0.5000"));

        Position position = new Position();
        position.setId(marketId * 100);
        position.setUser(user);
        position.setMarket(market);
        position.setOutcome(outcome);
        position.setQuantity(new BigDecimal(quantity));
        return position;
    }
}
