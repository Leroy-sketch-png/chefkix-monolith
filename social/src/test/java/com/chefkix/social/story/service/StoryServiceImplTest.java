package com.chefkix.social.story.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.dto.RecipeSummaryInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.story.dto.request.StoryCreateRequest;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryRepository;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryServiceImplTest {

    @Mock private StoryRepository storyRepository;
    @Mock private StoryMapper storyMapper;
    @Mock private RecipeProvider recipeProvider;

    @InjectMocks private StoryServiceImpl service;

    @Test
    void createStoryPersistsVerifiedPublicRecipeLink() {
        StoryCreateRequest request = requestWithRecipe("recipe-1");
        when(recipeProvider.getPublicRecipeSummary("recipe-1"))
                .thenReturn(RecipeSummaryInfo.builder().id("recipe-1").title("Pho").build());
        when(storyMapper.toStoryItems(List.of())).thenReturn(List.of());
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storyMapper.toStoryResponse(any(Story.class)))
                .thenReturn(new StoryResponse(
                        "story-1", "owner", "media", "IMAGE", 1.0, 0.0,
                        "recipe-1", List.of(), null, null));

        StoryResponse response = service.createStory("owner", request, "media");

        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(storyCaptor.capture());
        assertThat(storyCaptor.getValue().getRecipeId()).isEqualTo("recipe-1");
        assertThat(response.linkedRecipeId()).isEqualTo("recipe-1");
    }

    @Test
    void createStoryKeepsRecipeOptional() {
        StoryCreateRequest request = requestWithRecipe(null);
        when(storyMapper.toStoryItems(List.of())).thenReturn(List.of());
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createStory("owner", request, "media");

        verify(recipeProvider, never()).getPublicRecipeSummary(any());
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(storyCaptor.capture());
        assertThat(storyCaptor.getValue().getRecipeId()).isNull();
    }

    @Test
    void createStoryTreatsBlankRecipeLinkAsOptional() {
        StoryCreateRequest request = requestWithRecipe("   ");
        when(storyMapper.toStoryItems(List.of())).thenReturn(List.of());
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createStory("owner", request, "media");

        verify(recipeProvider, never()).getPublicRecipeSummary(any());
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(storyCaptor.capture());
        assertThat(storyCaptor.getValue().getRecipeId()).isNull();
    }

    @Test
    void createStoryRejectsMissingOrNonPublicRecipeBeforePersistence() {
        StoryCreateRequest request = requestWithRecipe("private-recipe");
        when(recipeProvider.getPublicRecipeSummary("private-recipe")).thenReturn(null);

        assertThatThrownBy(() -> service.createStory("owner", request, "media"))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.RECIPE_NOT_FOUND));

        verify(storyRepository, never()).save(any());
    }

    private StoryCreateRequest requestWithRecipe(String recipeId) {
        return new StoryCreateRequest("IMAGE", 1.0, 0.0, recipeId, List.of());
    }

    @Test
    void archiveStoryEarlyRejectsDeletedOrWrongOwnerStory() {
        when(storyRepository.findByIdAndUserIdAndIsDeletedFalse("removed-story", "owner"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiveStoryEarly("removed-story", "owner"))
                .isInstanceOf(AppException.class)
                .satisfies(
                        error ->
                                assertThat(((AppException) error).getErrorCode())
                                        .isEqualTo(ErrorCode.STORY_NOT_FOUND));

        verify(storyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
