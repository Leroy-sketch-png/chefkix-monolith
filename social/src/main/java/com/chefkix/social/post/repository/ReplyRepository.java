package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.Reply;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReplyRepository extends MongoRepository<Reply, String> {
    List<Reply> findByParentCommentId(String commentId);

    List<Reply> findByParentCommentId(String commentId, Pageable pageable);

    List<Reply> findByParentCommentIdIn(Collection<String> commentIds);

    void deleteAllByParentCommentId(String commentId);

    void deleteAllByParentCommentIdIn(Collection<String> commentIds);
}
