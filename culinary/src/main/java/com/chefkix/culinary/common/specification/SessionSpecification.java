package com.chefkix.culinary.common.specification;

import com.chefkix.culinary.common.dto.query.SessionHistoryQuery;
import com.chefkix.culinary.common.enums.SessionStatus;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SessionSpecification {

    /**
     * Create Criteria to filter Session History by userId and status.
     * @param userId User ID (Required)
     * @param query Status string ("completed", "posted", "all",...)
     * @return Complete MongoDB Criteria
     */
    public static Criteria getCriteria(String userId, SessionHistoryQuery query) {
        List<Criteria> criteriaList = new ArrayList<>();
        String statusFilter = query != null ? query.getEffectiveStatusFilter() : null;

        // 1. Filter by User ID (REQUIRED)
        criteriaList.add(Criteria.where("userId").is(userId));

        // 2. Filter by Status (statusFilter)
        if (StringUtils.hasText(statusFilter) && !"all".equalsIgnoreCase(statusFilter)) {
            try {
                SessionStatus status = SessionStatus.fromValue(statusFilter);
                criteriaList.add(Criteria.where("status").is(status));
            } catch (RuntimeException e) {
                // Ignore if value is invalid, or throw Exception (optional)
                // Currently, we filter by a non-existent value so query returns empty.
                criteriaList.add(Criteria.where("status").is("INVALID_STATUS_FILTER"));
            }
        }

        // 3. Combine all conditions using AND operator
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}