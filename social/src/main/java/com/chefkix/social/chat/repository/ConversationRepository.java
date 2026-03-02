package com.chefkix.social.chat.repository;

import java.util.List;
import java.util.Optional;

import com.chefkix.social.chat.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByParticipantsHash(String hash);

    // Sort by modifiedDate descending (most recent conversations first)
    @Query("{'participants.userId' : ?0}")
    List<Conversation> findAllByParticipantIdsContains(String userId, org.springframework.data.domain.Sort sort);

    @Query(value = "{ 'participants.userId': ?0 }", sort = "{ 'modifiedDate': -1 }")
    List<Conversation> findRecentConversations(String currentUserId, Pageable pageable);
}
