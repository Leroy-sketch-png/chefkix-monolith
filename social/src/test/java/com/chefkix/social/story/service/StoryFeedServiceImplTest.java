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
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import java.time.Instant;
import java.util.Optional;
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

    @InjectMocks private StoryFeedServiceImpl service;

    @Test
    void getStoryByIdRejectsStoryOutsidePublicActiveWindow() {
        when(storyRepository.findByIdAndIsDeletedFalseAndExpiresAtAfter(
                        eq("removed-story"), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStoryById("removed-story"))
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

        service.getStoryById("active-story");

        verify(storyMapper).toStoryResponse(story);
    }
}
