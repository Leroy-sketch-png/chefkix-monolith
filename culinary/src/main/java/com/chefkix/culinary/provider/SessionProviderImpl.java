package com.chefkix.culinary.provider;

import com.chefkix.culinary.api.SessionProvider;
import com.chefkix.culinary.api.dto.SessionInfo;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.service.CookingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
                .build();
    }
}
