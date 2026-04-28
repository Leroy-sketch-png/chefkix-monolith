package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.ReplyLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReplyLikeRepository extends MongoRepository<ReplyLike, String> {
    Optional<ReplyLike> findByReplyIdAndUserId(String replyId, String userId);
    
    boolean existsByReplyIdAndUserId(String replyId, String userId);
    
    void deleteByReplyIdAndUserId(String replyId, String userId);
    
    void deleteAllByReplyId(String replyId);

    void deleteAllByReplyIdIn(Collection<String> replyIds);

    List<ReplyLike> findAllByUserId(String userId);
    
    long countByReplyId(String replyId);
}
