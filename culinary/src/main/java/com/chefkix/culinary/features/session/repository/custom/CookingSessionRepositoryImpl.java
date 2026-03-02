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
     * Thực thi truy vấn động cho Session History.
     */
    public Page<CookingSession> findSessionHistory(String userId, SessionHistoryQuery dto, Pageable pageable) {

        // 1. Tạo Criteria (Mệnh đề WHERE)
        Query query = new Query(SessionSpecification.getCriteria(userId, dto));

        // 2. Thêm Sắp xếp (Sort: Sắp xếp theo completedAt hoặc startedAt mới nhất)
        Sort sort = Sort.by(Sort.Direction.DESC, "completedAt", "startedAt");
        query.with(sort);

        // 3. Tính tổng số lượng
        long total = mongoTemplate.count(query, CookingSession.class);

        // 4. Áp dụng Phân trang (SKIP và LIMIT)
        query.with(pageable);

        // 5. Thực thi truy vấn
        List<CookingSession> sessions = mongoTemplate.find(query, CookingSession.class);

        // 6. Trả về kết quả đã được phân trang
        return new PageImpl<>(sessions, pageable, total);
    }
}