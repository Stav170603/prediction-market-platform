package com.virtualmarket.polymarket.controller;

import com.virtualmarket.polymarket.dto.CreateMarketRequest;
import com.virtualmarket.polymarket.dto.MarketResponse;
import com.virtualmarket.polymarket.dto.MarketStatisticsResponse;
import com.virtualmarket.polymarket.dto.PriceHistoryResponse;
import com.virtualmarket.polymarket.dto.ResolutionRequest;
import com.virtualmarket.polymarket.dto.ResolutionResponse;
import com.virtualmarket.polymarket.service.MarketResolutionService;
import com.virtualmarket.polymarket.service.MarketService;
import com.virtualmarket.polymarket.service.MarketLifecycleService;
import com.virtualmarket.polymarket.service.StatisticsService;
import com.virtualmarket.polymarket.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/markets")
@Tag(name = "Markets", description = "Prediction market discovery, pricing, and administration")
public class MarketController {

    private final MarketService marketService;
    private final MarketResolutionService marketResolutionService;
    private final StatisticsService statisticsService;
    private final MarketLifecycleService marketLifecycleService;

    public MarketController(
            MarketService marketService,
            MarketResolutionService marketResolutionService,
            StatisticsService statisticsService,
            MarketLifecycleService marketLifecycleService
    ) {
        this.marketService = marketService;
        this.marketResolutionService = marketResolutionService;
        this.statisticsService = statisticsService;
        this.marketLifecycleService = marketLifecycleService;
    }

    @PostMapping("/admin/markets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a prediction market", tags = "Admin")
    public MarketResponse createMarket(@Valid @RequestBody CreateMarketRequest request) {
        return marketService.createMarket(request);
    }

    @GetMapping
    @Operation(summary = "List all markets")
    public List<MarketResponse> getAllMarkets() {
        return marketService.getAllMarkets();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a market by ID")
    public MarketResponse getMarketById(@PathVariable Long id) {
        return marketService.getMarketById(id);
    }

    @GetMapping("/{marketId}/history")
    @Operation(summary = "Get chronological trade-driven price history")
    public List<PriceHistoryResponse> getMarketPriceHistory(@PathVariable Long marketId) {
        return marketService.getPriceHistory(marketId);
    }

    @GetMapping("/{marketId}/statistics")
    @Operation(summary = "Get market trading statistics")
    public MarketStatisticsResponse getMarketStatistics(@PathVariable Long marketId) {
        return statisticsService.getMarketStatistics(marketId);
    }

    @PostMapping("/resolve")
    @Operation(summary = "Resolve and settle a market", tags = "Admin")
    public ResolutionResponse resolveMarket(@RequestBody ResolutionRequest request) {
        return marketResolutionService.resolveMarket(request);
    }

    @PostMapping("/{marketId}/cancel")
    @Operation(
            summary = "Cancel a market",
            description = "Changes an OPEN or CLOSED market to CANCELLED. Cancelled markets cannot be traded or resolved.",
            tags = "Admin"
    )
    public MarketResponse cancelMarket(@PathVariable Long marketId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User admin)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        marketLifecycleService.cancelMarket(marketId, admin);
        return marketService.getMarketById(marketId);
    }
}
