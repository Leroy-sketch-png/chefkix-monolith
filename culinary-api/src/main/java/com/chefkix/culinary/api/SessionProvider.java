package com.chefkix.culinary.api;

import com.chefkix.culinary.api.dto.SessionInfo;

/**
 * Cross-module contract for cooking session operations.
 * <p>
 * Implemented by {@code culinary} module, consumed by {@code social} module.
 * Replaces: RecipeClient Feign client in post-service.
 */
public interface SessionProvider {

    /**
     * Get cooking session details for post creation / post linking.
     * Replaces: {@code GET /cooking-sessions/internal/{sessionId}}
     * (recipe-service's internal endpoint consumed by post-service).
     *
     * @param sessionId the cooking session ID
     * @return session info including recipe metadata, or null if not found
     */
    SessionInfo getSession(String sessionId);
}
