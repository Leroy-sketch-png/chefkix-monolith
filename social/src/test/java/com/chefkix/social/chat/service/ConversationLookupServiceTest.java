package com.chefkix.social.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.chefkix.social.chat.entity.Conversation;
import java.time.Instant;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

@ExtendWith(MockitoExtension.class)
class ConversationLookupServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private MongoCollection<Document> collection;
    @Mock
    private FindIterable<Document> findIterable;
    @Mock
    private MongoConverter mongoConverter;

    private ConversationLookupService service;

    @BeforeEach
    void setUp() {
        service = new ConversationLookupService(mongoTemplate);
    }

    @Test
    void findByIdSupportsLegacyObjectIdConversationKeys() {
        String conversationId = new ObjectId().toHexString();
        Document storedDocument = new Document("_id", new ObjectId(conversationId));
        Conversation conversation = Conversation.builder().id(conversationId).build();

        when(mongoTemplate.getConverter()).thenReturn(mongoConverter);
        when(mongoConverter.read(eq(Conversation.class), eq(storedDocument))).thenReturn(conversation);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(storedDocument);
        when(mongoTemplate.execute(eq(Conversation.class), any())).thenAnswer(invocation -> {
            CollectionCallback<?> callback = invocation.getArgument(1);
            return callback.doInCollection(collection);
        });

        Optional<Conversation> result = service.findById(conversationId);

        assertThat(result).contains(conversation);

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(collection).find(filterCaptor.capture());
        String filterJson = filterCaptor.getValue()
                .toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry())
                .toJson();
        assertThat(filterJson).contains("$or");
        assertThat(filterJson).contains(conversationId);
        assertThat(filterJson).contains("$oid");
    }

    @Test
    void touchModifiedDateUsesLegacyCompatibleIdFilter() {
        String conversationId = new ObjectId().toHexString();
        Instant modifiedDate = Instant.parse("2026-04-27T15:18:19Z");

        when(mongoTemplate.execute(eq(Conversation.class), any())).thenAnswer(invocation -> {
            CollectionCallback<?> callback = invocation.getArgument(1);
            return callback.doInCollection(collection);
        });

        service.touchModifiedDate(conversationId, modifiedDate);

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(collection).updateOne(filterCaptor.capture(), updateCaptor.capture());

        String filterJson = filterCaptor.getValue()
                .toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry())
                .toJson();
        String updateJson = updateCaptor.getValue()
                .toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry())
                .toJson();

        assertThat(filterJson).contains("$or");
        assertThat(filterJson).contains(conversationId);
        assertThat(updateJson).contains("$set");
        assertThat(updateJson).contains("modifiedDate");
    }
}