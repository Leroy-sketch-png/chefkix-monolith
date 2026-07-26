package com.chefkix.culinary.features.session.controller;

import com.chefkix.culinary.common.dto.query.SessionHistoryQuery;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.dto.PaginationMeta;
import com.chefkix.culinary.common.dto.response.LoggedResponse;
import com.chefkix.culinary.features.session.dto.request.*;
import com.chefkix.culinary.features.session.dto.response.*;
import com.chefkix.culinary.features.session.service.CookingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cooking-sessions")
@RequiredArgsConstructor
public class CookingSessionController {

    private final CookingSessionService sessionService;

    @PostMapping
    public ApiResponse<StartSessionResponse> startSession(
            @Valid @RequestBody StartSessionRequest request
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(sessionService.startSession(userId, request));
    }

    @PostMapping("/{sessionId}/complete")
    public ApiResponse<SessionCompletionResponse> completeSession(
            @PathVariable String sessionId,
            @Valid @RequestBody CompleteSessionRequest request) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(sessionService.completeSession(userId, sessionId, request));
    }

    @GetMapping
    public ApiResponse<SessionHistoryResponse> getSessionHistory(
            @ModelAttribute SessionHistoryQuery query,
            @PageableDefault(sort = {"completedAt", "startedAt"},
                    direction = Sort.Direction.DESC,
                    size = 20) Pageable pageable) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Page<SessionHistoryResponse.SessionItemDto> pageResult =
                sessionService.getSessionHistory(userId, query, pageable);

        PaginationMeta paginationMeta = PaginationMeta.from(pageResult);

        SessionHistoryResponse historyResponse = SessionHistoryResponse.builder()
                .sessions(pageResult.getContent())
                .build();


        return ApiResponse.<SessionHistoryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Get paged list successfully")
                .data(historyResponse)
                .pagination(paginationMeta)
                .build();
    }

    @GetMapping("/pending")
    public ApiResponse<List<SessionHistoryResponse.SessionItemDto>> getPendingSessions() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(sessionService.getPendingSessions(userId));
    }

    @PostMapping("/abandon-active")
    public ApiResponse<SessionAbandonResponse> abandonActiveSession() {
        return ApiResponse.success(sessionService.abandonActiveSession());
    }

    @PostMapping("/{sessionId}/timer-event")
    public ApiResponse<LoggedResponse> startTimerEvent(
            @Valid @PathVariable String sessionId,
            @Valid @RequestBody TimerEventRequest request
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        sessionService.logTimerEvent(userId, sessionId, request);
        LoggedResponse response = LoggedResponse.builder().logged(true).build();
        return ApiResponse.success(response);
    }

    @PostMapping("/{sessionId}/navigate")
    public ApiResponse<SessionNavigateResponse> getSessionCurrentStep(
            @Valid @PathVariable String sessionId,
            @Valid @RequestBody SessionNavigateRequest request
    ) {
        SessionNavigateResponse response = sessionService.getSessionCurrentStep(sessionId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentSessionResponse>> getCurrentSession(
    ) {
        CurrentSessionResponse response = sessionService.getCurrentSession();
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<CurrentSessionResponse>builder()
                            .success(false)
                            .statusCode(404)
                            .message("No active session")
                            .build()
            );
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     */
    @GetMapping("/friends-active")
    public ApiResponse<FriendCookingActivityResponse> getFriendsActiveCooking() {
        return ApiResponse.success(sessionService.getFriendsActiveCooking());
    }

    @GetMapping("/{sessionId:[a-fA-F0-9]{24}}")
    public ApiResponse<CurrentSessionResponse> getSessionBySessionId(
            @PathVariable String sessionId
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(sessionService.getBySessionId(sessionId, userId));
    }

    @PostMapping("/{sessionId}/pause")
    public ApiResponse<SessionPauseResponse> pauseSession(
            @Valid @PathVariable String sessionId
    ) {
        SessionPauseResponse response = sessionService.pauseSession(sessionId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{sessionId}/resume")
    public ApiResponse<SessionResumeResponse> resumeSession(
            @Valid @PathVariable String sessionId
    ) {
        SessionResumeResponse response = sessionService.resumeSession(sessionId);
        return ApiResponse.success(response);
    }

    /**
     */
    @PostMapping("/{sessionId}/complete-step")
    public ApiResponse<CompleteStepResponse> completeStep(
            @PathVariable String sessionId,
            @Valid @RequestBody CompleteStepRequest request
    ) {
        CompleteStepResponse response = sessionService.completeStep(sessionId, request);
        return ApiResponse.success(response);
    }

    @PostMapping("/{sessionId}/link-post")
    public ApiResponse<SessionLinkingResponse> linkPost(
            @Valid @PathVariable String sessionId,
            @Valid @RequestBody SessionLinkingRequest request
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        SessionLinkingResponse response = sessionService.linkSession(userId, sessionId, request);
        return ApiResponse.success(response);
    }

    /**
     */
    @PostMapping("/{sessionId}/abandon")
    public ApiResponse<SessionAbandonResponse> abandonSession(
            @PathVariable String sessionId
    ) {
        SessionAbandonResponse response = sessionService.abandonSession(sessionId);
        return ApiResponse.success(response);
    }

    /**
     */
    @GetMapping("/{sessionId}/cook-card")
    public ApiResponse<CookCardDataResponse> getCookCardData(@PathVariable String sessionId) {
        return ApiResponse.success(sessionService.getCookCardData(sessionId));
    }
}
