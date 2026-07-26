package com.chefkix.social.story.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryServiceImplTest {

    @Mock private StoryRepository storyRepository;
    @Mock private StoryMapper storyMapper;

    @InjectMocks private StoryServiceImpl service;

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
