package com.chefkix.social.post.repository;

import com.chefkix.social.post.entity.Reply;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplyRepository extends MongoRepository<Reply, String> {
    List<Reply> findByParentCommentId(String commentId);

    void deleteAllByParentCommentId(String commentId);
}
