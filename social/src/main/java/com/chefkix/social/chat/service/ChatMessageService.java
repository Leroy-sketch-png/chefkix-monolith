package com.chefkix.social.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.entity.ChatMessage;
import com.chefkix.social.chat.entity.ParticipantInfo;
import com.chefkix.social.chat.enums.MessageType;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.chat.mapper.ChatMessageMapper;
import com.chefkix.social.chat.repository.ChatMessageRepository;
import com.chefkix.social.chat.repository.ConversationRepository;
import com.chefkix.social.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {
    ChatMessageRepository chatMessageRepository;
    ConversationRepository conversationRepository;
    ProfileProvider profileProvider;

    ChatMessageMapper chatMessageMapper;
    PostService postService;

    /**
     * Get messages for a conversation (non-paginated, for backwards compatibility).
     * Returns messages in ascending order (oldest first) for natural chat display.
     */
    public List<ChatMessageResponse> getMessages(String conversationId) {
        validateConversationAccess(conversationId);
        var messages = chatMessageRepository.findAllByConversationIdOrderByCreatedDateAsc(conversationId);
        return messages.stream().map(this::toChatMessageResponse).toList();
    }

    /**
     * Get paginated messages for a conversation.
     * @param conversationId the conversation ID
     * @param page page number (0-indexed)
     * @param size page size (default 50)
     */
    public Page<ChatMessageResponse> getMessagesPaginated(String conversationId, int page, int size) {
        validateConversationAccess(conversationId);
        Page<ChatMessage> messagesPage = chatMessageRepository.findByConversationIdOrderByCreatedDateDesc(
                conversationId, PageRequest.of(page, size));
        return messagesPage.map(this::toChatMessageResponse);
    }

    /**
     * Validates that the current user is a participant in the conversation.
     */
    private void validateConversationAccess(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND))
                .getParticipants()
                .stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    public ChatMessageResponse create(ChatMessageRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Determine message type
        MessageType msgType = request.getType() != null ? request.getType() : MessageType.TEXT;
        String finalMessageContent = request.getMessage();

        // NEW: Variables to cache post snapshot data
        String cachedPostImage = null;
        String cachedPostTitle = null;

        // 2. LOGIC VALIDATION & SNAPSHOT
        if (msgType == MessageType.POST_SHARE) {
            // --- Share post ---
            if (request.getRelatedId() == null || request.getRelatedId().trim().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            try {
                PostDetail postInfo = postService.getPostDetail(request.getRelatedId());

                // Cache post thumbnail (first image from photoUrls array)
                if (postInfo.getPhotoUrls() != null && !postInfo.getPhotoUrls().isEmpty()) {
                    cachedPostImage = postInfo.getPhotoUrls().get(0);
                    log.debug("Cached post image: {}", cachedPostImage);
                }

                // Cache recipe title (prioritized for cooking posts)
                if (postInfo.getRecipeTitle() != null
                        && !postInfo.getRecipeTitle().trim().isEmpty()) {
                    cachedPostTitle = postInfo.getRecipeTitle();
                    log.debug("Cached recipe title: {}", cachedPostTitle);
                }

                // Auto-fill Caption: if user doesn't write message, auto-fill
                if (finalMessageContent == null || finalMessageContent.trim().isEmpty()) {
                    if (postInfo.getRecipeTitle() != null
                            && !postInfo.getRecipeTitle().isEmpty()) {
                        finalMessageContent = "Post shared: " + postInfo.getRecipeTitle();
                    } else {
                        String snippet = (postInfo.getContent() != null
                                        && postInfo.getContent().length() > 40)
                                ? postInfo.getContent().substring(0, 40) + "..."
                                : postInfo.getContent();
                        finalMessageContent =
                                (snippet != null && !snippet.isEmpty()) ? "Shared: " + snippet : "Post shared.";
                    }
                    log.debug("Auto-filled message content: {}", finalMessageContent);
                }
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to verify post {}: {}", request.getRelatedId(), e.getMessage());
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
        } else {
            // --- Text message ---
            // If message type is TEXT, the content must not be NULL
            if (finalMessageContent == null || finalMessageContent.trim().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_MESSAGE);
            }
        }

        // 3. VALIDATE CONVERSATION & PARTICIPANTS
        var conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.getParticipants().stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        // 4. GET USER INFO
        BasicProfileInfo userInfo = profileProvider.getBasicProfile(userId);
        if (userInfo == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // 5. BUILD CHAT MESSAGE
        ChatMessage chatMessage = chatMessageMapper.toChatMessage(request);

        chatMessage.setSender(ParticipantInfo.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getDisplayName())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .avatar(userInfo.getAvatarUrl())
                .build());

        // SET VALIDATED FIELDS
        chatMessage.setMessage(finalMessageContent);
        chatMessage.setType(msgType);
        chatMessage.setRelatedId(request.getRelatedId());

        // NEW: Set cached post snapshot for POST_SHARE
        if (msgType == MessageType.POST_SHARE) {
            chatMessage.setSharedPostImage(cachedPostImage);
            chatMessage.setSharedPostTitle(cachedPostTitle);
            log.info(
                    "Created POST_SHARE message with cached data - image: {}, title: {}",
                    cachedPostImage != null,
                    cachedPostTitle != null);
        }

        chatMessage.setCreatedDate(Instant.now());

        // 6. SAVE DB
        chatMessage = chatMessageRepository.save(chatMessage);

        // Update conversation modified date
        conversation.setModifiedDate(Instant.now());
        conversationRepository.save(conversation);

        log.info("Message created successfully - type: {}, id: {}", msgType, chatMessage.getId());
        return toChatMessageResponse(chatMessage);
    }

    /**
     * Convert ChatMessage entity to ChatMessageResponse DTO
     * Maps 'me' field based on current authenticated user
     */
    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .me(message.getSender().getUserId().equals(currentUserId))
                .message(message.getMessage())
                .sender(message.getSender())
                .createdDate(message.getCreatedDate())
                .type(message.getType()) // ✅ Must be populated
                .relatedId(message.getRelatedId()) // ✅ Must be populated
                .sharedPostImage(message.getSharedPostImage()) // ✅ NEW
                .sharedPostTitle(message.getSharedPostTitle()) // ✅ NEW
                .build();

        // Debug log to verify data
        if (message.getType() == MessageType.POST_SHARE) {
            log.debug(
                    "Built POST_SHARE response - image: {}, title: {}",
                    response.getSharedPostImage() != null,
                    response.getSharedPostTitle() != null);
        }

        return response;
    }
}
