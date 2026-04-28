package com.chefkix.social.chat.service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.chefkix.social.chat.entity.Conversation;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationLookupService {
    MongoTemplate mongoTemplate;

    public Optional<Conversation> findById(String conversationId) {
        return mongoTemplate.execute(Conversation.class, collection -> {
            var filter = buildIdFilter(conversationId);
            var document = collection.find(filter).first();

            if (document == null) {
                return Optional.<Conversation>empty();
            }

            return Optional.of(mongoTemplate.getConverter().read(Conversation.class, document));
        });
    }

    public boolean isParticipant(String conversationId, String userId) {
        return findById(conversationId)
                .map(conversation -> conversation.getParticipants() != null
                        && conversation.getParticipants().stream()
                                .anyMatch(participant -> userId.equals(participant.getUserId())))
                .orElse(false);
    }

    public void touchModifiedDate(String conversationId, Instant modifiedDate) {
        mongoTemplate.execute(Conversation.class, collection -> {
            collection.updateOne(
                    buildIdFilter(conversationId),
                    Updates.set("modifiedDate", Date.from(modifiedDate)));
            return null;
        });
    }

    private org.bson.conversions.Bson buildIdFilter(String conversationId) {
        if (ObjectId.isValid(conversationId)) {
            // Seeded conversations may still have ObjectId-backed _id values while
            // newer records are stored as strings.
            return Filters.or(
                    Filters.eq("_id", conversationId),
                    Filters.eq("_id", new ObjectId(conversationId)));
        }

        return Filters.eq("_id", conversationId);
    }
}