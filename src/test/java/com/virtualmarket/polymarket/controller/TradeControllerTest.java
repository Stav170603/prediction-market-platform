package com.virtualmarket.polymarket.controller;

import com.virtualmarket.polymarket.dto.TradeRequest;
import com.virtualmarket.polymarket.dto.TradeResponse;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.service.TradeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    @Mock
    private TradeService tradeService;

    @InjectMocks
    private TradeController tradeController;

    @Test
    void authenticatedUserCanTradeForSelf() {
        User user = user(10L, UserRole.USER);
        TradeRequest request = requestWithUserId(10L);
        TradeResponse expected = new TradeResponse();
        when(tradeService.executeTrade(request)).thenReturn(expected);

        TradeResponse result = tradeController.executeTrade(request, authentication(user));

        assertThat(result).isSameAs(expected);
        assertThat(capturedRequest().getUserId()).isEqualTo(10L);
    }

    @Test
    void mismatchedRequestUserIdIsIgnoredAndReplacedWithAuthenticatedUserId() {
        User authenticatedUser = user(10L, UserRole.USER);
        TradeRequest request = requestWithUserId(99L);
        when(tradeService.executeTrade(request)).thenReturn(new TradeResponse());

        tradeController.executeTrade(request, authentication(authenticatedUser));

        assertThat(capturedRequest().getUserId()).isEqualTo(10L);
    }

    @Test
    void adminCannotImpersonateAnotherUserThroughNormalTradeEndpoint() {
        User admin = user(1L, UserRole.ADMIN);
        TradeRequest request = requestWithUserId(99L);
        when(tradeService.executeTrade(request)).thenReturn(new TradeResponse());

        tradeController.executeTrade(request, authentication(admin));

        assertThat(capturedRequest().getUserId()).isEqualTo(1L);
    }

    private TradeRequest capturedRequest() {
        ArgumentCaptor<TradeRequest> captor = ArgumentCaptor.forClass(TradeRequest.class);
        verify(tradeService).executeTrade(captor.capture());
        return captor.getValue();
    }

    private TradeRequest requestWithUserId(Long userId) {
        TradeRequest request = new TradeRequest();
        request.setUserId(userId);
        return request;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private UsernamePasswordAuthenticationToken authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, null);
    }
}
