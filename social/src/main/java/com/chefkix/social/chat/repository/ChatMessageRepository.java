package com.chefkix.social.chat.repository;

import java.util.List;

import com.chefkix.social.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    // Return messages in ascending order (oldest first) for natural chat display
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateAsc(String conversationId);

    // Paginated: descending for "load more older messages" pattern
    Page<ChatMessage> findByConversationIdOrderByCreatedDateDesc(String conversationId, Pageable pageable);
}
