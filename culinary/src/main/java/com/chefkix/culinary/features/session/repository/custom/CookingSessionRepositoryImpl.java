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
     */
    public Page<CookingSession> findSessionHistory(String userId, SessionHistoryQuery dto, Pageable pageable) {

        Query query = new Query(SessionSpecification.getCriteria(userId, dto));

        Sort sort = Sort.by(Sort.Direction.DESC, "completedAt", "startedAt");
        query.with(sort);

        long total = mongoTemplate.count(query, CookingSession.class);

        query.with(pageable);

        List<CookingSession> sessions = mongoTemplate.find(query, CookingSession.class);

        return new PageImpl<>(sessions, pageable, total);
    }
}