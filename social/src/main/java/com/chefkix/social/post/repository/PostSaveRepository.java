package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.PostSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostSaveRepository extends MongoRepository<PostSave, String> {
    PostSave findByPostIdAndUserId(String postId, String userId);
    
    boolean existsByPostIdAndUserId(String postId, String userId);
    
    void deleteByPostIdAndUserId(String postId, String userId);
    
    List<PostSave> findByUserIdOrderByCreatedDateDesc(String userId);
    
    /**
     * Get saved posts for a user with pagination.
     */
    Page<PostSave> findByUserIdOrderByCreatedDateDesc(String userId, Pageable pageable);
    
    long countByPostId(String postId);

    /**
     * Batch: find all saves by a user for a set of posts (eliminates N+1).
     */
    List<PostSave> findByUserIdAndPostIdIn(String userId, List<String> postIds);
}
