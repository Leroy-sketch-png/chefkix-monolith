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
import java.time.ZoneOffset;
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

    final ProfileProvider profileProvider;

    @Transactional
    public GroupResponse createGroup(GroupCreationRequest request, String currentUserId) {

        try {
            Group group = Group.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .coverImageUrl(request.getCoverImageUrl())
                    .privacyType(parsePrivacyType(request.getPrivacyType()))
                    .creatorId(currentUserId)
                    .ownerId(currentUserId)
                    .memberCount(1)
                    .createdAt(utcNow())
                    .build();

            Group savedGroup = groupRepository.save(group);

            GroupMember adminMember = GroupMember.builder()
                    .groupId(savedGroup.getId())
                    .userId(currentUserId)
                    .role(MemberRole.ADMIN)
                    .status(MemberStatus.ACTIVE)
                    .joinedAt(utcNow())
                    .build();

            memberRepository.save(adminMember);

            GroupResponse response = mapper.toGroupResponse(savedGroup);
            response.setMyRole(MemberRole.ADMIN.toString());
            response.setMyStatus(MemberStatus.ACTIVE.toString());

            return response;
        } catch (DataAccessException e) {
            log.error("Database error while creating group for user {}", currentUserId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public long cleanupDeletedUserData(String userId) {
        long affectedRecords = 0;
        Set<String> dissolvedGroupIds = new HashSet<>();

        List<GroupMember> userMemberships = new ArrayList<>(memberRepository.findByUserId(userId));
        Map<String, GroupMember> membershipsByGroupId = userMemberships.stream()
                .collect(Collectors.toMap(GroupMember::getGroupId, membership -> membership, (left, right) -> left));
        Set<String> removedMembershipIds = new HashSet<>();

        List<Group> ownedGroups = groupRepository.findAllByOwnerId(userId);
        for (Group group : ownedGroups) {
            GroupMember deletedOwnerMembership = membershipsByGroupId.get(group.getId());
            List<GroupMember> activeSurvivors = memberRepository.findAllByGroupIdAndStatus(group.getId(), MemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> !userId.equals(member.getUserId()))
                    .toList();

            Optional<GroupMember> successor = pickOwnershipSuccessor(activeSurvivors);
            if (successor.isPresent()) {
                GroupMember newOwnerMembership = successor.get();
                group.setOwnerId(newOwnerMembership.getUserId());
                if (Objects.equals(group.getCreatorId(), userId)) {
                    group.setCreatorId(newOwnerMembership.getUserId());
                }
                groupRepository.save(group);
                affectedRecords += 1;

                if (newOwnerMembership.getRole() != MemberRole.ADMIN) {
                    newOwnerMembership.setRole(MemberRole.ADMIN);
                    memberRepository.save(newOwnerMembership);
                    affectedRecords += 1;
                }

                if (deletedOwnerMembership != null) {
                    if (deletedOwnerMembership.getStatus() == MemberStatus.ACTIVE) {
                        decrementGroupMemberCount(group.getId());
                    }
                    memberRepository.delete(deletedOwnerMembership);
                    removedMembershipIds.add(deletedOwnerMembership.getId());
                    affectedRecords += 1;
                }

                eventPublisher.publishOwnershipTransferredEvent(group, newOwnerMembership.getUserId(), userId);
            } else {
                List<GroupMember> groupMembers = memberRepository.findAllByGroupId(group.getId());
                if (!groupMembers.isEmpty()) {
                    memberRepository.deleteAll(groupMembers);
                    groupMembers.stream()
                            .map(GroupMember::getId)
                            .filter(Objects::nonNull)
                            .forEach(removedMembershipIds::add);
                    affectedRecords += groupMembers.size();
                }

                groupRepository.delete(group);
                dissolvedGroupIds.add(group.getId());
                affectedRecords += 1;
            }
        }

        List<Group> creatorGroups = groupRepository.findAllByCreatorId(userId);
        List<Group> creatorGroupsToUpdate = creatorGroups.stream()
                .filter(group -> !dissolvedGroupIds.contains(group.getId()))
                .filter(group -> Objects.equals(group.getCreatorId(), userId))
                .filter(group -> !Objects.equals(group.getOwnerId(), userId))
                .peek(group -> group.setCreatorId(group.getOwnerId()))
                .toList();
        if (!creatorGroupsToUpdate.isEmpty()) {
            groupRepository.saveAll(creatorGroupsToUpdate);
            affectedRecords += creatorGroupsToUpdate.size();
        }

        List<GroupMember> remainingMemberships = userMemberships.stream()
                .filter(member -> member.getId() == null || !removedMembershipIds.contains(member.getId()))
                .filter(member -> !dissolvedGroupIds.contains(member.getGroupId()))
                .toList();
        remainingMemberships.stream()
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .forEach(member -> decrementGroupMemberCount(member.getGroupId()));

        if (!remainingMemberships.isEmpty()) {
            memberRepository.deleteAll(remainingMemberships);
            affectedRecords += remainingMemberships.size();
        }

        return affectedRecords;
    }

    @Transactional
    public JoinGroupResponse handleJoinRequest(String groupId, String currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        log.info("Joining group: {}", groupId);

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

        if (group.getPrivacyType() == PrivacyType.PUBLIC) {
            assignedStatus = MemberStatus.ACTIVE;
            message = "Successfully joined the group!";

            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId)),
                    new Update().inc("memberCount", 1),
                    Group.class
            );
        } else {
            assignedStatus = MemberStatus.PENDING;
            message = "Join request sent. Waiting for admin approval.";
        }

        GroupMember newMember = GroupMember.builder()
                .groupId(groupId)
                .userId(currentUserId)
                .role(MemberRole.MEMBER)
                .status(assignedStatus)
            .requestedAt(utcNow())
            .joinedAt(assignedStatus == MemberStatus.ACTIVE ? utcNow() : null)
                .build();

        memberRepository.save(newMember);

        eventPublisher.publishMembershipEvent(group, currentUserId, assignedStatus);

        return JoinGroupResponse.builder()
                .groupId(groupId)
                .membershipStatus(assignedStatus.name())
                .message(message)
                .build();
    }


    @Transactional
    public void handleLeaveOrCancel(String groupId, String currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (group.getOwnerId().equals(currentUserId)) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        if (member.getStatus() == MemberStatus.ACTIVE) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId).and("memberCount").gt(0)),
                    new Update().inc("memberCount", -1),
                    Group.class
            );
        } else if (member.getStatus() == MemberStatus.BANNED) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        memberRepository.delete(member);

        log.info("Member {} (status: {}) left group {} (\"{}\")", currentUserId, member.getStatus(), groupId, group.getName());
    }

    @Transactional(readOnly = true)
    public Page<PendingRequestResponse> getPendingRequests(String groupId, String currentUserId, Pageable pageable) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId) && !isGroupAdmin(groupId, currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        Page<GroupMember> pendingMembers = memberRepository.findAllByGroupIdAndStatus(
                groupId,
                MemberStatus.PENDING,
                pageable
        );

        return pendingMembers.map(member -> {
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

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId) && !isGroupAdmin(groupId, currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        GroupMember pendingMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

        if (pendingMember.getStatus() != MemberStatus.PENDING) {
            throw new AppException(ErrorCode.PENDING_NOT_FOUND);
        }

        if (action == RequestAction.ACCEPT) {
            pendingMember.setStatus(MemberStatus.ACTIVE);
            pendingMember.setJoinedAt(utcNow());
            memberRepository.save(pendingMember);

            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId)),
                    new Update().inc("memberCount", 1),
                    Group.class
            );

            eventPublisher.publishRequestApprovedEvent(group, currentUserId, pendingMember.getId());

        } else if (action == RequestAction.REJECT) {
            memberRepository.delete(pendingMember);
        }
    }

    @Transactional
    public void kickMember(String groupId, String targetUserId, String currentUserId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId) && !isGroupAdmin(groupId, currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        if (group.getOwnerId().equals(targetUserId)) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        GroupMember targetMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (targetMember.getStatus() == MemberStatus.ACTIVE) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(groupId).and("memberCount").gt(0)),
                    new Update().inc("memberCount", -1),
                    Group.class
            );
        }

        memberRepository.delete(targetMember);

        log.info("Admin {} kicked user {} from group {}", currentUserId, targetUserId, groupId);
    }

    @Transactional
    public void transferOwnership(String groupId, String targetUserId, String currentUserId, String confirmationPassword) {


        BasicProfileInfo info = profileProvider.getBasicProfile(currentUserId);
        boolean isPasswordValid = profileProvider.verifyUserPassword(info.getUsername(), confirmationPassword);


        if (!isPasswordValid) {
throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info(currentUserId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwnerId().equals(currentUserId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        if (currentUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        GroupMember targetMember = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        GroupMember previousAdmin = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (targetMember.getStatus() != MemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        group.setOwnerId(targetUserId);
        groupRepository.save(group);

        if (targetMember.getRole() != MemberRole.ADMIN) {
            targetMember.setRole(MemberRole.ADMIN);
            memberRepository.save(targetMember);
        }
        previousAdmin.setRole(MemberRole.MEMBER);
        memberRepository.save(previousAdmin);

        eventPublisher.publishOwnershipTransferredEvent(group, targetUserId, currentUserId);

        log.info("User {} transferred ownership of group {} to user {}", currentUserId, groupId, targetUserId);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(String groupId, String currentUserId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        Optional<GroupMember> membership = memberRepository.findByGroupIdAndUserId(groupId, currentUserId);

        String myRole = "NONE";
        String myStatus = "NONE";

        if (membership.isPresent()) {
            myRole = membership.get().getRole().name();
            myStatus = membership.get().getStatus().name();
        }

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .coverImageUrl(group.getCoverImageUrl())
                .privacyType(group.getPrivacyType().name())
                .creatorId(group.getCreatorId())
                .ownerId(group.getOwnerId())
                .memberCount(group.getMemberCount())
                .createdAt(group.getCreatedAt())
                .myRole(myRole)
                .myStatus(myStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<GroupResponse> exploreGroups(GroupExploreQuery query, Pageable pageable) {

        Map<String, GroupMember> myMemberships = memberRepository.findByUserId(query.getCurrentUserId())
                .stream().collect(Collectors.toMap(GroupMember::getGroupId, m -> m));

        query.setJoinedGroupIds(myMemberships.keySet());

        return groupRepository.searchGroups(query, pageable)
                .map(group -> mapper.toExploreResponse(group, myMemberships.get(group.getId())));
    }

    @Transactional(readOnly = true)
    public Page<GroupMemberResponse> getGroupMembers(String groupId, String currentUserId, Pageable pageable) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        Optional<GroupMember> requester = memberRepository.findByGroupIdAndUserId(groupId, currentUserId);

        if (requester.map(GroupMember::getStatus).orElse(null) == MemberStatus.BANNED) {
            throw new AppException(ErrorCode.GROUP_BANNED);
        }

        boolean canViewMembers = group.getPrivacyType() == PrivacyType.PUBLIC
                || requester.map(GroupMember::getStatus)
                .filter(status -> status == MemberStatus.ACTIVE)
                .isPresent();
        if (!canViewMembers) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        Page<GroupMember> activeMembers = memberRepository.findAllByGroupIdAndStatus(
                groupId,
                MemberStatus.ACTIVE,
                pageable
        );

        return activeMembers.map(member -> {
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

        Slice<GroupMember> membershipSlice;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            MemberStatus status = MemberStatus.valueOf(statusFilter.toUpperCase());
            membershipSlice = memberRepository.findByUserIdAndStatus(currentUserId, status, pageable);
        } else {
            membershipSlice = memberRepository.findByUserIdAndStatusNot(currentUserId, MemberStatus.BANNED, pageable);
        }

        if (membershipSlice.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        List<String> joinedGroupIds = membershipSlice.getContent().stream()
                .map(GroupMember::getGroupId)
                .toList();

        List<Group> joinedGroups = groupRepository.findAllById(joinedGroupIds);

        List<GroupResponse> responseList = mapper.toGroupResponseList(joinedGroups, membershipSlice.getContent());

        return new SliceImpl<>(responseList, pageable, membershipSlice.hasNext());
    }

    @Transactional
    public GroupResponse updateGroup(String groupId, GroupUpdateRequest request, String currentUserId) {

        GroupMember requester = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION));

        if (requester.getRole() != MemberRole.ADMIN) {
throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            group.setName(request.getName());
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }

        if (request.getCoverImageUrl() != null) {
            group.setCoverImageUrl(request.getCoverImageUrl());
        }

        group = groupRepository.save(group);

        return mapper.toExploreResponse(group, requester);
    }

    @Transactional
    public GroupResponse changePrivacy(String groupId, GroupPrivacyUpdateRequest request, String currentUserId) {

        GroupMember requester = memberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION));

        if (requester.getRole() != MemberRole.ADMIN) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        BasicProfileInfo info = profileProvider.getBasicProfile(currentUserId);
        boolean isPasswordValid = profileProvider.verifyUserPassword(info.getUsername(), request.getConfirmationPassword());


        if (!isPasswordValid) {
throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        PrivacyType newPrivacy = parsePrivacyType(request.getPrivacyType());
        PrivacyType oldPrivacy = group.getPrivacyType();

        if (oldPrivacy == newPrivacy) {
return mapper.toExploreResponse(group, requester);
        }

        group.setPrivacyType(newPrivacy);
        group = groupRepository.save(group);

        if (newPrivacy == PrivacyType.PUBLIC && oldPrivacy == PrivacyType.PRIVATE) {
            memberRepository.approveAllPendingMembers(groupId);
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

    private Optional<GroupMember> pickOwnershipSuccessor(List<GroupMember> activeMembers) {
        return activeMembers.stream()
                .sorted(Comparator.comparingInt(this::memberRolePriority)
                        .thenComparing(member -> member.getJoinedAt() != null ? member.getJoinedAt() : LocalDateTime.MAX))
                .findFirst();
    }

    private int memberRolePriority(GroupMember member) {
        return switch (member.getRole()) {
            case ADMIN -> 0;
            case MODERATOR -> 1;
            case MEMBER -> 2;
        };
    }

    private void decrementGroupMemberCount(String groupId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(groupId).and("memberCount").gt(0)),
                new Update().inc("memberCount", -1),
                Group.class
        );
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean isGroupAdmin(String groupId, String userId) {
        return memberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> m.getStatus() == MemberStatus.ACTIVE && m.getRole() == MemberRole.ADMIN)
                .orElse(false);
    }
}
