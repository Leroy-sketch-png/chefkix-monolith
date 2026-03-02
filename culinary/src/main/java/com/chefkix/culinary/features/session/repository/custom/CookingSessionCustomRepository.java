package com.chefkix.culinary.features.session.repository.custom;

import com.chefkix.culinary.common.dto.query.SessionHistoryQuery;
import com.chefkix.culinary.features.session.entity.CookingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CookingSessionCustomRepository {

    /**
     * Thực thi truy vấn động cho Session History.
     */
    Page<CookingSession> findSessionHistory(String userId, SessionHistoryQuery dto, Pageable pageable);

}