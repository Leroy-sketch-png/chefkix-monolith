package com.chefkix.social.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.request.ChatReactionRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.entity.ChatMessage;
import com.chefkix.social.chat.entity.Conversation;
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
    ContentModerationProvider contentModerationProvider;

    ChatMessageMapper chatMessageMapper;
    PostService postService;

    /**
     * Get messages for a conversation (non-paginated, for backwards compatibility).
     * Returns messages in ascending order (oldest first) for natural chat display.
     */
    public List<ChatMessageResponse> getMessages(String conversationId) {
        validateConversationAccess(conversationId);
        var messages = chatMessageRepository.findAllByConversationIdOrderByCreatedDateAsc(
                conversationId, PageRequest.of(0, 500));
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String userId = authentication.getName();
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String userId = authentication.getName();

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

        // AI CONTENT MODERATION — applies to ALL message types with user-supplied text
        if (finalMessageContent != null && !finalMessageContent.isBlank()) {
            var moderationResult = contentModerationProvider.moderate(finalMessageContent, "chat");
            if (moderationResult.isBlocked()) {
                log.warn("Chat message blocked by AI moderation for user {}: {}", userId, moderationResult.reason());
                throw new AppException(ErrorCode.CONTENT_MODERATION_FAILED);
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

        // Handle reply-to: cache replied message context for display
        if (request.getReplyToId() != null && !request.getReplyToId().isBlank()) {
            final ChatMessage msgRef = chatMessage;
            chatMessageRepository.findById(request.getReplyToId()).ifPresent(repliedMsg -> {
                msgRef.setReplyToId(repliedMsg.getId());
                String content = repliedMsg.getMessage();
                msgRef.setReplyToContent(content != null && content.length() > 100 
                    ? content.substring(0, 100) + "..." : content);
                msgRef.setReplyToSenderName(
                    repliedMsg.getSender() != null ? repliedMsg.getSender().getUsername() : "Unknown");
            });
        }

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

        // Handle soft-deleted messages: clear content but preserve structure
        String displayMessage = Boolean.TRUE.equals(message.getDeleted()) 
            ? "This message was deleted" : message.getMessage();

        // Map reactions with current user context
        List<ChatMessageResponse.ReactionInfo> reactionInfos = new ArrayList<>();
        if (message.getReactions() != null) {
            for (ChatMessage.Reaction reaction : message.getReactions()) {
                reactionInfos.add(ChatMessageResponse.ReactionInfo.builder()
                    .emoji(reaction.getEmoji())
                    .count(reaction.getUserIds() != null ? reaction.getUserIds().size() : 0)
                    .userReacted(reaction.getUserIds() != null && reaction.getUserIds().contains(currentUserId))
                    .build());
            }
        }

        // Map reply-to context
        ChatMessageResponse.ReplyInfo replyInfo = null;
        if (message.getReplyToId() != null) {
            replyInfo = ChatMessageResponse.ReplyInfo.builder()
                .messageId(message.getReplyToId())
                .content(message.getReplyToContent())
                .senderName(message.getReplyToSenderName())
                .build();
        }

        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .me(message.getSender().getUserId().equals(currentUserId))
                .message(displayMessage)
                .sender(message.getSender())
                .createdDate(message.getCreatedDate())
                .type(message.getType())
                .relatedId(message.getRelatedId())
                .sharedPostImage(message.getSharedPostImage())
                .sharedPostTitle(message.getSharedPostTitle())
                .replyTo(replyInfo)
                .reactions(reactionInfos)
                .deleted(message.getDeleted())
                .build();

        if (message.getType() == MessageType.POST_SHARE) {
            log.debug(
                    "Built POST_SHARE response - image: {}, title: {}",
                    response.getSharedPostImage() != null,
                    response.getSharedPostTitle() != null);
        }

        return response;
    }

    // ======================== STORY REPLY (KAFKA EVENT) ========================

    /**
     * Xử lý Event từ Kafka khi có người Reply Story.
     * Lưu ý: Hàm này chạy ở Background Worker (Thread của Kafka Listener),
     * KHÔNG có HTTP Request, do đó KHÔNG dùng SecurityContextHolder ở đây.
     */
    public void processStoryReplyEvent(com.chefkix.shared.event.StoryReplyEvent event) {
        // 1. Tìm hoặc tạo mới cuộc hội thoại 1-1 giữa người xem và chủ nhân Story
        Conversation conversation = getOrCreateDirectConversation(event.getReplierId(), event.getStoryOwnerId());

        // 2. AI Content Moderation
        if (event.getReplyText() != null && !event.getReplyText().isBlank()) {
            var moderationResult = contentModerationProvider.moderate(event.getReplyText(), "chat");
            if (moderationResult.isBlocked()) {
                log.warn("Story reply blocked by AI moderation for user {}: {}", event.getReplierId(), moderationResult.reason());
                return; // Dừng lại, không tạo tin nhắn độc hại
            }
        }

        // 3. Lấy thông tin Profile người gửi
        BasicProfileInfo senderInfo = profileProvider.getBasicProfile(event.getReplierId());
        if (senderInfo == null) {
            log.error("Could not fetch profile for user {}", event.getReplierId());
            return;
        }

        // 4. Khởi tạo ChatMessage (Tái sử dụng các trường của POST_SHARE)
        ChatMessage chatMessage = ChatMessage.builder()
                .conversationId(conversation.getId())
                .message(event.getReplyText())
                .type(MessageType.STORY_REPLY) // Yêu cầu: Đã thêm STORY_REPLY vào enum MessageType
                .relatedId(event.getStoryId())
                .sharedPostImage(event.getStoryMediaUrl()) // Lấy ảnh Thumbnail của Story
                .sharedPostTitle("Đã trả lời tin của bạn") // Text hiển thị trên Thumbnail
                .sender(ParticipantInfo.builder()
                        .userId(senderInfo.getUserId())
                        .username(senderInfo.getDisplayName())
                        .firstName(senderInfo.getFirstName())
                        .lastName(senderInfo.getLastName())
                        .avatar(senderInfo.getAvatarUrl())
                        .build())
                .createdDate(Instant.now())
                .deleted(false)
                .reactions(new ArrayList<>())
                .build();

        // 5. Lưu Message vào DB
        chatMessage = chatMessageRepository.save(chatMessage);

        // 6. Cập nhật thời gian hoạt động của phòng chat
        conversation.setModifiedDate(Instant.now());
        conversationRepository.save(conversation);

        log.info("Created STORY_REPLY message id {} in conversation {}", chatMessage.getId(), conversation.getId());

        // Ghi chú: Nếu hệ thống của bạn có WebSocket, bạn có thể gọi messagingTemplate.convertAndSendToUser() ở đây
        // để báo realtime cho người nhận. Bạn không gọi toChatMessageResponse() ở đây vì hàm đó đang gắn cứng
        // SecurityContextHolder (sẽ bị lỗi NullPointerException vì đây là luồng background).
    }

    /**
     * Hàm tiện ích: Tìm phòng chat 1-1, nếu chưa có thì tạo mới
     */
    private Conversation getOrCreateDirectConversation(String user1, String user2) {
        // Thuật toán băm: Sắp xếp theo thứ tự từ điển để (A chat với B) hay (B chat với A) đều ra chung 1 ID phòng
        String hash = user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;

        return conversationRepository.findByParticipantsHash(hash)
                .orElseGet(() -> {
                    BasicProfileInfo p1 = profileProvider.getBasicProfile(user1);
                    BasicProfileInfo p2 = profileProvider.getBasicProfile(user2);

                    Conversation newConv = Conversation.builder()
                            .type("DIRECT")
                            .participantsHash(hash)
                            .participants(List.of(
                                    ParticipantInfo.builder()
                                            .userId(user1)
                                            .username(p1 != null ? p1.getDisplayName() : "User")
                                            .avatar(p1 != null ? p1.getAvatarUrl() : null)
                                            .build(),
                                    ParticipantInfo.builder()
                                            .userId(user2)
                                            .username(p2 != null ? p2.getDisplayName() : "User")
                                            .avatar(p2 != null ? p2.getAvatarUrl() : null)
                                            .build()
                            ))
                            .createdDate(Instant.now())
                            .modifiedDate(Instant.now())
                            .build();
                    return conversationRepository.save(newConv);
                });
    }

    // ======================== Reactions ========================

    /**
     * Toggle a reaction on a message. If the user already reacted with this emoji,
     * remove it. Otherwise, add it. Returns the updated message.
     */
    public ChatMessageResponse reactToMessage(String messageId, ChatReactionRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // Verify user has access to this conversation
        validateConversationAccess(message.getConversationId());

        if (message.getReactions() == null) {
            message.setReactions(new ArrayList<>());
        }

        String emoji = request.getEmoji();

        // Find existing reaction for this emoji
        ChatMessage.Reaction existingReaction = message.getReactions().stream()
                .filter(r -> emoji.equals(r.getEmoji()))
                .findFirst()
                .orElse(null);

        if (existingReaction != null) {
            if (existingReaction.getUserIds().contains(userId)) {
                // User already reacted — toggle off
                existingReaction.getUserIds().remove(userId);
                if (existingReaction.getUserIds().isEmpty()) {
                    message.getReactions().remove(existingReaction);
                }
            } else {
                // Add user to existing emoji
                existingReaction.getUserIds().add(userId);
            }
        } else {
            // New emoji reaction
            List<String> userIds = new ArrayList<>();
            userIds.add(userId);
            message.getReactions().add(ChatMessage.Reaction.builder()
                    .emoji(emoji)
                    .userIds(userIds)
                    .build());
        }

        chatMessageRepository.save(message);
        log.info("User {} toggled reaction {} on message {}", userId, emoji, messageId);
        return toChatMessageResponse(message);
    }

    // ======================== Delete ========================

    /**
     * Soft-delete a message. Only the sender can delete their own messages.
     * Content is cleared but metadata (timestamp, sender) is preserved for thread context.
     */
    public ChatMessageResponse deleteMessage(String messageId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // Only sender can delete their own message
        if (!message.getSender().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        message.setDeleted(true);
        message.setMessage(null); // Clear actual content
        message.setReactions(new ArrayList<>()); // Clear reactions on deleted message

        chatMessageRepository.save(message);
        log.info("User {} deleted message {}", userId, messageId);
        return toChatMessageResponse(message);
    }
}
