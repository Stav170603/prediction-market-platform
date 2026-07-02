package com.virtualmarket.polymarket.controller;

import com.virtualmarket.polymarket.dto.TradeRequest;
import com.virtualmarket.polymarket.dto.TradeResponse;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@Tag(name = "Trades", description = "BUY and SELL execution and trade history")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    @Operation(summary = "Execute a BUY or SELL trade")
    public TradeResponse executeTrade(@RequestBody TradeRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        request.setUserId(currentUser.getId());
        return tradeService.executeTrade(request);
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Get trades for a user")
    public List<TradeResponse> getTradesByUser(@PathVariable Long userId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (!currentUser.getId().equals(userId) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's trades");
        }

        return tradeService.getTradesByUser(userId);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's trade history", tags = {"Trades", "Profile"})
    public List<TradeResponse> getMyTrades(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return tradeService.getTradesByUser(currentUser.getId());
    }

    @GetMapping("/by-market/{marketId}")
    @Operation(summary = "Get trades for a market")
    public List<TradeResponse> getTradesByMarket(@PathVariable Long marketId) {
        return tradeService.getTradesByMarket(marketId);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return user;
    }
}
