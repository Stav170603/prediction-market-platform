package com.virtualmarket.polymarket.repository;

import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.MarketOutcome;
import com.virtualmarket.polymarket.entity.Position;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.MarketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByUser(User user);

    List<Position> findByMarket(Market market);

    Optional<Position> findByUserAndMarketAndOutcome(User user, Market market, MarketOutcome outcome);

    @Query("""
            select p
            from Position p
            join fetch p.user
            join fetch p.market
            join fetch p.outcome
            where p.user.id = :userId
              and p.market.status = :marketStatus
              and p.quantity > :minimumQuantity
            """)
    List<Position> findOpenPositionsByUserId(
            @Param("userId") Long userId,
            @Param("marketStatus") MarketStatus marketStatus,
            @Param("minimumQuantity") BigDecimal minimumQuantity
    );
}
