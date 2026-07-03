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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Create a prediction market",
            description = "Creates an OPEN binary YES/NO market with all required metadata and initial prices.",
            tags = "Admin"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Market created"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing market data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public MarketResponse createMarket(@Valid @RequestBody CreateMarketRequest request) {
        return marketService.createMarket(request);
    }

    @GetMapping
    @Operation(summary = "List all markets", description = "Returns markets in every lifecycle status.")
    @ApiResponse(responseCode = "200", description = "Markets returned")
    public List<MarketResponse> getAllMarkets() {
        return marketService.getAllMarkets();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a market by ID", description = "Returns market metadata, status, outcomes, and current prices.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Market returned"),
            @ApiResponse(responseCode = "404", description = "Market not found")
    })
    public MarketResponse getMarketById(@PathVariable Long id) {
        return marketService.getMarketById(id);
    }

    @GetMapping("/{marketId}/history")
    @Operation(
            summary = "Get chronological trade-driven price history",
            description = "Returns persisted YES/NO price snapshots ordered from oldest to newest."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price history returned"),
            @ApiResponse(responseCode = "404", description = "Market not found")
    })
    public List<PriceHistoryResponse> getMarketPriceHistory(@PathVariable Long marketId) {
        return marketService.getPriceHistory(marketId);
    }

    @GetMapping("/{marketId}/statistics")
    @Operation(summary = "Get market trading statistics", description = "Returns trade count, volume, liquidity, and active traders.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics returned"),
            @ApiResponse(responseCode = "404", description = "Market not found")
    })
    public MarketStatisticsResponse getMarketStatistics(@PathVariable Long marketId) {
        return statisticsService.getMarketStatistics(marketId);
    }

    @PostMapping("/resolve")
    @Operation(
            summary = "Resolve and settle a market",
            description = "Selects the winning outcome, pays winning positions in virtual points, and updates reliability scores.",
            tags = "Admin"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Market resolved and settled"),
            @ApiResponse(responseCode = "400", description = "Market cannot be resolved or request is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Market, outcome, admin, or wallet not found")
    })
    public ResolutionResponse resolveMarket(@RequestBody ResolutionRequest request) {
        return marketResolutionService.resolveMarket(request);
    }

    @PostMapping("/{marketId}/cancel")
    @Operation(
            summary = "Cancel a market",
            description = "Changes an OPEN or CLOSED market to CANCELLED. Cancelled markets cannot be traded or resolved.",
            tags = "Admin"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Market cancelled"),
            @ApiResponse(responseCode = "400", description = "Market is resolved or already cancelled"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Market not found")
    })
    public MarketResponse cancelMarket(@PathVariable Long marketId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User admin)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        marketLifecycleService.cancelMarket(marketId, admin);
        return marketService.getMarketById(marketId);
    }
}
