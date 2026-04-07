package com.chefkix.social.story.repository;

import com.chefkix.social.story.entity.StoryInteraction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryInteractionRepository extends MongoRepository<StoryInteraction, String> {
    Optional<StoryInteraction> findByStoryIdAndUserId(String storyId, String userId);
    List<StoryInteraction> findByUserIdAndStoryIdInAndIsViewedTrue(String userId, List<String> storyIds);
    // Dùng để lấy danh sách những người đã xem
    List<StoryInteraction> findByStoryIdAndIsViewedTrueOrderByLastViewedAtDesc(String storyId);
}