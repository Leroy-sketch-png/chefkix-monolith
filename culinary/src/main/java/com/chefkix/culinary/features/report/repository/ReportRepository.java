package com.chefkix.culinary.features.report.repository;

import com.chefkix.culinary.common.enums.ReportStatus;
import com.chefkix.culinary.features.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {

    // Đếm số report của 1 user trong khoảng thời gian (để chống spam)
    long countByReporterIdAndCreatedAtAfter(String reporterId, LocalDateTime date);

    // Đếm số report "unique" cho một nội dung cụ thể
    long countByTargetIdAndStatus(String targetId, ReportStatus status);

    // Lấy danh sách report theo status (cho Admin Queue)
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
}
