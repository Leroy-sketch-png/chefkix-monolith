package com.chefkix.social.post.service;

import com.chefkix.social.post.dto.request.ReportRequest;
import com.chefkix.social.post.dto.response.ReportResponse;
import com.chefkix.social.post.entity.Report;
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
 * Service for handling content reports.
 * Per spec 13-moderation.txt:
 * - Max 3 reports per day per user
 * - 3 unique reports on same content triggers admin review
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService {

    ReportRepository reportRepository;
    PostRepository postRepository;
    CommentRepository commentRepository;

    private static final int MAX_REPORTS_PER_DAY = 3;
    private static final int REVIEW_THRESHOLD = 3;

    /**
     * Create a report for content (post, comment, or recipe).
     */
    public ReportResponse createReport(Authentication authentication, ReportRequest request) {
        String reporterId = authentication.getName();

        // Rate limiting: Check if user has exceeded daily report limit
        Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todayReportCount = reportRepository.countByReporterIdAndCreatedAtAfter(reporterId, dayStart);
        if (todayReportCount >= MAX_REPORTS_PER_DAY) {
            throw new AppException(ErrorCode.REPORT_LIMIT_EXCEEDED);
        }

        // Check if user already reported this target
        var existingReport = reportRepository.findByReporterIdAndTargetTypeAndTargetId(
                reporterId, request.getTargetType(), request.getTargetId());
        if (existingReport.isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_REPORT);
        }

        // Validate target exists
        validateTargetExists(request.getTargetType(), request.getTargetId());

        // Create the report
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .details(request.getDetails())
                .status("pending")
                .build();

        Report saved = reportRepository.save(report);

        // Count total reports for this target
        long totalReports = reportRepository.countByTargetTypeAndTargetId(
                request.getTargetType(), request.getTargetId());

        // Check if review should be triggered
        boolean reviewTriggered = totalReports >= REVIEW_THRESHOLD;

        if (reviewTriggered) {
            log.warn("Review triggered for {} {} - {} reports received",
                    request.getTargetType(), request.getTargetId(), totalReports);
            // TODO: Send Kafka event to moderation queue for admin review
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
     * Validate that the target content exists.
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
                // Recipe validation would require calling recipe-service
                // For MVP, we skip this validation
                log.info("Recipe validation skipped for {}", targetId);
                break;
            default:
                throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}
