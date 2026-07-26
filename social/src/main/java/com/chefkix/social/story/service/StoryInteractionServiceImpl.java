package com.chefkix.social.story.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.story.dto.request.StoryReplyRequest;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.entity.StoryInteraction;
import com.chefkix.social.story.publisher.StoryEventPublisher;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoryInteractionServiceImpl implements StoryInteractionService {

    private final StoryInteractionRepository interactionRepo;
    private final StoryRepository storyRepo;
    private final StoryEventPublisher publisher;

    private StoryInteraction getOrInitializeInteraction(String storyId, String userId) {
        return interactionRepo.findByStoryIdAndUserId(storyId, userId)
                .orElse(
                        StoryInteraction.builder()
                                .storyId(storyId)
                                .userId(userId)
                                .build());
    }

    private Story getActiveStory(String storyId) {
        return storyRepo.findByIdAndIsDeletedFalseAndExpiresAtAfter(storyId, Instant.now())
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
    }

    private Story getUndeletedStory(String storyId) {
        return storyRepo.findByIdAndIsDeletedFalse(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
    }

    @Override
    public void recordView(String storyId, String userId) {
        getActiveStory(storyId);
        StoryInteraction interaction = getOrInitializeInteraction(storyId, userId);
        interaction.setViewed(true);
        interaction.setLastViewedAt(Instant.now());
        interactionRepo.save(interaction);
    }

    @Override
    public void recordReaction(String storyId, String userId, String reactionType) {
        Story story = getActiveStory(storyId);
        StoryInteraction interaction = getOrInitializeInteraction(storyId, userId);
        interaction.setReaction(reactionType);
        interaction.setViewed(true);
        interaction.setLastViewedAt(Instant.now());

        interactionRepo.save(interaction);
        publisher.publishStoryInteractionEvent(
                storyId, story.getUserId(), userId, reactionType);
    }

    @Override
    public List<String> getViewerIds(String storyId, String ownerId) {
        Story story = getUndeletedStory(storyId);
        if (!story.getUserId().equals(ownerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return interactionRepo.findByStoryIdAndIsViewedTrueOrderByLastViewedAtDesc(storyId)
                .stream()
                .map(StoryInteraction::getUserId)
                .toList();
    }

    @Override
    public void replyToStory(String storyId, String replierId, StoryReplyRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            return;
        }

        Story story = getActiveStory(storyId);
        if (story.getUserId().equals(replierId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        StoryInteraction interaction = getOrInitializeInteraction(storyId, replierId);
        interaction.setReaction("REPLY");
        interaction.setViewed(true);
        interaction.setLastViewedAt(Instant.now());
        interactionRepo.save(interaction);

        publisher.publishStoryReplyEvent(
                story.getId(),
                story.getUserId(),
                replierId,
                request.text(),
                story.getMediaUrl());
    }
}
