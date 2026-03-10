package com.chefkix.culinary.api;

import com.chefkix.culinary.api.dto.SessionInfo;
import com.chefkix.identity.api.dto.BasicProfileInfo;

import java.util.List;

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

    /**
     * Get co-chefs (other participants) from a co-cooking room.
     * Used by post creation to populate co-attribution.
     *
     * @param roomCode       the room code
     * @param excludeUserId  the post author's user ID (excluded from results)
     * @return list of other participants' basic profile info, or empty list
     */
    List<BasicProfileInfo> getCoChefs(String roomCode, String excludeUserId);
}
