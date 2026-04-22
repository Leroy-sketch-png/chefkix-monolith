package com.chefkix.social.post.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chefkix.social.post.entity.BattleVote;

@Repository
public interface BattleVoteRepository extends MongoRepository<BattleVote, String> {
    Optional<BattleVote> findByPostIdAndUserId(String postId, String userId);
    List<BattleVote> findByPostIdInAndUserId(List<String> postIds, String userId);
    List<BattleVote> findAllByUserId(String userId);
    boolean existsByPostIdAndUserId(String postId, String userId);
    void deleteAllByPostId(String postId);
}
