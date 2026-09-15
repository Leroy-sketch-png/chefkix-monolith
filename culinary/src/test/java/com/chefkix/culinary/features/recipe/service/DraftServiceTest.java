package com.chefkix.culinary.features.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.helper.AsyncHelper;
import com.chefkix.culinary.features.ai.service.AiIntegrationService;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.mapper.IngredientMapper;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.mapper.StepMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import java.util.List;
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
