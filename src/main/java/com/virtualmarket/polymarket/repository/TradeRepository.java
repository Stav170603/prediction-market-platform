package com.virtualmarket.polymarket.repository;

import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.Trade;
import com.virtualmarket.polymarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUser(User user);

    List<Trade> findByMarket(Market market);
    List<Trade> findByUserId(Long userId);

    List<Trade> findByMarketId(Long marketId);

    long countByUser(User user);

    long countByMarketId(Long marketId);

    List<Trade> findTop10ByOrderByCreatedAtDesc();

    @Query("select coalesce(sum(t.totalCost), 0) from Trade t")
    BigDecimal sumTotalCost();

    @Query("select count(distinct t.user.id) from Trade t where t.createdAt >= :since")
    long countDistinctUsersWithTradesSince(@Param("since") LocalDateTime since);

    @Query("select coalesce(sum(t.totalCost), 0) from Trade t where t.market.id = :marketId")
    BigDecimal sumTotalCostByMarketId(@Param("marketId") Long marketId);

    @Query("select count(distinct t.user.id) from Trade t where t.market.id = :marketId")
    long countDistinctUsersByMarketId(@Param("marketId") Long marketId);
}
