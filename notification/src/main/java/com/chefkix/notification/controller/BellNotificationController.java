package com.chefkix.notification.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.chefkix.notification.dto.request.NotificationUpdateRequest;
import com.chefkix.notification.dto.response.NotificationResponse;
import com.chefkix.notification.dto.response.NotificationSummaryResponse;
import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Validated
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Slf4j
public class BellNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadNotificationCount() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(notificationService.getUnreadNotificationCount(userId));
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(notificationService.getNotifications(userId, limit, unreadOnly));
    }

    @PutMapping
    public ApiResponse<Void> updateNotificationReadStatus(@Valid @RequestBody NotificationUpdateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.updateReadStatus(userId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Map<String, Boolean>> markAsRead(@PathVariable String notificationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.markAsRead(userId, notificationId);
        return ApiResponse.success(Map.of("read", true));
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Long>> markAllAsRead() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        long readCount = notificationService.markAllAsRead(userId);
        return ApiResponse.success(Map.of("readCount", readCount));
    }

    /**
     * Get aggregated activity summary since a given timestamp.
     * Powers the "Welcome Back" card on the dashboard.
     * FE stores lastVisitTimestamp in localStorage and passes it here.
     */
    @GetMapping("/summary-since")
    public ApiResponse<NotificationSummaryResponse> getActivitySummary(
            @RequestParam Instant since) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(notificationService.getActivitySummary(userId, since));
    }
}
