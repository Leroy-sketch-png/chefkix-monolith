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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cooking-sessions")
@RequiredArgsConstructor
public class CookingSessionController {

    private final CookingSessionService sessionService;

    // 1. Start Session
    @PostMapping
    public ApiResponse<StartSessionResponse> startSession(
            @RequestBody StartSessionRequest request
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(sessionService.startSession(userId, request));
    }

    // 2. Complete Session (Nhận 30% XP)
    @PostMapping("/{sessionId}/complete")
    public ApiResponse<SessionCompletionResponse> completeSession(
            @PathVariable String sessionId,
            @RequestBody CompleteSessionRequest request) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(sessionService.completeSession(userId, sessionId, request));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<CurrentSessionResponse> getSessionBySessionId(
            @PathVariable String sessionId
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(sessionService.getBySessionId(sessionId, userId));
    }

    @GetMapping
    public ApiResponse<SessionHistoryResponse> getSessionHistory(
            @ModelAttribute SessionHistoryQuery query,
            @PageableDefault(sort = {"completedAt", "startedAt"},
                    direction = Sort.Direction.DESC,
                    size = 20) Pageable pageable) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Service trả về Page<SessionItemDto>
        Page<SessionHistoryResponse.SessionItemDto> pageResult =
                sessionService.getSessionHistory(userId, query, pageable);

        // 2. Tạo Pagination Meta
        PaginationMeta paginationMeta = PaginationMeta.from(pageResult);

        // 3. Đóng gói dữ liệu và Pagination vào DTO SessionHistoryResponse
        SessionHistoryResponse historyResponse = SessionHistoryResponse.builder()
                .sessions(pageResult.getContent())
                // NOTE: SessionHistoryResponse cần một trường PaginationMeta để khớp với logic này.
                // Nếu bạn không muốn thay đổi SessionHistoryResponse, bạn có thể truyền thẳng vào ApiResponse.
                .build();

        // 4. Trả về ApiResponse với DTO và Pagination Meta (Dùng hàm successPage tùy chỉnh)
        // Chúng ta phải tạo lại hàm successPage để đặt Pagination vào DTO chính.

        // DO KHÔNG THỂ THÊM FIELD PAGINATION VÀO SessionHistoryResponse
        // VÀ KHÔNG THỂ DÙNG HÀM successPage cũ, TA PHẢI TỰ DỰNG BUILDER:

        return ApiResponse.<SessionHistoryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Get paged list successfully")
                .data(historyResponse)
                .pagination(paginationMeta)
                .build();
    }

    @PostMapping("/{sessionId}/timer-event")
    public ApiResponse<LoggedResponse> startTimerEvent(
            @Valid @PathVariable String sessionId,
            @RequestBody TimerEventRequest request
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        sessionService.logTimerEvent(userId, sessionId, request);
        LoggedResponse response = LoggedResponse.builder().logged(true).build();
        return ApiResponse.success(response);
    }

    @PostMapping("/{sessionId}/navigate")
    public ApiResponse<SessionNavigateResponse> getSessionCurrentStep(
            @Valid @PathVariable String sessionId,
            @RequestBody SessionNavigateRequest request
    ) {
        SessionNavigateResponse response = sessionService.getSessionCurrentStep(sessionId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/current")
    public ApiResponse<CurrentSessionResponse> getCurrentSession(
    ) {
        CurrentSessionResponse response = sessionService.getCurrentSession();
        // Return 404-style response when no active session exists
        // This prevents FE from receiving 200 OK with null/undefined data
        if (response == null) {
            return ApiResponse.<CurrentSessionResponse>builder()
                    .success(false)
                    .statusCode(404)
                    .message("No active session")
                    .build();
        }
        return ApiResponse.success(response);
    }

    /**
     * Get active cooking sessions of people the current user follows.
     * Powers the "Friends Cooking Now" widget on dashboard and explore pages.
     */
    @GetMapping("/friends-active")
    public ApiResponse<FriendCookingActivityResponse> getFriendsActiveCooking() {
        return ApiResponse.success(sessionService.getFriendsActiveCooking());
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
     * Mark a step as completed.
     * Navigation and completion are separate:
     * - navigate = move cursor (which step is displayed)
     * - complete-step = mark a step done (add to completedSteps[])
     * Users can complete steps in any order (non-linear cooking).
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
            @RequestBody SessionLinkingRequest request
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        SessionLinkingResponse response = sessionService.linkSession(userId, sessionId, request);
        return ApiResponse.success(response);
    }

    /**
     * Abandon a cooking session.
     * Sets status to ABANDONED. Cannot be resumed.
     */
    @PostMapping("/{sessionId}/abandon")
    public ApiResponse<SessionAbandonResponse> abandonSession(
            @PathVariable String sessionId
    ) {
        SessionAbandonResponse response = sessionService.abandonSession(sessionId);
        return ApiResponse.success(response);
    }
}