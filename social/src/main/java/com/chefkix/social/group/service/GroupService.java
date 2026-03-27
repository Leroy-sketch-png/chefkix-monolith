package com.chefkix.social.group.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.chat.enums.RequestAction;
import com.chefkix.social.group.dto.query.GroupExploreQuery;
import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.request.GroupPrivacyUpdateRequest;
import com.chefkix.social.group.dto.request.GroupUpdateRequest;
import com.chefkix.social.group.dto.response.GroupMemberResponse;
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
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupService {

    final GroupRepository groupRepository;
    final GroupMemberRepository memberRepository;
    final GroupMapper mapper;
    final GroupEventPublisher eventPublisher;
    final MongoTemplate mongoTemplate;

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
                    .privacyType(parsePrivacyType(request.getPrivacyType()))
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

            // Atomic increment to prevent race conditions
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId)),
                    new Update().inc("memberCount", 1),
                    Group.class
            );
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
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        // If they were an ACTIVE member, we must decrease the count
        if (member.getStatus() == MemberStatus.ACTIVE) {
            // Atomic decrement to prevent race conditions
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId).and("memberCount").gt(0)),
                    new Update().inc("memberCount", -1),
                    Group.class
            );
        } else if (member.getStatus() == MemberStatus.BANNED) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
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

        if (!group.getOwnerId().equals(currentUserId) && !isGroupAdmin(groupId, currentUserId)) {
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

        if (!group.getOwnerId().equals(currentUserId) && !isGroupAdmin(groupId, currentUserId)) {
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

            // Atomic increment to prevent race conditions
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId)),
                    new Update().inc("memberCount", 1),
                    Group.class
            );

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

        if (!group.getOwnerId().equals(currentUserId) && !isGroupAdmin(groupId, currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // 2. Prevent the Owner from kicking themselves
        if (group.getOwnerId().equals(targetUserId)) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        // 3. Fetch the target member
        GroupMember targetMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // 4. Atomic decrement ONLY if they were an ACTIVE member
        if (targetMember.getStatus() == MemberStatus.ACTIVE) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId).and("memberCount").gt(0)),
                    new Update().inc("memberCount", -1),
                    Group.class
            );
        }

        // 5. Hard Delete: Remove the record from MongoDB
        memberRepository.delete(targetMember);

        log.info("Admin {} kicked user {} from group {}", currentUserId, targetUserId, groupId);
    }

    @Transactional
    public void transferOwnership(String groupId, String targetUserId, String currentUserId, String confirmationPassword) {

        // 1. SECURITY CHECK: Verify the Owner's Password!
        // (Assuming you have a method in your profileProvider or a new AuthClient to check this)

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
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        // 4. Fetch Target Member & Ensure they are ACTIVE
        GroupMember targetMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        GroupMember previousAdmin = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (targetMember.getStatus() != MemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        // 5. ATOMIC UPDATES
        group.setOwnerId(targetUserId);
        groupRepository.save(group);

        if (targetMember.getRole() != MemberRole.ADMIN) {
            targetMember.setRole(MemberRole.ADMIN);
            memberRepository.save(targetMember);
        }
        previousAdmin.setRole(MemberRole.MEMBER);
        memberRepository.save(previousAdmin);

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

    @Transactional(readOnly = true)
    public Page<GroupResponse> exploreGroups(GroupExploreQuery query, Pageable pageable) {

        // 1. Fetch user context so the repository can filter by 'isJoined'
        Map<String, GroupMember> myMemberships = memberRepository.findByUserId(query.getCurrentUserId())
                .stream().collect(Collectors.toMap(GroupMember::getGroupId, m -> m));

        query.setJoinedGroupIds(myMemberships.keySet());

        // 2. Execute Custom Repository Query & Map Results (The Clean 1-Liner!)
        return groupRepository.searchGroups(query, pageable)
                .map(group -> mapper.toExploreResponse(group, myMemberships.get(group.getId())));
    }

    @Transactional(readOnly = true)
    public Page<GroupMemberResponse> getGroupMembers(String groupId, String currentUserId, Pageable pageable) {

        // 1. SECURITY CHECK: Verify the requester is actually in the group AND is an Admin
        GroupMember requester = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION));

        if (requester.getRole() != MemberRole.ADMIN) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // 2. Fetch all ACTIVE members (we don't want to show pending or banned users here)
        Page<GroupMember> activeMembers = memberRepository.findAllByGroupIdAndStatus(
                groupId,
                MemberStatus.ACTIVE,
                pageable
        );

        // 3. Map to DTO and inject the Identity Profile Data
        return activeMembers.map(member -> {
            String displayName = "Unknown User";
            String avatarUrl = null;

            // Fetch visual data from Identity module safely
            try {
                BasicProfileInfo profile = profileProvider.getBasicProfile(member.getUserId());
                if (profile != null) {
                    displayName = profile.getDisplayName();
                    avatarUrl = profile.getAvatarUrl();
                }
            } catch (Exception e) {
                log.warn("Could not fetch profile for user {}", member.getUserId());
            }

            return GroupMemberResponse.builder()
                    .userId(member.getUserId())
                    .displayName(displayName)
                    .avatarUrl(avatarUrl)
                    .role(member.getRole().name())
                    .joinedAt(member.getJoinedAt())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public Slice<GroupResponse> getMyGroups(String currentUserId, String statusFilter, Pageable pageable) {

        // 1. Fetch Memberships using SLICE instead of Page
        Slice<GroupMember> membershipSlice;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            MemberStatus status = MemberStatus.valueOf(statusFilter.toUpperCase());
            membershipSlice = memberRepository.findByUserIdAndStatus(currentUserId, status, pageable);
        } else {
            membershipSlice = memberRepository.findByUserIdAndStatusNot(currentUserId, MemberStatus.BANNED, pageable);
        }

        if (membershipSlice.isEmpty()) {
            // Return an empty slice if they have no groups
            return new SliceImpl<>(List.of(), pageable, false);
        }

        // 2. Extract IDs
        List<String> joinedGroupIds = membershipSlice.getContent().stream()
                .map(GroupMember::getGroupId)
                .toList();

        // 3. Fetch Groups
        List<Group> joinedGroups = groupRepository.findAllById(joinedGroupIds);

        // 4. Map the List
        List<GroupResponse> responseList = mapper.toGroupResponseList(joinedGroups, membershipSlice.getContent());

        // 5. Wrap in a SliceImpl!
        // Notice we don't pass 'totalElements' anymore. We just pass 'hasNext()'.
        return new SliceImpl<>(responseList, pageable, membershipSlice.hasNext());
    }

    @Transactional
    public GroupResponse updateGroup(String groupId, GroupUpdateRequest request, String currentUserId) {

        // 1. SECURITY CHECK: Ensure requester is in the group and is an ADMIN
        GroupMember requester = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION));

        if (requester.getRole() != MemberRole.ADMIN) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION); // Or custom ErrorCode.NOT_GROUP_ADMIN
        }

        // 2. Fetch the Group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        // 3. Apply Partial Updates (Only overwrite if the frontend sent a value)
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            group.setName(request.getName());
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }

        if (request.getCoverImageUrl() != null) {
            group.setCoverImageUrl(request.getCoverImageUrl());
        }

        // 4. Save to Database
        group = groupRepository.save(group);

        // 5. Return the updated group (reusing our mapper!)
        return mapper.toExploreResponse(group, requester);
    }

    @Transactional
    public GroupResponse changePrivacy(String groupId, GroupPrivacyUpdateRequest request, String currentUserId) {

        // 1. SECURITY CHECK: Must be ADMIN
        GroupMember requester = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION));

        if (requester.getRole() != MemberRole.ADMIN) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        BasicProfileInfo info = profileProvider.getBasicProfile(currentUserId);
        boolean isPasswordValid = profileProvider.verifyUserPassword(info.getUsername(), request.getConfirmationPassword());


        if (!isPasswordValid) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS); // Or "Incorrect Password"
        }

        // 2. Fetch the Group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        // 3. Check if the privacy is actually changing
        PrivacyType newPrivacy = parsePrivacyType(request.getPrivacyType());
        PrivacyType oldPrivacy = group.getPrivacyType();

        if (oldPrivacy == newPrivacy) {
            return mapper.toExploreResponse(group, requester); // Nothing to do!
        }

        // 4. Update and Save the Group
        group.setPrivacyType(newPrivacy);
        group = groupRepository.save(group);

        // 5. THE SIDE EFFECT: Auto-accept pending members if going PUBLIC
        if (newPrivacy == PrivacyType.PUBLIC && oldPrivacy == PrivacyType.PRIVATE) {
            /* * Pro-Tip: If you expect 100,000+ pending members, you would wrap this
             * in an @Async method or publish a Spring ApplicationEvent so the
             * HTTP request doesn't wait for it to finish. But for standard apps,
             * Mongo bulk updates are fast enough to run synchronously!
             */
            memberRepository.approveAllPendingMembers(groupId);
            // Update memberCount to reflect newly approved members
            long activeCount = memberRepository.countByGroupIdAndStatus(groupId, MemberStatus.ACTIVE);
            group.setMemberCount((int) activeCount);
            group = groupRepository.save(group);
        }

        return mapper.toExploreResponse(group, requester);
    }

    private PrivacyType parsePrivacyType(String value) {
        try {
            return PrivacyType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean isGroupAdmin(String groupId, String userId) {
        return memberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> m.getStatus() == MemberStatus.ACTIVE && m.getRole() == MemberRole.ADMIN)
                .orElse(false);
    }
}