package com.chefkix.culinary.features.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.enums.RecipeVisibility;
import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.helper.AsyncHelper;
import com.chefkix.culinary.common.helper.RecipeHelper;
import com.chefkix.culinary.features.interaction.service.InteractionService;
import com.chefkix.culinary.features.recipe.dto.response.RecentCookResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSocialProofResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.identity.api.ProfileProvider;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RecipeServiceTest {

    @Mock
    private AsyncHelper asyncHelper;
    @Mock
    private ProfileProvider profileProvider;
    @Mock
    private RecipeMapper recipeMapper;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private RecipeHelper recipeHelper;
    @Mock
    private InteractionService interactionService;
    @Mock
    private CookingSessionRepository cookingSessionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RecipeService recipeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRecipeSocialProofRedactsDeletedCookerIdentity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer-1", null, List.of()));

        Recipe recipe = Recipe.builder()
                .id("recipe-1")
                .userId("author-1")
                .status(RecipeStatus.PUBLISHED)
                .recipeVisibility(RecipeVisibility.PUBLIC)
                .cookCount(12)
                .averageRating(4.6)
                .build();
        CookingSession deletedSession = CookingSession.builder()
                .id("session-1")
                .userId("deleted-user")
                .recipeId("recipe-1")
                .status(SessionStatus.POST_DELETED)
                .completedAt(LocalDateTime.of(2026, 4, 23, 10, 0))
                .userDeleted(true)
                .build();

        when(recipeRepository.findById("recipe-1")).thenReturn(java.util.Optional.of(recipe));
        when(cookingSessionRepository.countByRecipeIdAndStatus("recipe-1", SessionStatus.POSTED)).thenReturn(0L);
        when(cookingSessionRepository.findByRecipeIdAndStatusIn(eq("recipe-1"), any(List.class), any()))
                .thenReturn(new PageImpl<>(List.of(deletedSession)));

        RecipeSocialProofResponse response = recipeService.getRecipeSocialProof("recipe-1");

        assertThat(response.getRecentCookers()).hasSize(1);
        assertThat(response.getRecentCookers().get(0).getUserId()).isNull();
        assertThat(response.getRecentCookers().get(0).getDisplayName()).isEqualTo("Deleted User");
        assertThat(response.getRecentCookers().get(0).getUsername()).isNull();
        assertThat(response.getRecentCookers().get(0).getAvatarUrl()).isNull();
        verify(profileProvider, never()).getBasicProfile("deleted-user");
    }

    @Test
    void getRecentCooksOfMyRecipesRedactsDeletedCookerIdentity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("author-1", null, List.of()));

        Recipe recipe = Recipe.builder()
                .id("recipe-1")
                .userId("author-1")
                .status(RecipeStatus.PUBLISHED)
                .build();
        CookingSession deletedSession = CookingSession.builder()
                .id("session-1")
                .userId("deleted-user")
                .recipeId("recipe-1")
                .recipeTitle("Braised Beans")
                .status(SessionStatus.COMPLETED)
                .completedAt(LocalDateTime.of(2026, 4, 23, 10, 0))
                .baseXpAwarded(45.0)
                .rating(4)
                .userDeleted(true)
                .build();

        when(recipeRepository.findByUserIdAndStatus("author-1", RecipeStatus.PUBLISHED)).thenReturn(List.of(recipe));
        when(cookingSessionRepository.findByRecipeIdInAndStatus(eq(List.of("recipe-1")), eq(SessionStatus.COMPLETED), any()))
                .thenReturn(new PageImpl<>(List.of(deletedSession)));

        RecentCookResponse response = recipeService.getRecentCooksOfMyRecipes(0, 10);

        assertThat(response.getCooks()).hasSize(1);
        assertThat(response.getCooks().get(0).getCookUserId()).isNull();
        assertThat(response.getCooks().get(0).getCookDisplayName()).isEqualTo("Deleted User");
        assertThat(response.getCooks().get(0).getCookUsername()).isNull();
        assertThat(response.getCooks().get(0).getCookAvatarUrl()).isNull();
        verify(profileProvider, never()).getBasicProfile("deleted-user");
    }
}