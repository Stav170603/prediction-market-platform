package com.virtualmarket.polymarket.controller;

import com.virtualmarket.polymarket.dto.PositionResponse;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@Tag(name = "Positions", description = "User holdings in market outcomes")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get active positions for a user",
            description = "Returns positive positions in OPEN markets only."
    )
    public List<PositionResponse> getUserPositions(@PathVariable Long userId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (!currentUser.getId().equals(userId) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's positions");
        }

        return positionService.getUserPositions(userId);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get the authenticated user's active portfolio positions",
            description = "Returns positive positions in OPEN markets only.",
            tags = {"Positions", "Profile"}
    )
    public List<PositionResponse> getMyPositions(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return positionService.getUserPositions(currentUser.getId());
    }

    @GetMapping("/market/{marketId}")
    @Operation(
            summary = "Get all positions in a market",
            description = "Returns market positions regardless of lifecycle status for administrative or historical use."
    )
    public List<PositionResponse> getMarketPositions(@PathVariable Long marketId) {
        return positionService.getMarketPositions(marketId);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return user;
    }
}
