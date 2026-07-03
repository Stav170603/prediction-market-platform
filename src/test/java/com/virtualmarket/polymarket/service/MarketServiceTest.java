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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock
    private MarketRepository marketRepository;
    @Mock
    private MarketOutcomeRepository marketOutcomeRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private MarketService marketService;

    @Test
    void adminCanCreateMarketWithInitialOutcomes() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(marketRepository.save(any(Market.class))).thenAnswer(invocation -> {
            Market market = invocation.getArgument(0);
            market.setId(50L);
            return market;
        });

        CreateMarketRequest request = new CreateMarketRequest();
        request.setAdminUserId(1L);
        request.setTitle("  Will the test pass?  ");
        request.setDescription("  A testable market description.  ");
        request.setCategory("  Technology  ");
        request.setResolutionSource("  Official test results  ");
        request.setTradingCloseDate(LocalDateTime.now().plusDays(1));
        request.setResolutionDate(LocalDateTime.now().plusDays(2));
        request.setLiquidity(new BigDecimal("250.0000"));

        MarketResponse response = marketService.createMarket(request);

        assertThat(response.getMarketId()).isEqualTo(50L);
        assertThat(response.getTitle()).isEqualTo("Will the test pass?");
        assertThat(response.getCategory()).isEqualTo("Technology");
        assertThat(response.getDescription()).isEqualTo("A testable market description.");
        assertThat(response.getResolutionSource()).isEqualTo("Official test results");
        assertThat(response.getStatus()).isEqualTo(MarketStatus.OPEN);
        assertThat(response.getYesPrice()).isEqualByComparingTo("0.5000");
        assertThat(response.getNoPrice()).isEqualByComparingTo("0.5000");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MarketOutcome>> outcomesCaptor = ArgumentCaptor.forClass(List.class);
        verify(marketOutcomeRepository).saveAll(outcomesCaptor.capture());
        assertThat(outcomesCaptor.getValue())
                .extracting(MarketOutcome::getName)
                .containsExactly("YES", "NO");
    }

    @Test
    void priceHistoryIsReturnedInRepositoryOrder() {
        Market market = new Market();
        market.setId(50L);
        when(marketRepository.findById(50L)).thenReturn(Optional.of(market));

        PriceHistory first = history(market, "0.4500", "0.5500", LocalDateTime.of(2026, 1, 1, 10, 0));
        PriceHistory second = history(market, "0.6000", "0.4000", LocalDateTime.of(2026, 1, 1, 11, 0));
        when(priceHistoryRepository.findByMarketOrderByCreatedAtAsc(market))
                .thenReturn(List.of(first, second));

        List<PriceHistoryResponse> result = marketService.getPriceHistory(50L);

        assertThat(result)
                .extracting(PriceHistoryResponse::getYesPrice)
                .containsExactly(new BigDecimal("0.4500"), new BigDecimal("0.6000"));
        assertThat(result)
                .extracting(PriceHistoryResponse::getTimestamp)
                .isSorted();
    }

    @Test
    void descriptionAndResolutionSourceAreRequired() {
        CreateMarketRequest request = new CreateMarketRequest();
        request.setAdminUserId(1L);
        request.setTitle("Will required fields be enforced?");
        request.setCategory("Testing");
        request.setTradingCloseDate(LocalDateTime.now().plusDays(1));
        request.setResolutionDate(LocalDateTime.now().plusDays(2));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> marketService.createMarket(request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Description must not be blank");

        request.setDescription("Required description");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> marketService.createMarket(request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Resolution source must not be blank");
    }

    private PriceHistory history(Market market, String yes, String no, LocalDateTime timestamp) {
        PriceHistory history = new PriceHistory();
        history.setMarket(market);
        history.setYesPrice(new BigDecimal(yes));
        history.setNoPrice(new BigDecimal(no));
        history.setCreatedAt(timestamp);
        return history;
    }
}
