package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.Report;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {

    /**
     */
    long countByTargetTypeAndTargetId(String targetType, String targetId);

    /**
     */
    Optional<Report> findByReporterIdAndTargetTypeAndTargetId(
            String reporterId, String targetType, String targetId);

    /**
     */
    long countByReporterIdAndCreatedAtAfter(String reporterId, Instant since);

    /**
     */
    List<Report> findByTargetTypeAndTargetId(String targetType, String targetId);

    long deleteAllByTargetTypeAndTargetId(String targetType, String targetId);

    /**
     */
    List<Report> findByStatus(String status);
}
