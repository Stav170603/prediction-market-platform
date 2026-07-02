package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.repository.MarketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketLifecycleServiceTest {

    @Mock
    private MarketRepository marketRepository;
    @InjectMocks
    private MarketLifecycleService marketLifecycleService;

    @Test
    void automaticallyClosesMarketsWhoseTradingCloseDateWasReached() {
        Market expiredMarket = market(10L, MarketStatus.OPEN);
        when(marketRepository.findByStatusAndTradingCloseDateLessThanEqual(
                eq(MarketStatus.OPEN),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(List.of(expiredMarket));

        marketLifecycleService.closeExpiredMarkets();

        assertThat(expiredMarket.getStatus()).isEqualTo(MarketStatus.CLOSED);
        ArgumentCaptor<List<Market>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(expiredMarket);
    }

    @Test
    void adminCanCancelAnOpenMarket() {
        Market market = market(11L, MarketStatus.OPEN);
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        when(marketRepository.findById(11L)).thenReturn(java.util.Optional.of(market));

        marketLifecycleService.cancelMarket(11L, admin);

        assertThat(market.getStatus()).isEqualTo(MarketStatus.CANCELLED);
        verify(marketRepository).save(market);
    }

    private Market market(Long id, MarketStatus status) {
        Market market = new Market();
        market.setId(id);
        market.setStatus(status);
        return market;
    }
}
