package com.chefkix.social.story.repository;

import com.chefkix.social.story.entity.StoryHighlight;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryHighlightRepository extends MongoRepository<StoryHighlight, String> {
    // Tìm toàn bộ Highlight của 1 User, cái nào tạo sau thì hiện lên đầu
    List<StoryHighlight> findByUserIdOrderByCreatedAtDesc(String userId);
}