package com.chefkix.social.story.repository;

import com.chefkix.social.story.entity.StoryHighlight;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryHighlightRepository extends MongoRepository<StoryHighlight, String> {
    List<StoryHighlight> findByUserIdOrderByCreatedAtDesc(String userId);

    void deleteAllByUserId(String userId);
}