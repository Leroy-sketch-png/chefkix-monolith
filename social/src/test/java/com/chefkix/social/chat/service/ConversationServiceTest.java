package com.chefkix.social.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.social.chat.dto.response.ConversationResponse;
import com.chefkix.social.chat.entity.Conversation;
import com.chefkix.social.chat.entity.ParticipantInfo;
import com.chefkix.social.chat.mapper.ConversationMapper;
import com.chefkix.social.chat.repository.ConversationRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ProfileProvider profileProvider;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ConversationMapper conversationMapper;

    private ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(
                conversationRepository,
                profileProvider,
                messagingTemplate,
                conversationMapper);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user-1", null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myConversationsBuildsReadableGroupNames() {
        Conversation groupConversation = Conversation.builder()
                .id("group-1")
                .type("GROUP")
                .participants(List.of(
                        participant("user-1", "testuser", "Test", "User"),
                        participant("user-2", "weekend_cook", "Sarah", "Chen"),
                        participant("user-3", "fitness_fuel", "Marcus", "Johnson"),
                        participant("user-4", "vegan_vibes", "Luna", "Martinez"),
                        participant("user-5", "student_eats", "Alex", "Park")))
                .createdDate(Instant.parse("2026-04-28T02:00:00Z"))
                .modifiedDate(Instant.parse("2026-04-28T02:05:00Z"))
                .build();

        when(conversationRepository.findRecentConversations(eq("user-1"), any(Pageable.class)))
                .thenReturn(List.of(groupConversation));
        when(conversationMapper.toConversationResponse(groupConversation))
                .thenReturn(ConversationResponse.builder()
                        .id(groupConversation.getId())
                        .type(groupConversation.getType())
                        .participants(groupConversation.getParticipants())
                        .createdDate(groupConversation.getCreatedDate())
                        .modifiedDate(groupConversation.getModifiedDate())
                        .build());

        List<ConversationResponse> responses = service.myConversations();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getConversationName()).isEqualTo("Sarah, Marcus +2");
        assertThat(responses.getFirst().getConversationAvatar())
                .isEqualTo(ConversationService.GROUP_CONVERSATION_AVATAR);
    }

    private ParticipantInfo participant(
            String userId,
            String username,
            String firstName,
            String lastName) {
        return ParticipantInfo.builder()
                .userId(userId)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .avatar("/placeholder-avatar.svg")
                .build();
    }
}