package com.chefkix.culinary.features.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.client.AIRestClient;
import com.chefkix.culinary.features.ai.dto.internal.AIModerationRequest;
import com.chefkix.culinary.features.ai.dto.internal.AIModerationResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiIntegrationServiceModerationTest {

    @Mock AIRestClient aiRestClient;
    @Mock RecipeMapper recipeMapper;
    @Mock RecipeRepository recipeRepository;

    @Test
    void flaggedRecipeIsNotApprovedForPublishing() {
        AIModerationResponse response = response("flag");
        when(aiRestClient.moderateContent(any(AIModerationRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> service().moderateRecipeContent(recipe()))
                .isInstanceOfSatisfying(AppException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RECIPE_MODERATION_FAILED));
    }

    @Test
    void unknownRecipeModerationActionFailsClosed() {
        when(aiRestClient.moderateContent(any(AIModerationRequest.class))).thenReturn(response("review"));

        assertThatThrownBy(() -> service().moderateRecipeContent(recipe()))
                .isInstanceOfSatisfying(AppException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RECIPE_MODERATION_FAILED));
    }

    @Test
    void missingRecipeModerationResponseFailsClosed() {
        when(aiRestClient.moderateContent(any(AIModerationRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> service().moderateRecipeContent(recipe()))
                .isInstanceOfSatisfying(AppException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RECIPE_MODERATION_FAILED));
    }

    @Test
    void explicitlyApprovedRecipePassesModeration() {
        AIModerationResponse response = response("approve");
        when(aiRestClient.moderateContent(any(AIModerationRequest.class))).thenReturn(response);

        assertThat(service().moderateRecipeContent(recipe())).isSameAs(response);
    }

    private AiIntegrationService service() {
        return new AiIntegrationService(aiRestClient, recipeMapper, recipeRepository);
    }

    private static Recipe recipe() {
        return Recipe.builder().id("recipe-1").userId("creator-1").title("Test recipe").build();
    }

    private static AIModerationResponse response(String action) {
        AIModerationResponse response = new AIModerationResponse();
        response.setAction(action);
        return response;
    }
}
