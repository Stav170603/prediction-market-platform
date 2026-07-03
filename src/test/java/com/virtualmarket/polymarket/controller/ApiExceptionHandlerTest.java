package com.virtualmarket.polymarket.controller;

import com.virtualmarket.polymarket.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    @Test
    void businessValidationErrorKeepsBadRequestStatusAndMessage() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient position to sell")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Insufficient position to sell");
    }
}
