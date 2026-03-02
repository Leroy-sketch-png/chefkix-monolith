package com.chefkix.culinary.features.report.service;

import com.chefkix.culinary.common.enums.ReportStatus;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.features.report.dto.request.CreateReportRequest;
import com.chefkix.culinary.features.report.dto.request.ReportRequest;
import com.chefkix.culinary.features.report.dto.response.ReportResponse;
import com.chefkix.culinary.features.report.entity.Report;
import com.chefkix.culinary.features.report.repository.AppealRepository;
import com.chefkix.culinary.features.report.repository.RecipeReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// Dead code — all methods commented out. @Service removed to prevent bean name conflict.
// @Service
@RequiredArgsConstructor
public class ReportService {

    private final RecipeReportRepository reportRepository;
    private final AppealRepository appealRepository;

    // Configs
    private static final int MIN_ACCOUNT_AGE_DAYS = 7;
    private static final int MAX_REPORTS_PER_DAY = 3;
    private static final int REVIEW_THRESHOLD = 3;

    // --- 1. TẠO BÁO CÁO ---
//    public ReportResponse createReport(String reporterId, CreateReportRequest dto) {
//        // 1. Lấy user từ MongoDB
//        User reporter = userRepository.findById(reporterId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // 2. Check điều kiện "Skin in the game"
//        long accountAge = ChronoUnit.DAYS.between(reporter.getCreatedAt(), LocalDateTime.now());
//        if (accountAge < MIN_ACCOUNT_AGE_DAYS) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account too new");
//        }
//        if (reporter.getCompletedRecipesCount() < 1) {
//            throw new AppException(ErrorCode.NO_COMPLETION_FOUND);
//        }
//
//        // 3. Check Spam (Rate Limit)
//        long reportsToday = reportRepository.countByReporterIdAndCreatedAtAfter(
//                reporterId, LocalDateTime.now().minusDays(1)
//        );
//        if (reportsToday >= MAX_REPORTS_PER_DAY) {
//            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
//        }
//
//        // 4. Lưu Report vào Mongo
//        Report report = new Report();
//        report.setReporterId(reporterId);
//        report.setTargetType(dto.getTargetType());
//        report.setTargetId(dto.getTargetId());
//        report.setReason(dto.getReason());
//        report.setDetails(dto.getDetails());
//
//        reportRepository.save(report);
//
//        // 5. Check Threshold Trigger Review
//        long uniqueReports = reportRepository.countByTargetIdAndStatus(dto.getTargetId(), ReportStatus.PENDING);
//        boolean reviewTriggered = uniqueReports >= REVIEW_THRESHOLD;
//
//        return new ReportResponse(report.getId(), (int)uniqueReports, reviewTriggered);
//    }
}