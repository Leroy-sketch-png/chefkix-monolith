package com.chefkix.social.moderation.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.moderation.dto.*;
import com.chefkix.social.moderation.entity.Appeal;
import com.chefkix.social.moderation.entity.Ban;
import com.chefkix.social.moderation.service.ModerationService;
import com.chefkix.social.post.entity.Report;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for moderation, reports, bans, and appeals.
 * All endpoints require ROLE_ADMIN.
 * Per spec 16-moderation.txt.
 *
 * Monolith path: /api/v1/admin/*
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    ModerationService moderationService;

    // ─── REPORTS ────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/reports — Admin report review queue.
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Report>>> getPendingReports() {
        List<Report> reports = moderationService.getPendingReports();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    /**
     * GET /api/v1/admin/reports/all — All reports (any status).
     */
    @GetMapping("/reports/all")
    public ResponseEntity<ApiResponse<List<Report>>> getAllReports() {
        List<Report> reports = moderationService.getAllReports();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    /**
     * POST /api/v1/admin/reports/{reportId}/review — Review a report.
     * Decision: "resolved", "dismissed", "ban_user"
     */
    @PostMapping("/reports/{reportId}/review")
    public ResponseEntity<ApiResponse<Report>> reviewReport(
            @PathVariable String reportId,
            Authentication authentication,
            @Valid @RequestBody ReviewReportRequest request) {
        String adminId = authentication.getName();
        Report reviewed = moderationService.reviewReport(reportId, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok(reviewed));
    }

    // ─── BANS ───────────────────────────────────────────────────────

    /**
     * POST /api/v1/admin/users/{userId}/ban — Manually ban a user.
     */
    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<ApiResponse<BanResponse>> banUser(
            @PathVariable String userId,
            Authentication authentication,
            @Valid @RequestBody BanUserRequest request) {
        String adminId = authentication.getName();
        Ban ban = moderationService.banUser(
                userId, request.getReason(), adminId,
                request.getScope() != null ? request.getScope() : "all",
                null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(toBanResponse(ban)));
    }

    /**
     * GET /api/v1/admin/users/{userId}/bans — Get ban history for a user.
     */
    @GetMapping("/users/{userId}/bans")
    public ResponseEntity<ApiResponse<List<BanResponse>>> getBanHistory(
            @PathVariable String userId) {
        List<BanResponse> bans = moderationService.getBanHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(bans));
    }

    /**
     * DELETE /api/v1/admin/bans/{banId} — Revoke a ban.
     */
    @DeleteMapping("/bans/{banId}")
    public ResponseEntity<ApiResponse<String>> revokeBan(
            @PathVariable String banId,
            Authentication authentication) {
        String adminId = authentication.getName();
        moderationService.revokeBan(banId, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Ban revoked successfully"));
    }

    // ─── APPEALS ────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/appeals — Pending appeals queue.
     */
    @GetMapping("/appeals")
    public ResponseEntity<ApiResponse<List<Appeal>>> getPendingAppeals() {
        List<Appeal> appeals = moderationService.getPendingAppeals();
        return ResponseEntity.ok(ApiResponse.ok(appeals));
    }

    /**
     * POST /api/v1/admin/appeals/{appealId}/review — Review an appeal.
     * Decision: "approved" (revokes ban) or "rejected"
     */
    @PostMapping("/appeals/{appealId}/review")
    public ResponseEntity<ApiResponse<Appeal>> reviewAppeal(
            @PathVariable String appealId,
            Authentication authentication,
            @Valid @RequestBody ReviewAppealRequest request) {
        String adminId = authentication.getName();
        Appeal reviewed = moderationService.reviewAppeal(appealId, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok(reviewed));
    }

    // ─── HELPERS ───────────────────────────────────────────────────

    private BanResponse toBanResponse(Ban ban) {
        return BanResponse.builder()
                .id(ban.getId())
                .userId(ban.getUserId())
                .reason(ban.getReason())
                .scope(ban.getScope())
                .durationDays(ban.getDurationDays())
                .offenseNumber(ban.getOffenseNumber())
                .issuedAt(ban.getIssuedAt())
                .expiresAt(ban.getExpiresAt())
                .active(ban.isActive())
                .permanent(ban.getDurationDays() == -1)
                .build();
    }
}
