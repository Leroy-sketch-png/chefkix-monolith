package com.chefkix.social.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}