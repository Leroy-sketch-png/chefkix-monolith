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
import org.springframework.transaction.annotation.Transactional;

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
    ConversationLookupService conversationLookupService;
    ProfileProvider profileProvider;
    ContentModerationProvider contentModerationProvider;

    ChatMessageMapper chatMessageMapper;
    PostService postService;

    /**
     */
    public List<ChatMessageResponse> getMessages(String conversationId) {
        validateConversationAccess(conversationId);
        var messages = chatMessageRepository.findAllByConversationIdOrderByCreatedDateAsc(
                conversationId, PageRequest.of(0, 500));
        return messages.stream().map(this::toChatMessageResponse).toList();
    }

    /**
     */
    public Page<ChatMessageResponse> getMessagesPaginated(String conversationId, int page, int size) {
        validateConversationAccess(conversationId);
        Page<ChatMessage> messagesPage = chatMessageRepository.findByConversationIdOrderByCreatedDateDesc(
                conversationId, PageRequest.of(page, size));
        return messagesPage.map(this::toChatMessageResponse);
    }

    /**
     */
    private void validateConversationAccess(String conversationId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String userId = authentication.getName();
        conversationLookupService
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND))
                .getParticipants()
                .stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    @Transactional
    public ChatMessageResponse create(ChatMessageRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String userId = authentication.getName();

        MessageType msgType = request.getType() != null ? request.getType() : MessageType.TEXT;
        String finalMessageContent = request.getMessage();

        String cachedPostImage = null;
        String cachedPostTitle = null;

        if (msgType == MessageType.POST_SHARE) {
            if (request.getRelatedId() == null || request.getRelatedId().trim().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            try {
                PostDetail postInfo = postService.getPostDetail(request.getRelatedId());

                if (postInfo.getPhotoUrls() != null && !postInfo.getPhotoUrls().isEmpty()) {
                    cachedPostImage = postInfo.getPhotoUrls().get(0);
                    log.debug("Cached post image: {}", cachedPostImage);
                }

                if (postInfo.getRecipeTitle() != null
                        && !postInfo.getRecipeTitle().trim().isEmpty()) {
                    cachedPostTitle = postInfo.getRecipeTitle();
                    log.debug("Cached recipe title: {}", cachedPostTitle);
                }

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
            if (finalMessageContent == null || finalMessageContent.trim().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_MESSAGE);
            }
        }

        if (finalMessageContent != null && !finalMessageContent.isBlank()) {
            var moderationResult = contentModerationProvider.moderate(finalMessageContent, "chat");
            if (moderationResult.isBlocked()) {
                log.warn("Chat message blocked by AI moderation for user {}: {}", userId, moderationResult.reason());
                throw new AppException(ErrorCode.CONTENT_MODERATION_FAILED);
            }
        }

    var conversation = conversationLookupService
                .findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.getParticipants().stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        BasicProfileInfo userInfo = profileProvider.getBasicProfile(userId);
        if (userInfo == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        ChatMessage chatMessage = chatMessageMapper.toChatMessage(request);

        chatMessage.setSender(ParticipantInfo.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getDisplayName())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .avatar(userInfo.getAvatarUrl())
                .build());

        chatMessage.setMessage(finalMessageContent);
        chatMessage.setType(msgType);
        chatMessage.setRelatedId(request.getRelatedId());

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

        if (msgType == MessageType.POST_SHARE) {
            chatMessage.setSharedPostImage(cachedPostImage);
            chatMessage.setSharedPostTitle(cachedPostTitle);
            log.info(
                    "Created POST_SHARE message with cached data - image: {}, title: {}",
                    cachedPostImage != null,
                    cachedPostTitle != null);
        }

        chatMessage.setCreatedDate(Instant.now());

        chatMessage = chatMessageRepository.save(chatMessage);

        conversationLookupService.touchModifiedDate(request.getConversationId(), Instant.now());

        log.info("Message created successfully - type: {}, id: {}", msgType, chatMessage.getId());
        return toChatMessageResponse(chatMessage);
    }

    /**
     */
    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        String displayMessage = Boolean.TRUE.equals(message.getDeleted()) 
            ? "This message was deleted" : message.getMessage();

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
                .storyOwnerId(message.getStoryOwnerId())
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


    public void processStoryReplyEvent(com.chefkix.shared.event.StoryReplyEvent event) {
        Conversation conversation = getOrCreateDirectConversation(event.getReplierId(), event.getStoryOwnerId());

        if (event.getReplyText() != null && !event.getReplyText().isBlank()) {
            var moderationResult = contentModerationProvider.moderate(event.getReplyText(), "chat");
            if (moderationResult.isBlocked()) {
                log.warn("Story reply blocked by AI moderation for user {}: {}", event.getReplierId(), moderationResult.reason());
                return;
            }
        }

        BasicProfileInfo senderInfo = profileProvider.getBasicProfile(event.getReplierId());
        if (senderInfo == null) {
            log.error("Could not fetch profile for user {}", event.getReplierId());
            return;
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .conversationId(conversation.getId())
                .message(event.getReplyText())
                .type(MessageType.STORY_REPLY)
                .relatedId(event.getStoryId())
                .storyOwnerId(event.getStoryOwnerId())
                .sharedPostImage(event.getStoryMediaUrl())
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

        chatMessage = chatMessageRepository.save(chatMessage);

        conversationLookupService.touchModifiedDate(conversation.getId(), Instant.now());

        log.info("Created STORY_REPLY message id {} in conversation {}", chatMessage.getId(), conversation.getId());
    }

    /**
     * Find or create a direct conversation between two users.
     * Uses a deterministic hash so A-B and B-A map to the same room.
     */
    private Conversation getOrCreateDirectConversation(String user1, String user2) {
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


    /**
     */
    public ChatMessageResponse reactToMessage(String messageId, ChatReactionRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        validateConversationAccess(message.getConversationId());

        if (message.getReactions() == null) {
            message.setReactions(new ArrayList<>());
        }

        String emoji = request.getEmoji();

        ChatMessage.Reaction existingReaction = message.getReactions().stream()
                .filter(r -> emoji.equals(r.getEmoji()))
                .findFirst()
                .orElse(null);

        if (existingReaction != null) {
            if (existingReaction.getUserIds().contains(userId)) {
                existingReaction.getUserIds().remove(userId);
                if (existingReaction.getUserIds().isEmpty()) {
                    message.getReactions().remove(existingReaction);
                }
            } else {
                existingReaction.getUserIds().add(userId);
            }
        } else {
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


    /**
     */
    public ChatMessageResponse deleteMessage(String messageId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getSender().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        message.setDeleted(true);
message.setMessage(null);
message.setReactions(new ArrayList<>());

        chatMessageRepository.save(message);
        log.info("User {} deleted message {}", userId, messageId);
        return toChatMessageResponse(message);
    }
}
