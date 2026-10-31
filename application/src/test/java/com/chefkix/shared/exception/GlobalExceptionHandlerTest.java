package com.chefkix.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.chefkix.shared.exception.AppException;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;

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

    @Test
    void optimisticLockingFailureReturnsConflictStatusAndSafeMessage() {
        var response = new GlobalExceptionHandler()
                .handleOptimisticLocking(new OptimisticLockingFailureException("Version conflict"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatusCode()).isEqualTo(409);
        assertThat(response.getBody().getMessage())
                .isEqualTo(ErrorCode.RECIPE_CONCURRENT_UPDATE.getMessage());
    }

    @Test
    void storyHighlightErrorsUseTheirClientErrorStatusAndStableMessage() {
        var handler = new GlobalExceptionHandler();

        var notFound = handler.handleAppException(new AppException(ErrorCode.STORY_HIGHLIGHT_NOT_FOUND));
        var invalidStories = handler.handleAppException(new AppException(ErrorCode.STORY_HIGHLIGHT_STORIES_INVALID));

        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody()).isNotNull();
        assertThat(notFound.getBody().getMessage()).isEqualTo("Story highlight not found");
        assertThat(invalidStories.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidStories.getBody()).isNotNull();
        assertThat(invalidStories.getBody().getMessage())
                .isEqualTo("One or more selected stories cannot be added to this highlight");
    }
}
