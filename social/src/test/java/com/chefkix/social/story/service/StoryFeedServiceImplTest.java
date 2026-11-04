package com.chefkix.social.story.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.policy.BlockedContentPolicy;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.doThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryFeedServiceImplTest {

    @Mock private StoryRepository storyRepository;
    @Mock private StoryInteractionRepository storyInteractionRepository;
    @Mock private StoryMapper storyMapper;
    @Mock private ProfileProvider profileProvider;
    @Mock private BlockedContentPolicy blockedContentPolicy;

    @InjectMocks private StoryFeedServiceImpl service;

    @Test
    void getStoryByIdRejectsStoryOutsidePublicActiveWindow() {
        when(storyRepository.findByIdAndIsDeletedFalseAndExpiresAtAfter(
                        eq("removed-story"), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStoryById("removed-story", "viewer"))
                .isInstanceOf(AppException.class)
                .satisfies(
                        error ->
                                assertThat(((AppException) error).getErrorCode())
                                        .isEqualTo(ErrorCode.STORY_NOT_FOUND));

        verifyNoInteractions(storyMapper);
    }

    @Test
    void getStoryByIdMapsActiveStory() {
        Story story = Story.builder().id("active-story").isDeleted(false).build();
        when(storyRepository.findByIdAndIsDeletedFalseAndExpiresAtAfter(
                        eq("active-story"), any(Instant.class)))
                .thenReturn(Optional.of(story));

        service.getStoryById("active-story", "viewer");

        verify(storyMapper).toStoryResponse(story);
    }

    @Test
    void getStoryByIdRejectsBlockedOwnerBeforeMapping() {
        Story story = Story.builder().id("active-story").userId("blocked-owner").isDeleted(false).build();
        when(storyRepository.findByIdAndIsDeletedFalseAndExpiresAtAfter(
                        eq("active-story"), any(Instant.class)))
                .thenReturn(Optional.of(story));
        doThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .when(blockedContentPolicy).requireVisible("viewer", "blocked-owner");

        assertThatThrownBy(() -> service.getStoryById("active-story", "viewer"))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.POST_NOT_FOUND));

        verifyNoInteractions(storyMapper);
    }

    @Test
    void getUserStoriesRejectsBlockedOwnerBeforeRepositoryRead() {
        doThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .when(blockedContentPolicy).requireVisible("viewer", "blocked-owner");

        assertThatThrownBy(() -> service.getUserActiveStories("viewer", "blocked-owner"))
                .isInstanceOf(AppException.class);

        verifyNoInteractions(storyRepository, storyMapper);
    }

    @Test
    void feedExcludesInvisibleAuthorsButKeepsTheViewersOwnStories() {
        when(profileProvider.getFollowingIds("viewer")).thenReturn(List.of("friend", "blocked"));
        when(blockedContentPolicy.invisibleUserIds("viewer")).thenReturn(List.of("blocked", "viewer"));
        when(storyRepository.findByUserIdInAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(
                        eq(Set.of("viewer", "friend")), any(Instant.class)))
                .thenReturn(List.of());

        service.getStoryFeed("viewer");

        verify(storyRepository).findByUserIdInAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(
                eq(Set.of("viewer", "friend")), any(Instant.class));
    }
}
