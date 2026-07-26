package com.chefkix.social.chat.repository;

import java.util.List;

import com.chefkix.social.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateAsc(String conversationId);

    List<ChatMessage> findAllByConversationIdOrderByCreatedDateAsc(String conversationId, Pageable pageable);

    Page<ChatMessage> findByConversationIdOrderByCreatedDateDesc(String conversationId, Pageable pageable);

    List<ChatMessage> findAllBySenderUserId(String userId);

    List<ChatMessage> findAllByReplyToIdIn(List<String> replyToIds);

    @Query("{ 'reactions.userIds': ?0 }")
    List<ChatMessage> findAllByReactionUserId(String userId);
}
