package com.chefkix.social.moderation.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.moderation.dto.*;
import com.chefkix.social.moderation.entity.Appeal;
import com.chefkix.social.moderation.entity.Ban;
import com.chefkix.social.moderation.repository.AppealRepository;
import com.chefkix.social.moderation.repository.BanRepository;
import com.chefkix.social.post.entity.Report;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.ReportRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Moderation service for admin report review, ban management, and appeals.
 * Per spec 16-moderation.txt: Escalating penalties.
 *
 * Penalty escalation:
 * - 1st offense: 3 days
 * - 2nd offense: 7 days
 * - 3rd offense: 14 days
 * - 4th+ offense: permanent
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModerationService {

    ReportRepository reportRepository;
    BanRepository banRepository;
    AppealRepository appealRepository;
    PostRepository postRepository;

    private static final int[] PENALTY_DAYS = {3, 7, 14, -1}; // -1 = permanent

    // ─── REPORTS ────────────────────────────────────────────────────

    /**
     * Get all pending reports for admin review queue.
     */
    public List<Report> getPendingReports() {
        return reportRepository.findByStatus("pending");
    }

    /**
     * Get all reports (any status) for admin dashboard.
     */
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    /**
     * Admin reviews a report. Can resolve, dismiss, or ban the user.
     */
    public Report reviewReport(String reportId, String adminId, ReviewReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (!"pending".equals(report.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        report.setStatus(request.getDecision());
        report.setReviewedBy(adminId);
        report.setReviewNotes(request.getNotes());
        report.setReviewedAt(Instant.now());
        reportRepository.save(report);

        // If decision is ban_user, issue a ban
        if ("ban_user".equals(request.getDecision())) {
            String targetUserId = resolveReportedUserId(report);
            if (targetUserId != null) {
                banUser(targetUserId, report.getReason(), adminId,
                        request.getBanScope() != null ? request.getBanScope() : "all",
                        reportId);
            }
        }

        // If decision is resolved, unhide content if it was auto-hidden
        if ("resolved".equals(request.getDecision())) {
            unhideContent(report.getTargetType(), report.getTargetId());
        }

        log.info("Report {} reviewed by admin {} — decision: {}", reportId, adminId, request.getDecision());
        return report;
    }

    // ─── BANS ───────────────────────────────────────────────────────

    /**
     * Issue a ban with escalating penalties.
     */
    public Ban banUser(String userId, String reason, String adminId, String scope, String relatedReportId) {
        // Count previous offenses
        long previousOffenses = banRepository.countByUserId(userId);
        int offenseNumber = (int) previousOffenses + 1;
        int durationDays = offenseNumber <= PENALTY_DAYS.length
                ? PENALTY_DAYS[offenseNumber - 1]
                : PENALTY_DAYS[PENALTY_DAYS.length - 1]; // Default to permanent for 4th+

        Instant expiresAt = durationDays > 0
                ? Instant.now().plus(durationDays, ChronoUnit.DAYS)
                : null; // null = permanent

        Ban ban = Ban.builder()
                .userId(userId)
                .reason(reason)
                .scope(scope)
                .durationDays(durationDays)
                .issuedBy(adminId)
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
                .active(true)
                .offenseNumber(offenseNumber)
                .relatedReportId(relatedReportId)
                .build();

        Ban saved = banRepository.save(ban);
        log.warn("User {} banned — offense #{}, {} days, scope: {}, reason: {}",
                userId, offenseNumber, durationDays, scope, reason);
        return saved;
    }

    /**
     * Check if a user is currently banned (for a given scope).
     */
    public boolean isUserBanned(String userId, String scope) {
        List<Ban> activeBans = banRepository.findByUserIdAndActiveTrue(userId);
        return activeBans.stream().anyMatch(ban ->
                "all".equals(ban.getScope()) || ban.getScope().equals(scope));
    }

    /**
     * Get active ban for a user (for displaying ban info).
     */
    public BanResponse getActiveBan(String userId) {
        return banRepository.findFirstByUserIdAndActiveTrueOrderByIssuedAtDesc(userId)
                .map(this::toBanResponse)
                .orElse(null);
    }

    /**
     * Get ban history for a user.
     */
    public List<BanResponse> getBanHistory(String userId) {
        return banRepository.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(this::toBanResponse)
                .toList();
    }

    /**
     * Revoke a ban (admin action or appeal approved).
     */
    public void revokeBan(String banId, String adminId) {
        Ban ban = banRepository.findById(banId)
                .orElseThrow(() -> new AppException(ErrorCode.BAN_NOT_FOUND));
        ban.setActive(false);
        banRepository.save(ban);
        log.info("Ban {} revoked by admin {}", banId, adminId);
    }

    // ─── APPEALS ────────────────────────────────────────────────────

    /**
     * User submits an appeal against their ban.
     */
    public Appeal createAppeal(String userId, AppealRequest request) {
        // Verify ban exists and belongs to the requesting user
        Ban ban = banRepository.findById(request.getBanId())
                .orElseThrow(() -> new AppException(ErrorCode.BAN_NOT_FOUND));
        if (!ban.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // Check for existing pending appeal
        appealRepository.findByBanIdAndStatus(request.getBanId(), "pending")
                .ifPresent(a -> { throw new AppException(ErrorCode.APPEAL_ALREADY_EXISTS); });

        Appeal appeal = Appeal.builder()
                .userId(userId)
                .banId(request.getBanId())
                .reason(request.getReason())
                .evidenceUrls(request.getEvidenceUrls())
                .status("pending")
                .build();

        Appeal saved = appealRepository.save(appeal);
        log.info("Appeal created by user {} for ban {}", userId, request.getBanId());
        return saved;
    }

    /**
     * Admin reviews an appeal.
     */
    public Appeal reviewAppeal(String appealId, String adminId, ReviewAppealRequest request) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));

        if (!"pending".equals(appeal.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ACTION);
        }

        appeal.setStatus(request.getDecision());
        appeal.setReviewedBy(adminId);
        appeal.setReviewNotes(request.getNotes());
        appeal.setReviewedAt(Instant.now());
        appealRepository.save(appeal);

        // If approved, revoke the ban
        if ("approved".equals(request.getDecision())) {
            revokeBan(appeal.getBanId(), adminId);
        }

        log.info("Appeal {} reviewed by admin {} — decision: {}", appealId, adminId, request.getDecision());
        return appeal;
    }

    /**
     * Get all pending appeals for admin review.
     */
    public List<Appeal> getPendingAppeals() {
        return appealRepository.findByStatus("pending");
    }

    // ─── SCHEDULED: EXPIRE BANS ─────────────────────────────────────

    /**
     * Runs hourly to expire bans that have passed their expiration date.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void expireBans() {
        try {
            List<Ban> expiredBans = banRepository.findByActiveTrueAndExpiresAtBefore(Instant.now());
            for (Ban ban : expiredBans) {
                ban.setActive(false);
                banRepository.save(ban);
                log.info("Ban {} expired for user {}", ban.getId(), ban.getUserId());
            }
            if (!expiredBans.isEmpty()) {
                log.info("Expired {} bans", expiredBans.size());
            }
        } catch (Exception e) {
            log.error("Ban expiry scheduler failed — will retry next cycle", e);
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────

    /**
     * Resolve the userId of the content author from a report.
     */
    private String resolveReportedUserId(Report report) {
        if ("post".equals(report.getTargetType())) {
            return postRepository.findById(report.getTargetId())
                    .map(post -> post.getUserId())
                    .orElse(null);
        }
        // For comments and recipes, we'd need cross-module calls
        // For now, return null (admin can manually look up)
        return null;
    }

    private void unhideContent(String targetType, String targetId) {
        if ("post".equals(targetType)) {
            postRepository.findById(targetId).ifPresent(post -> {
                if (post.isHidden()) {
                    post.setHidden(false);
                    postRepository.save(post);
                    log.info("Post {} unhidden after report resolved", targetId);
                }
            });
        }
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
