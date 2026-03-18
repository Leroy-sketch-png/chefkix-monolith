package com.chefkix.social.group.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.chat.enums.RequestAction;
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
import com.chefkix.social.group.publisher.GroupEventPublisher;
import com.chefkix.social.group.repository.GroupMemberRepository;
import com.chefkix.social.group.repository.GroupRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupService {

    final GroupRepository groupRepository;
    final GroupMemberRepository memberRepository;
    final GroupMapper mapper;
    final GroupEventPublisher eventPublisher;

    // Simulating a call to your identity-api to ensure the user isn't banned globally
    final ProfileProvider profileProvider;

    @Transactional
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
            GroupResponse response = mapper.toGroupResponse(savedGroup);
            response.setMyRole(MemberRole.ADMIN.toString());
            response.setMyStatus(MemberStatus.ACTIVE.toString());

            return response;
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
        eventPublisher.publishMembershipEvent(group, currentUserId, assignedStatus);

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
        } else if (member.getStatus() == MemberStatus.BANNED) {
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

    @Transactional
    public void processJoinRequest(String groupId, String targetUserId, RequestAction action) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Fetch Group & Verify Admin Permission
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // 2. Fetch the specific pending request
        GroupMember pendingMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

        // 3. Prevent processing if the user is already ACTIVE or BANNED
        if (pendingMember.getStatus() != MemberStatus.PENDING) {
            throw new AppException(ErrorCode.PENDING_NOT_FOUND);
        }

        // 4. Process the action
        if (action == RequestAction.ACCEPT) {
            // Update the member to ACTIVE
            pendingMember.setStatus(MemberStatus.ACTIVE);
            pendingMember.setJoinedAt(LocalDateTime.now());
            memberRepository.save(pendingMember);

            // Increment the group's active member count
            group.setMemberCount(group.getMemberCount() + 1);
            groupRepository.save(group);

            // KAFKA EVENT: Notify the target user they were accepted!
            eventPublisher.publishRequestApprovedEvent(group, currentUserId, pendingMember.getId());

        } else if (action == RequestAction.REJECT) {
            // Simply delete the record so they can try again in the future if desired
            memberRepository.delete(pendingMember);
        }
    }

    @Transactional
    public void kickMember(String groupId, String targetUserId, String currentUserId) {

        // 1. Fetch Group & Verify Admin Permission
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // 2. Prevent the Owner from kicking themselves
        if (group.getOwnerId().equals(targetUserId)) {
            throw new IllegalStateException("The group owner cannot be kicked.");
        }

        // 3. Fetch the target member
        GroupMember targetMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // 4. Decrease member count ONLY if they were an ACTIVE member
        if (targetMember.getStatus() == MemberStatus.ACTIVE) {
            group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
            groupRepository.save(group);
        }

        // 5. Hard Delete: Remove the record from MongoDB
        memberRepository.delete(targetMember);

        log.info("Admin {} kicked user {} from group {}", currentUserId, targetUserId, groupId);
    }

    @Transactional
    public void transferOwnership(String groupId, String targetUserId, String currentUserId, String confirmationPassword) {

        // 1. SECURITY CHECK: Verify the Owner's Password!
        // (Assuming you have a method in your profileProvider or a new AuthClient to check this)

        log.info(currentUserId);
        BasicProfileInfo info = profileProvider.getBasicProfile(currentUserId);
        boolean isPasswordValid = profileProvider.verifyUserPassword(info.getUsername(), confirmationPassword);


        if (!isPasswordValid) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS); // Or "Incorrect Password"
        }

        log.info(currentUserId);

        // 2. Fetch Group & Verify Current Owner
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // 3. Prevent transferring to self
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalStateException("You are already the owner of this group.");
        }

        // 4. Fetch Target Member & Ensure they are ACTIVE
        GroupMember targetMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        GroupMember previousAdmin = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (targetMember.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("Only active members can receive group ownership.");
        }

        // 5. ATOMIC UPDATES
        group.setOwnerId(targetUserId);
        groupRepository.save(group);

        if (targetMember.getRole() != MemberRole.ADMIN) {
            targetMember.setRole(MemberRole.ADMIN);
            previousAdmin.setRole(MemberRole.MEMBER);
            memberRepository.save(targetMember);
        }

        // 6. Fire Notification Event!
        eventPublisher.publishOwnershipTransferredEvent(group, targetUserId, currentUserId);

        log.info("User {} transferred ownership of group {} to user {}", currentUserId, groupId, targetUserId);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(String groupId, String currentUserId) {

        // 1. Fetch the core group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        // 2. Look up the current user's membership state
        Optional<GroupMember> membership = memberRepository.findByGroupIdAndUserId(groupId, currentUserId);

        // 3. Default to "NONE" if they are a completely new visitor
        String myRole = "NONE";
        String myStatus = "NONE";

        if (membership.isPresent()) {
            myRole = membership.get().getRole().name();
            myStatus = membership.get().getStatus().name();
        }

        // 4. Map and return using your existing DTO
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .coverImageUrl(group.getCoverImageUrl())
                .privacyType(group.getPrivacyType().name())
                .creatorId(group.getCreatorId())
                .ownerId(group.getOwnerId())
                .memberCount(group.getMemberCount())
                // Assuming you have tags in your Group entity. If not, omit this or pass an empty list.
                // .tags(group.getTags())
                .createdAt(group.getCreatedAt())
                .myRole(myRole)
                .myStatus(myStatus)
                .build();
    }
}