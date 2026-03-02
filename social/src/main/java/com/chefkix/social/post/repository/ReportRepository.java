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
     * Count reports for a specific target (post/comment/recipe).
     */
    long countByTargetTypeAndTargetId(String targetType, String targetId);

    /**
     * Check if a user already reported this target.
     */
    Optional<Report> findByReporterIdAndTargetTypeAndTargetId(
            String reporterId, String targetType, String targetId);

    /**
     * Check if user has already reported X times today (rate limiting).
     */
    long countByReporterIdAndCreatedAtAfter(String reporterId, Instant since);

    /**
     * Find all reports for a specific target.
     */
    List<Report> findByTargetTypeAndTargetId(String targetType, String targetId);

    /**
     * Find all pending reports for admin review.
     */
    List<Report> findByStatus(String status);
}
