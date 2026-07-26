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
 *
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    ModerationService moderationService;


    /**
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Report>>> getPendingReports() {
        List<Report> reports = moderationService.getPendingReports();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    /**
     */
    @GetMapping("/reports/all")
    public ResponseEntity<ApiResponse<List<Report>>> getAllReports() {
        List<Report> reports = moderationService.getAllReports();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    /**
     */
    @PostMapping("/reports/{reportId}/review")
    public ResponseEntity<ApiResponse<Report>> reviewReport(
            @PathVariable("reportId") String reportId,
            Authentication authentication,
            @Valid @RequestBody ReviewReportRequest request) {
        String adminId = authentication.getName();
        Report reviewed = moderationService.reviewReport(reportId, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok(reviewed));
    }


    /**
     */
    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<ApiResponse<BanResponse>> banUser(
            @PathVariable("userId") String userId,
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
     */
    @GetMapping("/users/{userId}/bans")
    public ResponseEntity<ApiResponse<List<BanResponse>>> getBanHistory(
            @PathVariable("userId") String userId) {
        List<BanResponse> bans = moderationService.getBanHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(bans));
    }

    /**
     */
    @DeleteMapping("/bans/{banId}")
    public ResponseEntity<ApiResponse<String>> revokeBan(
            @PathVariable("banId") String banId,
            Authentication authentication) {
        String adminId = authentication.getName();
        moderationService.revokeBan(banId, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Ban revoked successfully"));
    }


    /**
     */
    @GetMapping("/appeals")
    public ResponseEntity<ApiResponse<List<Appeal>>> getPendingAppeals() {
        List<Appeal> appeals = moderationService.getPendingAppeals();
        return ResponseEntity.ok(ApiResponse.ok(appeals));
    }

    /**
     */
    @PostMapping("/appeals/{appealId}/review")
    public ResponseEntity<ApiResponse<Appeal>> reviewAppeal(
            @PathVariable("appealId") String appealId,
            Authentication authentication,
            @Valid @RequestBody ReviewAppealRequest request) {
        String adminId = authentication.getName();
        Appeal reviewed = moderationService.reviewAppeal(appealId, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok(reviewed));
    }


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
