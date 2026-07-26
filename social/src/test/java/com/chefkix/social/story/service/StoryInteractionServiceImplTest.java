package com.chefkix.social.story.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.story.dto.request.StoryReplyRequest;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.publisher.StoryEventPublisher;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import java.time.Instant;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryInteractionServiceImplTest {

    @Mock private StoryInteractionRepository interactionRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private StoryEventPublisher publisher;

    @InjectMocks private StoryInteractionServiceImpl service;

    @Test
    void recordViewRejectsUnknownStoryBeforeCreatingInteraction() {
        givenNoActiveStory("missing-story");

        assertStoryNotFound(() -> service.recordView("missing-story", "viewer"));

        verify(interactionRepository, never()).save(any());
        verify(interactionRepository, never()).findByStoryIdAndUserId(any(), any());
    }

    @Test
    void reactionRejectsDeletedOrExpiredStoryWithoutPublishing() {
        givenNoActiveStory("inactive-story");

        assertStoryNotFound(
                () -> service.recordReaction("inactive-story", "viewer", "HEART"));

        verify(interactionRepository, never()).save(any());
        verify(publisher, never()).publishStoryInteractionEvent(any(), any(), any(), any());
    }

    @Test
    void replyRejectsDeletedOrExpiredStoryWithoutPublishing() {
        givenNoActiveStory("inactive-story");

        assertStoryNotFound(
                () ->
                        service.replyToStory(
                                "inactive-story", "viewer", new StoryReplyRequest("Looks great")));

        verify(interactionRepository, never()).save(any());
        verify(publisher, never()).publishStoryReplyEvent(any(), any(), any(), any(), any());
    }

    @Test
    void viewerHistoryAllowsUndeletedArchivedStory() {
        Story archived =
                Story.builder().id("archived-story").userId("owner").isDeleted(false).build();
        when(storyRepository.findByIdAndIsDeletedFalse("archived-story"))
                .thenReturn(Optional.of(archived));

        service.getViewerIds("archived-story", "owner");

        verify(interactionRepository)
                .findByStoryIdAndIsViewedTrueOrderByLastViewedAtDesc("archived-story");
    }

    @Test
    void viewerHistoryRejectsDeletedStory() {
        when(storyRepository.findByIdAndIsDeletedFalse("deleted-story"))
                .thenReturn(Optional.empty());

        assertStoryNotFound(() -> service.getViewerIds("deleted-story", "owner"));

        verify(interactionRepository, never())
                .findByStoryIdAndIsViewedTrueOrderByLastViewedAtDesc(any());
    }

    private void givenNoActiveStory(String storyId) {
        when(storyRepository.findByIdAndIsDeletedFalseAndExpiresAtAfter(
                        eq(storyId), any(Instant.class)))
                .thenReturn(Optional.empty());
    }

    private static void assertStoryNotFound(ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(AppException.class)
                .satisfies(
                        error ->
                                assertThat(((AppException) error).getErrorCode())
                                        .isEqualTo(ErrorCode.STORY_NOT_FOUND));
    }
}
