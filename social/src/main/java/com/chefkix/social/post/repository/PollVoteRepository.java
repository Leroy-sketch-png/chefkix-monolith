package com.chefkix.social.post.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chefkix.social.post.entity.PollVote;

@Repository
public interface PollVoteRepository extends MongoRepository<PollVote, String> {
    Optional<PollVote> findByPostIdAndUserId(String postId, String userId);
    List<PollVote> findByPostIdInAndUserId(List<String> postIds, String userId);
    boolean existsByPostIdAndUserId(String postId, String userId);
    void deleteAllByPostId(String postId);
}
