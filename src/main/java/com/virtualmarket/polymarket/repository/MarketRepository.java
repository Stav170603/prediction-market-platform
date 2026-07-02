package com.virtualmarket.polymarket.repository;

import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.enums.MarketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface MarketRepository extends JpaRepository<Market, Long> {
    List<Market> findByStatus(MarketStatus status);

    List<Market> findByStatusAndTradingCloseDateLessThanEqual(
            MarketStatus status,
            LocalDateTime tradingCloseDate
    );

    List<Market> findByCategory(String category);

    long countByStatus(MarketStatus status);
}
