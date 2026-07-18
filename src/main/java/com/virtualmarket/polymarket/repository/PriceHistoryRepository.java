package com.virtualmarket.polymarket.repository;

import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByMarketOrderByCreatedAtAsc(Market market);

    List<PriceHistory> findByMarketAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            Market market, LocalDateTime createdAt);
}
