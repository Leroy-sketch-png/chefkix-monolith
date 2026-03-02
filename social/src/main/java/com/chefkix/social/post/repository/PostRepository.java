package com.chefkix.social.post.repository;

import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // this is to retrieve Posts after a period of since
    List<Post> findByCreatedAtAfter(Instant since);

    Page<Post> findAllByOrderByHotScoreDesc(Pageable pageable);

    Page<Post> findAll(Pageable pageable);

    Page<Post> findAllByUserId(String userId, Pageable pageable);

    long countByUserId(String userId);
}
