package com.chefkix.culinary.features.session.repository.custom;

import com.chefkix.culinary.common.dto.query.SessionHistoryQuery;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.common.specification.SessionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CookingSessionRepositoryImpl implements CookingSessionCustomRepository {

    private final MongoTemplate mongoTemplate;

    /**
     * Execute dynamic query for Session History.
     */
    public Page<CookingSession> findSessionHistory(String userId, SessionHistoryQuery dto, Pageable pageable) {

        // 1. Build Criteria (WHERE clause)
        Query query = new Query(SessionSpecification.getCriteria(userId, dto));

        // 2. Add Sorting (Sort by completedAt or startedAt, newest first)
        Sort sort = Sort.by(Sort.Direction.DESC, "completedAt", "startedAt");
        query.with(sort);

        // 3. Calculate total count
        long total = mongoTemplate.count(query, CookingSession.class);

        // 4. Apply Pagination (SKIP and LIMIT)
        query.with(pageable);

        // 5. Execute query
        List<CookingSession> sessions = mongoTemplate.find(query, CookingSession.class);

        // 6. Return paginated results
        return new PageImpl<>(sessions, pageable, total);
    }
}