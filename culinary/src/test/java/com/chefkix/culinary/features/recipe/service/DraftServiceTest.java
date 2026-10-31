package com.chefkix.culinary.features.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.helper.AsyncHelper;
import com.chefkix.culinary.features.ai.service.AiIntegrationService;
import com.chefkix.culinary.features.recipe.dto.request.RecipePublishRequest;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.entity.Ingredient;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.entity.Step;
import com.chefkix.culinary.features.recipe.mapper.IngredientMapper;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.mapper.StepMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

    @Mock RecipeRepository recipeRepository;
    @Mock RecipeMapper recipeMapper;
    @Mock StepMapper stepMapper;
    @Mock IngredientMapper ingredientMapper;
    @Mock AsyncHelper asyncHelper;
    @Mock AiIntegrationService aiIntegrationService;
    @Mock ApplicationEventPublisher eventPublisher;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preservesVerifiedAuthorInDraftSummaries() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("creator-1", null));
        Recipe draft = Recipe.builder().id("draft-1").userId("creator-1").build();
        AuthorResponse author = AuthorResponse.builder()
                .userId("creator-1")
                .username("minh")
                .displayName("Minh Tran")
                .verified(true)
                .build();
        when(asyncHelper.getProfileAsync("creator-1"))
                .thenReturn(CompletableFuture.completedFuture(author));
        when(recipeRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                "creator-1", RecipeStatus.DRAFT)).thenReturn(List.of(draft));
        when(recipeMapper.toRecipeSummaryResponse(draft))
                .thenReturn(RecipeSummaryResponse.builder().id("draft-1").build());

        List<RecipeSummaryResponse> summaries = service().getMyDrafts();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().getAuthor().isVerified()).isTrue();
    }

    @Test
    void doesNotScoreSaveOrIndexRecipeWhenModerationRejectsIt() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("creator-1", null));
        Recipe draft = Recipe.builder()
                .id("draft-1")
                .userId("creator-1")
                .title("Tomato pasta")
                .coverImageUrl(List.of("/cover.jpg"))
                .fullIngredientList(List.of(new Ingredient("tomato", "2", "cups")))
                .steps(List.of(new Step()))
                .servings(2)
                .build();
        when(recipeRepository.findById("draft-1")).thenReturn(Optional.of(draft));
        doThrow(new AppException(ErrorCode.RECIPE_MODERATION_FAILED))
                .when(aiIntegrationService)
                .moderateRecipeContent(draft);

        assertThatThrownBy(() ->
                        service().publishRecipe("draft-1", new RecipePublishRequest()))
                .isInstanceOfSatisfying(
                        AppException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.RECIPE_MODERATION_FAILED));

        verify(aiIntegrationService).validateRecipeForPublish(draft);
        verify(aiIntegrationService).moderateRecipeContent(draft);
        verify(aiIntegrationService, never()).scoreRecipeQuality(draft);
        verify(recipeRepository, never()).save(draft);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void autoSaveDraftThrowsRecipeConcurrentUpdateOnOptimisticLockingFailure() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("creator-1", null));
        Recipe draft = Recipe.builder()
                .id("draft-1")
                .userId("creator-1")
                .status(RecipeStatus.DRAFT)
                .version(2L)
                .build();
        when(recipeRepository.findById("draft-1")).thenReturn(Optional.of(draft));
        when(recipeRepository.save(draft)).thenThrow(
                new org.springframework.dao.OptimisticLockingFailureException("Concurrent draft save conflict"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service().autoSaveDraft("draft-1", new com.chefkix.culinary.features.recipe.dto.request.RecipeRequest()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECIPE_CONCURRENT_UPDATE);
    }

    private DraftService service() {
        return new DraftService(
                recipeRepository,
                recipeMapper,
                stepMapper,
                ingredientMapper,
                asyncHelper,
                aiIntegrationService,
                eventPublisher);
    }
}
