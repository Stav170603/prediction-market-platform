package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.entity.Market;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.MarketStatus;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.repository.MarketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketLifecycleService {

    private final MarketRepository marketRepository;

    public MarketLifecycleService(MarketRepository marketRepository) {
        this.marketRepository = marketRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredMarkets() {
        List<Market> expiredMarkets = marketRepository
                .findByStatusAndTradingCloseDateLessThanEqual(MarketStatus.OPEN, LocalDateTime.now());
        expiredMarkets.forEach(market -> market.setStatus(MarketStatus.CLOSED));
        marketRepository.saveAll(expiredMarkets);
    }

    public void closeIfExpired(Market market) {
        if (market.getStatus() == MarketStatus.OPEN
                && market.getTradingCloseDate() != null
                && !LocalDateTime.now().isBefore(market.getTradingCloseDate())) {
            market.setStatus(MarketStatus.CLOSED);
            marketRepository.save(market);
        }
    }

    @Transactional
    public void cancelMarket(Long marketId, User admin) {
        if (admin == null || admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN users can cancel markets");
        }

        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Market not found"));

        if (market.getStatus() == MarketStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolved market cannot be cancelled");
        }
        if (market.getStatus() == MarketStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market is already cancelled");
        }

        market.setStatus(MarketStatus.CANCELLED);
        marketRepository.save(market);
    }
}
