package com.chefkix.social.chat.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.entity.ChatMessage;
import com.chefkix.social.chat.entity.Conversation;
import com.chefkix.social.chat.entity.ParticipantInfo;
import com.chefkix.social.chat.enums.MessageType;
import com.chefkix.social.chat.mapper.ChatMessageMapper;
import com.chefkix.social.chat.repository.ChatMessageRepository;
import com.chefkix.social.chat.repository.ConversationRepository;
import com.chefkix.social.post.service.PostService;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.event.StoryReplyEvent;
import com.chefkix.shared.exception.AppException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationLookupService conversationLookupService;
    @Mock
    private ProfileProvider profileProvider;
    @Mock
    private ContentModerationProvider contentModerationProvider;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private PostService postService;

    private ChatMessageService service;

    @BeforeEach
    void setUp() {
        service = new ChatMessageService(
                chatMessageRepository,
                conversationRepository,
                conversationLookupService,
                profileProvider,
                contentModerationProvider,
                chatMessageMapper,
                postService);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user-1", null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTouchesLegacyConversationInsteadOfSavingItAgain() {
        String conversationId = new org.bson.types.ObjectId().toHexString();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId(conversationId)
                .message("legacy conversation send")
                .type(MessageType.TEXT)
                .build();

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .participants(List.of(ParticipantInfo.builder().userId("user-1").build()))
                .build();

        ChatMessage mappedMessage = ChatMessage.builder()
                .conversationId(conversationId)
                .build();

        when(contentModerationProvider.moderate("legacy conversation send", "chat"))
                .thenReturn(ContentModerationProvider.ModerationResult.approved());
        when(conversationLookupService.findById(conversationId)).thenReturn(java.util.Optional.of(conversation));
        when(profileProvider.getBasicProfile("user-1")).thenReturn(BasicProfileInfo.builder()
                .userId("user-1")
                .displayName("Test User")
                .firstName("Test")
                .lastName("User")
                .avatarUrl("/placeholder-avatar.svg")
                .build());
        when(chatMessageMapper.toChatMessage(request)).thenReturn(mappedMessage);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId("message-1");
            message.setCreatedDate(Instant.parse("2026-04-27T15:18:19Z"));
            return message;
        });

        ChatMessageResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo("message-1");
        assertThat(response.getMessage()).isEqualTo("legacy conversation send");
        verify(conversationLookupService).touchModifiedDate(eq(conversationId), any(Instant.class));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createRejectsFlaggedModerationResponseBeforeSaving() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId("conversation-1")
                .message("ambiguous cooking message")
                .type(MessageType.TEXT)
                .build();
        when(contentModerationProvider.moderate("ambiguous cooking message", "chat"))
                .thenReturn(new ContentModerationProvider.ModerationResult(
                        "flag", "off_topic", "medium", 0.5, "Manual review required"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(conversationLookupService, never()).findById("conversation-1");
    }

    @Test
    void blockedPostShareUsesSenderIdentityAndStopsBeforeChatPersistence() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId("conversation-1")
                .message("Sharing this recipe")
                .type(MessageType.POST_SHARE)
                .relatedId("blocked-post")
                .build();
        when(postService.getPostDetail("blocked-post", "user-1"))
                .thenThrow(new AppException(ErrorCode.POST_NOT_FOUND));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class);

        verify(postService).getPostDetail("blocked-post", "user-1");
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(contentModerationProvider, never()).moderate(any(), any());
    }
    @Test
    void createDoesNotLogSharedPostContentOrSignedMediaUrl() {
        String conversationId = new org.bson.types.ObjectId().toHexString();
        String recipeTitle = "private-recipe-title-72aef";
        String signedImageUrl = "https://cdn.example/private-image.jpg?token=secret-937bd";
        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId(conversationId)
                .type(MessageType.POST_SHARE)
                .relatedId("post-1")
                .build();
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .participants(List.of(ParticipantInfo.builder().userId("user-1").build()))
                .build();
        ChatMessage mappedMessage = ChatMessage.builder().conversationId(conversationId).build();

        when(postService.getPostDetail("post-1", "user-1")).thenReturn(com.chefkix.social.api.dto.PostDetail.builder()
                .photoUrls(List.of(signedImageUrl))
                .recipeTitle(recipeTitle)
                .build());
        when(contentModerationProvider.moderate("Post shared: " + recipeTitle, "chat"))
                .thenReturn(ContentModerationProvider.ModerationResult.approved());
        when(conversationLookupService.findById(conversationId))
                .thenReturn(java.util.Optional.of(conversation));
        when(profileProvider.getBasicProfile("user-1")).thenReturn(BasicProfileInfo.builder()
                .userId("user-1")
                .displayName("Test User")
                .build());
        when(chatMessageMapper.toChatMessage(request)).thenReturn(mappedMessage);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId("message-1");
            return message;
        });

        Logger logger = (Logger) LoggerFactory.getLogger(ChatMessageService.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
        try {
            ChatMessageResponse response = service.create(request);

            assertThat(response.getSharedPostTitle()).isEqualTo(recipeTitle);
            String loggedText = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(loggedText)
                    .doesNotContain(recipeTitle)
                    .doesNotContain(signedImageUrl)
                    .doesNotContain("Post shared: " + recipeTitle);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void createDoesNotLogPostIdentifierOrProviderExceptionMessage() {
        String postId = "private-post-id-3a8c1";
        String providerMessage = "private-provider-response-4b6d2";
        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId("conversation-1")
                .type(MessageType.POST_SHARE)
                .relatedId(postId)
                .build();
        when(postService.getPostDetail(postId, "user-1")).thenThrow(new IllegalStateException(providerMessage));

        Logger logger = (Logger) LoggerFactory.getLogger(ChatMessageService.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> service.create(request)).isInstanceOf(AppException.class);

            String loggedText = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(loggedText)
                    .doesNotContain(postId)
                    .doesNotContain(providerMessage);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void createReturnsTheExistingMessageForTheSameSenderClientKey() {
        String conversationId = new org.bson.types.ObjectId().toHexString();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId(conversationId)
                .message("water is boiling")
                .type(MessageType.TEXT)
                .clientMessageId("client-message-1")
                .build();
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .participants(List.of(ParticipantInfo.builder().userId("user-1").build()))
                .build();
        ChatMessage mappedMessage = ChatMessage.builder()
                .conversationId(conversationId)
                .clientMessageId("client-message-1")
                .build();

        when(chatMessageRepository.findBySenderUserIdAndClientMessageId(
                        "user-1", "client-message-1"))
                .thenReturn(java.util.Optional.empty(), java.util.Optional.of(mappedMessage));
        when(contentModerationProvider.moderate("water is boiling", "chat"))
                .thenReturn(ContentModerationProvider.ModerationResult.approved());
        when(conversationLookupService.findById(conversationId))
                .thenReturn(java.util.Optional.of(conversation));
        when(profileProvider.getBasicProfile("user-1")).thenReturn(BasicProfileInfo.builder()
                .userId("user-1")
                .displayName("Test User")
                .firstName("Test")
                .lastName("User")
                .avatarUrl("/placeholder-avatar.svg")
                .build());
        when(chatMessageMapper.toChatMessage(request)).thenReturn(mappedMessage);
        when(chatMessageRepository.save(mappedMessage)).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId("message-1");
            return message;
        });

        ChatMessageResponse first = service.create(request);
        ChatMessageResponse retry = service.create(request);

        assertThat(first.getId()).isEqualTo("message-1");
        assertThat(retry.getId()).isEqualTo("message-1");
        assertThat(retry.getClientMessageId()).isEqualTo("client-message-1");
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(contentModerationProvider, times(1)).moderate("water is boiling", "chat");
    }

    @Test
    void storyReplyPersistsOwnerContextWithoutBackendDisplayCopy() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .participantsHash("owner-1_replier-1")
                .build();
        StoryReplyEvent event = StoryReplyEvent.builder()
                .storyId("story-1")
                .storyOwnerId("owner-1")
                .replierId("replier-1")
                .replyText("That looks delicious")
                .storyMediaUrl("https://cdn.example/story-1.jpg")
                .build();

        when(conversationRepository.findByParticipantsHash("owner-1_replier-1"))
                .thenReturn(java.util.Optional.of(conversation));
        when(contentModerationProvider.moderate("That looks delicious", "chat"))
                .thenReturn(ContentModerationProvider.ModerationResult.approved());
        when(profileProvider.getBasicProfile("replier-1")).thenReturn(BasicProfileInfo.builder()
                .userId("replier-1")
                .displayName("Replying Cook")
                .build());
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.processStoryReplyEvent(event);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage saved = messageCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(MessageType.STORY_REPLY);
        assertThat(saved.getRelatedId()).isEqualTo("story-1");
        assertThat(saved.getStoryOwnerId()).isEqualTo("owner-1");
        assertThat(saved.getSharedPostImage()).isEqualTo("https://cdn.example/story-1.jpg");
        assertThat(saved.getSharedPostTitle()).isNull();
        verify(conversationLookupService).touchModifiedDate(eq("conversation-1"), any(Instant.class));
    }

    @Test
    void flaggedStoryReplyIsNotPublished() {
        StoryReplyEvent event = StoryReplyEvent.builder()
                .storyId("story-1")
                .storyOwnerId("owner-1")
                .replierId("replier-1")
                .replyText("ambiguous story reply")
                .build();
        when(conversationRepository.findByParticipantsHash("owner-1_replier-1"))
                .thenReturn(java.util.Optional.of(Conversation.builder()
                        .id("conversation-1")
                        .participantsHash("owner-1_replier-1")
                        .build()));
        when(contentModerationProvider.moderate("ambiguous story reply", "chat"))
                .thenReturn(new ContentModerationProvider.ModerationResult(
                        "flag", "off_topic", "medium", 0.5, "Manual review required"));

        service.processStoryReplyEvent(event);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(profileProvider, never()).getBasicProfile("replier-1");
    }

    @Test
    void storyReplyResponseReturnsPersistedOwnerContext() {
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .participants(List.of(ParticipantInfo.builder().userId("user-1").build()))
                .build();
        ChatMessage stored = ChatMessage.builder()
                .id("message-1")
                .conversationId("conversation-1")
                .message("That looks delicious")
                .type(MessageType.STORY_REPLY)
                .relatedId("story-1")
                .storyOwnerId("owner-1")
                .sharedPostImage("https://cdn.example/story-1.jpg")
                .sender(ParticipantInfo.builder().userId("replier-1").build())
                .createdDate(Instant.parse("2026-07-23T10:00:00Z"))
                .build();

        when(conversationLookupService.findById("conversation-1"))
                .thenReturn(java.util.Optional.of(conversation));
        when(chatMessageRepository.findAllByConversationIdOrderByCreatedDateAsc(
                eq("conversation-1"), any()))
                .thenReturn(List.of(stored));

        ChatMessageResponse response = service.getMessages("conversation-1").get(0);

        assertThat(response.getStoryOwnerId()).isEqualTo("owner-1");
        assertThat(response.getRelatedId()).isEqualTo("story-1");
        assertThat(response.getSharedPostTitle()).isNull();
    }
}
