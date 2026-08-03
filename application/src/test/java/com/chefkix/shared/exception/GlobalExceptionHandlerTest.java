package com.chefkix.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    @Test
    void methodParameterViolationReturnsSafeBadRequest() {
        var response = new GlobalExceptionHandler()
                .handleConstraintViolation(new ConstraintViolationException(Set.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatusCode()).isEqualTo(400);
        assertThat(response.getBody().getMessage())
                .isEqualTo("One or more request parameters are invalid.");
    }
}
