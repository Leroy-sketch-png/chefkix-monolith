package com.chefkix.social.group.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.BaseEvent;
import com.chefkix.shared.event.GroupJoinRequestedEvent;
import com.chefkix.shared.event.GroupMemberJoinedEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.dto.response.JoinGroupResponse;
import com.chefkix.social.group.dto.response.PendingRequestResponse;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.entity.GroupMember;
import com.chefkix.social.group.enums.MemberRole;
import com.chefkix.social.group.enums.MemberStatus;
import com.chefkix.social.group.enums.PrivacyType;
import com.chefkix.social.group.mapper.GroupMapper;
import com.chefkix.social.group.repository.GroupMemberRepository;
import com.chefkix.social.group.repository.GroupRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupService {

    final GroupRepository groupRepository;
    final GroupMemberRepository memberRepository;
    final KafkaTemplate<String, Object> kafkaTemplate;
    @Qualifier("taskExecutor")
    final Executor taskExecutor;
    final GroupMapper mapper;

    // Simulating a call to your identity-api to ensure the user isn't banned globally
    private final ProfileProvider profileProvider;

    @Transactional // CRITICAL: Requires MongoDB Replica Set to work
    public GroupResponse createGroup(GroupCreationRequest request, String currentUserId) {

        try {
            // --- 2. CREATE THE GROUP ENTITY ---
            Group group = Group.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .coverImageUrl(request.getCoverImageUrl())
                    .privacyType(PrivacyType.valueOf(request.getPrivacyType()))
                    .creatorId(currentUserId)
                    .ownerId(currentUserId)
                    .memberCount(1)
                    .createdAt(LocalDateTime.now())
                    .build();

            Group savedGroup = groupRepository.save(group);

            // --- 3. CREATE THE ADMIN MEMBERSHIP ---
            GroupMember adminMember = GroupMember.builder()
                    .groupId(savedGroup.getId())
                    .userId(currentUserId)
                    .role(MemberRole.ADMIN)
                    .status(MemberStatus.ACTIVE)
                    .joinedAt(LocalDateTime.now())
                    .build();

            memberRepository.save(adminMember);

            // --- 5. RETURN RESPONSE ---
            return mapper.toGroupResponse(savedGroup);

        } catch (DataAccessException e) {
            // Catches MongoDB connection issues or constraints
            log.error("Database error while creating group for user {}", currentUserId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public JoinGroupResponse handleJoinRequest(String groupId, String currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        log.info("Joining group: {}", groupId);

        // Idempotency: Check if they already have a record
        Optional<GroupMember> existingRecord = memberRepository.findByGroupIdAndUserId(groupId, currentUserId);
        if (existingRecord.isPresent()) {
            MemberStatus currentStatus = existingRecord.get().getStatus();
            if (currentStatus == MemberStatus.BANNED) {
                throw new AppException(ErrorCode.GROUP_BANNED);
            }
            throw new AppException(ErrorCode.GROUP_ALREADY_IN);
        }

        MemberStatus assignedStatus;
        String message;

        // Routing based on Privacy
        if (group.getPrivacyType() == PrivacyType.PUBLIC) {
            assignedStatus = MemberStatus.ACTIVE;
            message = "Successfully joined the group!";

            // Increase member count ONLY if they are fully active
            group.setMemberCount(group.getMemberCount() + 1);
            groupRepository.save(group);
        } else {
            assignedStatus = MemberStatus.PENDING;
            message = "Join request sent. Waiting for admin approval.";
        }

        // Save new membership record
        GroupMember newMember = GroupMember.builder()
                .groupId(groupId)
                .userId(currentUserId)
                .role(MemberRole.MEMBER)
                .status(assignedStatus)
                .requestedAt(LocalDateTime.now())
                .joinedAt(assignedStatus == MemberStatus.ACTIVE ? LocalDateTime.now() : null)
                .build();

        memberRepository.save(newMember);

        // TODO: Fire Kafka event here (e.g., alert admins if PENDING)
        fireGroupMembershipEventAsync(group, currentUserId, assignedStatus);

        return JoinGroupResponse.builder()
                .groupId(groupId)
                .membershipStatus(assignedStatus.name())
                .message(message)
                .build();
    }


    // --- LEAVE / CANCEL LOGIC ---
    @Transactional
    public void handleLeaveOrCancel(String groupId, String currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // Rule: The Owner cannot abandon the group.
        if (group.getOwnerId().equals(currentUserId)) {
            throw new IllegalStateException("You are the owner. You must transfer ownership before leaving.");
        }

        // If they were an ACTIVE member, we must decrease the count
        if (member.getStatus() == MemberStatus.ACTIVE) {
            // Math.max ensures we never get a negative count due to race conditions
            group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
            groupRepository.save(group);
        }
        else if (member.getStatus() == MemberStatus.BANNED) {
            throw new IllegalStateException("Action not permitted.");
        }

        // Delete the record completely so they can re-apply in the future if they want
        memberRepository.delete(member);

        // TODO: Fire Kafka event here (e.g., update analytics)
    }

    @Transactional(readOnly = true)
    public Page<PendingRequestResponse> getPendingRequests(String groupId, String currentUserId, Pageable pageable) {

        // 1. Security Check: Only the Admin/Owner can view pending requests
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // 2. Fetch the PENDING records from MongoDB using the index we created earlier
        Page<GroupMember> pendingMembers = memberRepository.findAllByGroupIdAndStatus(
                groupId,
                MemberStatus.PENDING,
                pageable
        );

        // 3. Map the raw database records into the rich DTO
        return pendingMembers.map(member -> {
            // Fetch visual data from Identity module (with fallback)
            String displayName = "Unknown User";
            String avatarUrl = null;

            try {
                BasicProfileInfo profile = profileProvider.getBasicProfile(member.getUserId());
                if (profile != null) {
                    displayName = profile.getDisplayName();
                    avatarUrl = profile.getAvatarUrl();
                }
            } catch (Exception e) {
                log.warn("Could not fetch profile for user {}", member.getUserId());
            }

            return PendingRequestResponse.builder()
                    .userId(member.getUserId())
                    .displayName(displayName)
                    .avatarUrl(avatarUrl)
                    .requestedAt(member.getRequestedAt())
                    .build();
        });
    }

    // ===============================================
    // ASYNC EVENT PUBLISHER
    // ===============================================

    private void fireGroupMembershipEventAsync(Group group, String currentUserId, MemberStatus status) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Lấy thông tin Profile với Fallback an toàn (Graceful Degradation)
                BasicProfileInfo profile = null;
                try {
                    profile = profileProvider.getBasicProfile(currentUserId);
                } catch (Exception ignored) {
                    log.warn("Could not fetch profile for user {}. Using fallback data.", currentUserId);
                }

                String displayName = profile != null ? profile.getDisplayName() : "A new user";
                String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

                // 2. Build Event đa hình dựa vào Status
                BaseEvent event;
                if (status == MemberStatus.ACTIVE) {
                    event = GroupMemberJoinedEvent.builder()
                            .groupId(group.getId())
                            .groupName(group.getName())
                            .memberId(currentUserId)
                            .memberDisplayName(displayName)
                            .memberAvatarUrl(avatarUrl)
                            .adminId(group.getOwnerId())
                            .build();
                } else {
                    event = GroupJoinRequestedEvent.builder()
                            .groupId(group.getId())
                            .groupName(group.getName())
                            .requesterId(currentUserId)
                            .requesterDisplayName(displayName)
                            .requesterAvatarUrl(avatarUrl)
                            .adminId(group.getOwnerId())
                            .build();
                }

                // 3. Bắn vào đúng topic bạn đang cấu hình
                kafkaTemplate.send("group-delivery", event);
                log.info("Successfully published {} for group {}", event.getEventType(), group.getId());

            } catch (Exception e) {
                // Lỗi Kafka chết mạng cũng không làm sập API của người dùng
                log.error("Critical error while sending group membership event", e);
            }
        }, taskExecutor);
    }
}
