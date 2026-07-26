package com.chefkix.social.post.service;

import com.chefkix.social.post.dto.request.ReportRequest;
import com.chefkix.social.post.dto.response.ReportResponse;
import com.chefkix.social.post.entity.Report;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.ReportRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService {

    ReportRepository reportRepository;
    PostRepository postRepository;
    CommentRepository commentRepository;
    ProfileProvider profileProvider;

    private static final int MAX_REPORTS_PER_DAY = 3;
    private static final int REVIEW_THRESHOLD = 3;
    private static final int MIN_ACCOUNT_AGE_DAYS = 7;
    private static final int MIN_USER_LEVEL = 2;

    /**
     */
    public ReportResponse createReport(Authentication authentication, ReportRequest request) {
        String reporterId = authentication.getName();

        Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todayReportCount = reportRepository.countByReporterIdAndCreatedAtAfter(reporterId, dayStart);
        if (todayReportCount >= MAX_REPORTS_PER_DAY) {
            throw new AppException(ErrorCode.REPORT_LIMIT_EXCEEDED);
        }

        validateReporterMaturity(reporterId);

        var existingReport = reportRepository.findByReporterIdAndTargetTypeAndTargetId(
                reporterId, request.getTargetType(), request.getTargetId());
        if (existingReport.isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_REPORT);
        }

        validateTargetExists(request.getTargetType(), request.getTargetId());

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .details(request.getDetails())
                .status("pending")
                .build();

        Report saved = reportRepository.save(report);

        long totalReports = reportRepository.countByTargetTypeAndTargetId(
                request.getTargetType(), request.getTargetId());

        boolean reviewTriggered = totalReports >= REVIEW_THRESHOLD;

        if (reviewTriggered) {
            log.warn("Review triggered for {} {} - {} reports received",
                    request.getTargetType(), request.getTargetId(), totalReports);
            autoHideContent(request.getTargetType(), request.getTargetId());
        }

        log.info("Report created: {} reported {} {} for {}",
                reporterId, request.getTargetType(), request.getTargetId(), request.getReason());

        return ReportResponse.builder()
                .reportId(saved.getId())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .reason(saved.getReason())
                .reportCount((int) totalReports)
                .reviewTriggered(reviewTriggered)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     */
    private void autoHideContent(String targetType, String targetId) {
        if ("post".equals(targetType)) {
            postRepository.findById(targetId).ifPresent(post -> {
                if (!post.isHidden()) {
                    post.setHidden(true);
                    postRepository.save(post);
                    log.info("Post {} auto-hidden after reaching report threshold", targetId);
                }
            });
        }
        if ("comment".equals(targetType) || "recipe".equals(targetType)) {
            log.warn("{} {} reached report threshold — needs admin review", targetType, targetId);
        }
    }

    /**
     */
    private void validateReporterMaturity(String reporterId) {
        Instant accountCreated = profileProvider.getAccountCreatedAt(reporterId);
        if (ChronoUnit.DAYS.between(accountCreated, Instant.now()) < MIN_ACCOUNT_AGE_DAYS) {
            throw new AppException(ErrorCode.ACCOUNT_TOO_NEW);
        }
        int userLevel = profileProvider.getUserLevel(reporterId);
        if (userLevel < MIN_USER_LEVEL) {
            throw new AppException(ErrorCode.INSUFFICIENT_ACTIVITY);
        }
    }

    /**
     */
    private void validateTargetExists(String targetType, String targetId) {
        switch (targetType) {
            case "post":
                postRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
                break;
            case "comment":
                commentRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
                break;
            case "recipe":
                log.info("Recipe validation skipped for {}", targetId);
                break;
            default:
                throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}
