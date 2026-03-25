package com.chefkix.social.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.social.chat.dto.request.ConversationRequest;
import com.chefkix.social.chat.dto.response.ConversationResponse;
import com.chefkix.social.chat.dto.response.ShareContactResponse;
import com.chefkix.social.chat.entity.Conversation;
import com.chefkix.social.chat.entity.ParticipantInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.chat.mapper.ConversationMapper;
import com.chefkix.social.chat.repository.ConversationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
public class ConversationService {
    ConversationRepository conversationRepository;
    ProfileProvider profileProvider;
    SimpMessagingTemplate messagingTemplate;

    ConversationMapper conversationMapper;

    public List<ConversationResponse> myConversations() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // Sort by modifiedDate descending so most recent conversations appear first
        Sort sort = Sort.by(Sort.Direction.DESC, "modifiedDate");
        List<Conversation> conversations = conversationRepository.findAllByParticipantIdsContains(userId, sort);

        return conversations.stream().map(this::toConversationResponse).toList();
    }

    public ConversationResponse create(ConversationRequest request) {
        // Fetch user infos
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        String targetUserId = request.getParticipantIds().getFirst();
        log.info(
                "Creating conversation for user {} with participant {}",
                userId,
                targetUserId);

        // PRIVACY: Block check — blocked users cannot create conversations with each other
        if (profileProvider.isBlocked(userId, targetUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        var userInfo = profileProvider.getBasicProfile(userId);
        log.info("User info: {}", userInfo);
        var participantInfo = profileProvider.getBasicProfile(targetUserId);

        if (userInfo == null || participantInfo == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        List<String> userIds = new ArrayList<>();
        userIds.add(userId);
        userIds.add(participantInfo.getUserId());

        var sortedIds = userIds.stream().sorted().toList();
        String userIdHash = generateParticipantHash(sortedIds);

        var conversation = conversationRepository
                .findByParticipantsHash(userIdHash)
                .orElseGet(() -> {
                    List<ParticipantInfo> participantInfos = List.of(
                            ParticipantInfo.builder()
                                    .userId(userInfo.getUserId())
                                    .username(userInfo.getUsername())
                                    .firstName(userInfo.getFirstName())
                                    .lastName(userInfo.getLastName())
                                    .avatar(userInfo.getAvatarUrl())
                                    .build(),
                            ParticipantInfo.builder()
                                    .userId(participantInfo.getUserId())
                                    .username(participantInfo.getUsername())
                                    .firstName(participantInfo.getFirstName())
                                    .lastName(participantInfo.getLastName())
                                    .avatar(participantInfo.getAvatarUrl())
                                    .build());

                    // Build conversation info
                    Conversation newConversation = Conversation.builder()
                            .type(request.getType())
                            .participantsHash(userIdHash)
                            .createdDate(Instant.now())
                            .modifiedDate(Instant.now())
                            .participants(participantInfos)
                            .build();

                    Conversation savedConversation = conversationRepository.save(newConversation);

                    // Notify all participants about the new conversation via WebSocket
                    notifyNewConversation(savedConversation);

                    return savedConversation;
                });

        return toConversationResponse(conversation);
    }

    private String generateParticipantHash(List<String> ids) {
        StringJoiner stringJoiner = new StringJoiner("_");
        ids.forEach(stringJoiner::add);

        // SHA 256

        return stringJoiner.toString();
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        ConversationResponse conversationResponse = conversationMapper.toConversationResponse(conversation);

        conversation.getParticipants().stream()
                .filter(participantInfo -> !participantInfo.getUserId().equals(currentUserId))
                .findFirst()
                .ifPresent(participantInfo -> {
                    conversationResponse.setConversationName(participantInfo.getUsername());
                    conversationResponse.setConversationAvatar(participantInfo.getAvatar());
                });

        return conversationResponse;
    }

    /**
     * Notify all participants about a new conversation via WebSocket.
     * Each participant gets notified on their personal topic.
     */
    private void notifyNewConversation(Conversation conversation) {
        for (ParticipantInfo participant : conversation.getParticipants()) {
            // Build a response tailored to this participant (showing OTHER user's info)
            ConversationResponse response = conversationMapper.toConversationResponse(conversation);

            // Find the "other" participant to set conversation name/avatar for this user
            conversation.getParticipants().stream()
                    .filter(p -> !p.getUserId().equals(participant.getUserId()))
                    .findFirst()
                    .ifPresent(other -> {
                        response.setConversationName(other.getUsername());
                        response.setConversationAvatar(other.getAvatar());
                    });

            // Send to user-specific topic
            String destination = "/topic/user/" + participant.getUserId() + "/conversations";
            messagingTemplate.convertAndSend(destination, response);
            log.info("Notified user {} about new conversation {}", participant.getUserId(), conversation.getId());
        }
    }

    public List<ShareContactResponse> getShareSuggestions(Pageable pageable) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Lấy danh sách conversation gần nhất (Pageable để giới hạn số lượng, ví dụ 10-20 người)
        List<Conversation> conversations = conversationRepository.findRecentConversations(currentUserId, pageable);

        // 2. Map sang DTO
        return conversations.stream()
                .map(conv -> {
                    // Xác định thông tin hiển thị
                    String displayName;
                    String avatar;
                    String targetUserId = null;

                    if ("GROUP".equals(conv.getType())) {
                        // Nếu là nhóm: Dùng tên nhóm và ảnh nhóm (nếu chưa có field này thì cần bổ sung vào Entity)
                        displayName = "Nhóm Chat"; // Hoặc conv.getGroupName()
                        avatar = "group-placeholder.png";
                    } else {
                        // Nếu là chat 1-1: Tìm người kia (Filter người không phải là mình)
                        ParticipantInfo otherUser = conv.getParticipants().stream()
                                .filter(p -> !p.getUserId().equals(currentUserId))
                                .findFirst()
                                .orElse(null);

                        if (otherUser != null) {
                            displayName = otherUser.getFirstName() + " "
                                    + otherUser.getLastName(); // Hoặc getUsername() tùy data
                            avatar = otherUser.getAvatar();
                            targetUserId = otherUser.getUserId();
                        } else {
                            // Trường hợp chat với chính mình (nếu có)
                            displayName = "Me";
                            avatar = "";
                        }
                    }

                    return ShareContactResponse.builder()
                            .conversationId(conv.getId())
                            .displayName(displayName)
                            .avatar(avatar)
                            .type(conv.getType())
                            .userId(targetUserId)
                            .build();
                })
                .toList();
    }
}
