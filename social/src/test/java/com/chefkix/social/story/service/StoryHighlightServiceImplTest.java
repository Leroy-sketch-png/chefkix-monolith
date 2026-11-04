package com.chefkix.social.story.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.policy.BlockedContentPolicy;
import com.chefkix.social.story.dto.request.HighlightCreateRequest;
import com.chefkix.social.story.dto.request.HighlightUpdateRequest;
import com.chefkix.social.story.dto.response.HighlightResponse;
import com.chefkix.social.story.entity.StoryHighlight;
import com.chefkix.social.story.mapper.StoryHighlightMapper;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryHighlightRepository;
import com.chefkix.social.story.repository.StoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryHighlightServiceImplTest {

    @Mock private StoryHighlightRepository highlightRepo;
    @Mock private StoryRepository storyRepo;
    @Mock private StoryMapper storyMapper;
    @Mock private StoryHighlightMapper storyHighlightMapper;
    @Mock private BlockedContentPolicy blockedContentPolicy;

    @InjectMocks private StoryHighlightServiceImpl service;

    @Test
    void createHighlightPreservesNonblankTitle() {
        when(highlightRepo.save(any(StoryHighlight.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storyHighlightMapper.toHighlightResponse(any(StoryHighlight.class)))
                .thenReturn(new HighlightResponse("highlight-1", "Family dinner", null, 0));

        service.createHighlight("owner", new HighlightCreateRequest(" Family dinner ", null, List.of()));

        ArgumentCaptor<StoryHighlight> saved = ArgumentCaptor.forClass(StoryHighlight.class);
        verify(highlightRepo).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Family dinner");
    }

    @Test
    void createHighlightDefaultsNullOrBlankTitle() {
        when(highlightRepo.save(any(StoryHighlight.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storyHighlightMapper.toHighlightResponse(any(StoryHighlight.class)))
                .thenReturn(new HighlightResponse("highlight-1", "highlights", null, 0));

        service.createHighlight("owner", new HighlightCreateRequest(null, null, List.of()));
        service.createHighlight("owner", new HighlightCreateRequest("  ", null, List.of()));

        ArgumentCaptor<StoryHighlight> saved = ArgumentCaptor.forClass(StoryHighlight.class);
        verify(highlightRepo, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(StoryHighlight::getTitle)
                .containsExactly("highlights", "highlights");
    }

    @Test
    void createHighlightRejectsUnavailableStoriesBeforePersistence() {
        when(storyRepo.countByIdInAndUserId(List.of("foreign-story"), "owner")).thenReturn(0L);

        assertThatThrownBy(() -> service.createHighlight(
                        "owner", new HighlightCreateRequest("Dinner", null, List.of("foreign-story"))))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.STORY_HIGHLIGHT_STORIES_INVALID));

        verify(highlightRepo, never()).save(any(StoryHighlight.class));
    }

    @Test
    void updateHighlightRejectsUnavailableStoriesBeforePersistence() {
        StoryHighlight highlight = StoryHighlight.builder()
                .id("highlight-1")
                .userId("owner")
                .title("Dinner")
                .storyIds(List.of())
                .build();
        when(highlightRepo.findById("highlight-1")).thenReturn(Optional.of(highlight));
        when(storyRepo.countByIdInAndUserId(List.of("foreign-story"), "owner")).thenReturn(0L);

        assertThatThrownBy(() -> service.updateHighlight(
                        "highlight-1", "owner", new HighlightUpdateRequest(null, null, List.of("foreign-story"))))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.STORY_HIGHLIGHT_STORIES_INVALID));

        verify(highlightRepo, never()).save(any(StoryHighlight.class));
    }

    @Test
    void missingHighlightUsesNotFoundForReadAndMutation() {
        when(highlightRepo.findById("missing")).thenReturn(Optional.empty());

        assertNotFound(() -> service.getStoriesInHighlight("viewer", "missing"));
        assertNotFound(() -> service.updateHighlight(
                "missing", "owner", new HighlightUpdateRequest(null, null, null)));
        assertNotFound(() -> service.deleteHighlight("missing", "owner"));

        verify(highlightRepo, never()).save(any(StoryHighlight.class));
        verify(highlightRepo, never()).delete(any(StoryHighlight.class));
    }

    @Test
    void anotherUsersHighlightIsIndistinguishableFromMissing() {
        when(highlightRepo.findById("private-highlight"))
                .thenReturn(Optional.of(StoryHighlight.builder()
                        .id("private-highlight")
                        .userId("other-owner")
                        .title("Private")
                        .build()));

        assertNotFound(() -> service.updateHighlight(
                "private-highlight", "viewer", new HighlightUpdateRequest(null, null, null)));
        assertNotFound(() -> service.deleteHighlight("private-highlight", "viewer"));

        verify(highlightRepo, never()).save(any(StoryHighlight.class));
        verify(highlightRepo, never()).delete(any(StoryHighlight.class));
    }

    @Test
    void blockedOwnerHighlightsAreHiddenBeforeReturningContent() {
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .when(blockedContentPolicy).requireVisible("viewer", "blocked-owner");
        when(highlightRepo.findById("highlight-1"))
                .thenReturn(Optional.of(StoryHighlight.builder()
                        .id("highlight-1")
                        .userId("blocked-owner")
                        .storyIds(List.of("story-1"))
                        .build()));

        assertThatThrownBy(() -> service.getUserHighlights("viewer", "blocked-owner"))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.getStoriesInHighlight("viewer", "highlight-1"))
                .isInstanceOf(AppException.class);

        verify(storyRepo, never()).findByIdInAndIsDeletedFalse(any());
    }

    private void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.STORY_HIGHLIGHT_NOT_FOUND));
    }
}
