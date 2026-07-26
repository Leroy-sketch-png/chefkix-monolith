package com.chefkix.culinary.common.specification;

import com.chefkix.culinary.common.dto.query.SessionHistoryQuery;
import com.chefkix.culinary.common.enums.SessionStatus;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SessionSpecification {

    /**
     */
    public static Criteria getCriteria(String userId, SessionHistoryQuery query) {
        List<Criteria> criteriaList = new ArrayList<>();
        String statusFilter = query != null ? query.getEffectiveStatusFilter() : null;

        criteriaList.add(Criteria.where("userId").is(userId));

        if (StringUtils.hasText(statusFilter) && !"all".equalsIgnoreCase(statusFilter)) {
            try {
                SessionStatus status = SessionStatus.fromValue(statusFilter);
                criteriaList.add(Criteria.where("status").is(status));
            } catch (RuntimeException e) {
                criteriaList.add(Criteria.where("status").is("INVALID_STATUS_FILTER"));
            }
        }

        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}