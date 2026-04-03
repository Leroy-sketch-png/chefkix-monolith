package com.chefkix.social.story.service;

import com.chefkix.social.story.entity.StoryInteraction;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StoryInteractionServiceImpl implements StoryInteractionService {

    private final StoryInteractionRepository interactionRepo;
    private final StoryRepository storyRepo;

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

        interactionRepo.save(interaction);
        // TODO: Bắn Event sang Notification Service để báo cho chủ Story biết
    }
//
//    @Override
//    public List<String> getViewerIds(String storyId, String ownerId) {
//        // Fallback Bảo mật: Chỉ chủ nhân Story mới được xem danh sách này
//        Story story = storyRepo.findById(storyId)
//                .orElseThrow(() -> new RuntimeException("Story không tồn tại"));
//        if (!story.getUserId().equals(ownerId)) {
//            throw new RuntimeException("Chỉ chủ nhân mới được xem người xem");
//        }
//
//        return interactionRepo.findByStoryIdAndIsViewedTrueOrderByLastViewedAtDesc(storyId)
//                .stream().map(StoryInteraction::getUserId).toList();
//    }
}