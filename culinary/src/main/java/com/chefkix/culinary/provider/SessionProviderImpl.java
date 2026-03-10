package com.chefkix.culinary.provider;

import com.chefkix.culinary.api.SessionProvider;
import com.chefkix.culinary.api.dto.SessionInfo;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.room.model.CookingRoom;
import com.chefkix.culinary.features.room.repository.CookingRoomRedisRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.service.CookingSessionService;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Provider implementation exposing cooking session data to other modules (social/post).
 * Delegates to internal CookingSessionService and RecipeRepository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionProviderImpl implements SessionProvider {

    private final CookingSessionService cookingSessionService;
    private final RecipeRepository recipeRepository;
    private final CookingRoomRedisRepository roomRepository;

    @Override
    public SessionInfo getSession(String sessionId) {
        CookingSession session = cookingSessionService.getSessionById(sessionId);
        if (session == null) {
            return null;
        }

        Recipe recipe = recipeRepository.findById(session.getRecipeId()).orElse(null);

        return SessionInfo.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .status(session.getStatus() != null ? session.getStatus().name() : null)
                .completedAt(session.getCompletedAt())
                .pendingXp(session.getPendingXp())
                .recipeId(session.getRecipeId())
                .recipeTitle(session.getRecipeTitle())
                .recipeAuthorId(recipe != null ? recipe.getUserId() : null)
                .recipeBaseXp(recipe != null ? (double) recipe.getXpReward() : null)
                .roomCode(session.getRoomCode())
                .build();
    }

    @Override
    public List<BasicProfileInfo> getCoChefs(String roomCode, String excludeUserId) {
        if (roomCode == null || roomCode.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return roomRepository.findByRoomCode(roomCode)
                    .map(CookingRoom::getParticipants)
                    .map(participants -> participants.stream()
                            .filter(p -> !"SPECTATOR".equals(p.getRole()))
                            .filter(p -> !p.getUserId().equals(excludeUserId))
                            .map(p -> BasicProfileInfo.builder()
                                    .userId(p.getUserId())
                                    .displayName(p.getDisplayName())
                                    .avatarUrl(p.getAvatarUrl())
                                    .build())
                            .toList())
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.warn("Failed to get co-chefs for room {}: {}", roomCode, e.getMessage());
            return Collections.emptyList();
        }
    }
}
