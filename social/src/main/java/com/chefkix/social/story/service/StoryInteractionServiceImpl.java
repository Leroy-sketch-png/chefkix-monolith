package com.chefkix.social.story.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.story.dto.request.StoryReplyRequest;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.entity.StoryInteraction;
import com.chefkix.social.story.publisher.StoryEventPublisher;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryInteractionServiceImpl implements StoryInteractionService {

    private final StoryInteractionRepository interactionRepo;
    private final StoryRepository storyRepo;
    private final StoryEventPublisher publisher;

    // Hàm tiện ích nội bộ để lấy hoặc tạo mới Interaction
    private StoryInteraction getOrInitializeInteraction(String storyId, String userId) {
        return interactionRepo.findByStoryIdAndUserId(storyId, userId)
                .orElse(StoryInteraction.builder()
                        .storyId(storyId)
                        .userId(userId)
                        .build());
    }

    @Override
    public void recordView(String storyId, String userId) {
        StoryInteraction interaction = getOrInitializeInteraction(storyId, userId);
        interaction.setViewed(true);
        interaction.setLastViewedAt(Instant.now());
        interactionRepo.save(interaction);
    }

    @Override
    public void recordReaction(String storyId, String userId, String reactionType) {
        StoryInteraction interaction = getOrInitializeInteraction(storyId, userId);
        interaction.setReaction(reactionType);

        // Tùy chọn: Đánh dấu là đã xem luôn nếu user thả tim từ xa
        interaction.setViewed(true);
        interaction.setLastViewedAt(Instant.now());

        Story story = storyRepo.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));

        interactionRepo.save(interaction);
        publisher.publishStoryInteractionEvent(storyId, story.getUserId(), userId ,reactionType);
    }

    @Override
    public List<String> getViewerIds(String storyId, String ownerId) {
        // Fallback Bảo mật: Chỉ chủ nhân Story mới được xem danh sách này
        Story story = storyRepo.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story không tồn tại"));
        if (!story.getUserId().equals(ownerId)) {
            throw new RuntimeException("Chỉ chủ nhân mới được xem người xem");
        }

        return interactionRepo.findByStoryIdAndIsViewedTrueOrderByLastViewedAtDesc(storyId)
                .stream().map(StoryInteraction::getUserId).toList();
    }

    @Override
    public void replyToStory(String storyId, String replierId, StoryReplyRequest request) {
        if (request.text() == null || request.text().isBlank()) return;

        Story story = storyRepo.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story không tồn tại"));

        // Chống tự reply chính mình
        if (story.getUserId().equals(replierId)) {
            throw new RuntimeException("Bạn không thể tự reply story của chính mình");
        }

        // Tùy chọn: Lưu record vào bảng story_interactions để biết là có Reply
        StoryInteraction interaction = getOrInitializeInteraction(storyId, replierId);
        interaction.setReaction("REPLY");
        interaction.setViewed(true);
        interaction.setLastViewedAt(Instant.now());
        interactionRepo.save(interaction);

        // Bắn Event sang module Chat (Sử dụng hàm publishStoryReplyEvent trong StoryEventPublisher)
        publisher.publishStoryReplyEvent(
                story.getId(),
                story.getUserId(),
                replierId,
                request.text(),
                story.getMediaUrl() // Kèm theo ảnh để module chat vẽ UI
        );
    }
}