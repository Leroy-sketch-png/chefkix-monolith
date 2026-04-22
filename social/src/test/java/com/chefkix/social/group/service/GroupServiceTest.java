package com.chefkix.social.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.shared.exception.AppException;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.entity.GroupMember;
import com.chefkix.social.group.enums.MemberRole;
import com.chefkix.social.group.enums.MemberStatus;
import com.chefkix.social.group.enums.PrivacyType;
import com.chefkix.social.group.mapper.GroupMapper;
import com.chefkix.social.group.publisher.GroupEventPublisher;
import com.chefkix.social.group.repository.GroupMemberRepository;
import com.chefkix.social.group.repository.GroupRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository memberRepository;
    @Mock
    private GroupMapper mapper;
    @Mock
    private GroupEventPublisher eventPublisher;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private ProfileProvider profileProvider;

    @InjectMocks
    private GroupService groupService;

    @Test
    void cleanupDeletedUserDataTransfersOwnershipToHighestRankedActiveSurvivor() {
        String userId = "user-1";
        Group ownedGroup = Group.builder()
                .id("group-1")
                .creatorId(userId)
                .ownerId(userId)
                .memberCount(3)
                .build();
        GroupMember deletedOwnerMembership = GroupMember.builder()
                .id("membership-owner")
                .groupId("group-1")
                .userId(userId)
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.of(2026, 4, 1, 8, 0))
                .build();
        GroupMember moderatorMembership = GroupMember.builder()
                .id("membership-moderator")
                .groupId("group-1")
                .userId("user-2")
                .role(MemberRole.MODERATOR)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.of(2026, 4, 10, 8, 0))
                .build();
        GroupMember memberMembership = GroupMember.builder()
                .id("membership-member")
                .groupId("group-1")
                .userId("user-3")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.of(2026, 4, 2, 8, 0))
                .build();

        when(memberRepository.findByUserId(userId)).thenReturn(List.of(deletedOwnerMembership));
        when(groupRepository.findAllByOwnerId(userId)).thenReturn(List.of(ownedGroup));
        when(memberRepository.findAllByGroupIdAndStatus("group-1", MemberStatus.ACTIVE))
                .thenReturn(List.of(deletedOwnerMembership, moderatorMembership, memberMembership));
        when(groupRepository.findAllByCreatorId(userId)).thenReturn(List.of(ownedGroup));

        long affectedRecords = groupService.cleanupDeletedUserData(userId);

        assertThat(affectedRecords).isEqualTo(3);
        assertThat(ownedGroup.getOwnerId()).isEqualTo("user-2");
        assertThat(ownedGroup.getCreatorId()).isEqualTo("user-2");
        assertThat(moderatorMembership.getRole()).isEqualTo(MemberRole.ADMIN);

        verify(groupRepository).save(ownedGroup);
        verify(memberRepository).save(moderatorMembership);
        verify(memberRepository).delete(deletedOwnerMembership);
        verify(mongoTemplate).updateFirst(any(), any(), eq(Group.class));
        verify(eventPublisher).publishOwnershipTransferredEvent(ownedGroup, "user-2", userId);
        verify(groupRepository, never()).delete(ownedGroup);
    }

    @Test
    void cleanupDeletedUserDataDissolvesOwnerlessGroupsAndReassignsCreatorIdOnSurvivors() {
        String userId = "user-1";
        Group dissolvedGroup = Group.builder()
                .id("group-1")
                .creatorId(userId)
                .ownerId(userId)
                .memberCount(1)
                .build();
        Group creatorOnlyGroup = Group.builder()
                .id("group-2")
                .creatorId(userId)
                .ownerId("user-2")
                .memberCount(2)
                .build();
        GroupMember dissolvedOwnerMembership = GroupMember.builder()
                .id("membership-owner")
                .groupId("group-1")
                .userId(userId)
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
        GroupMember pendingMembership = GroupMember.builder()
                .id("membership-pending")
                .groupId("group-1")
                .userId("user-4")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.PENDING)
                .build();
        GroupMember creatorOnlyMembership = GroupMember.builder()
                .id("membership-creator-only")
                .groupId("group-2")
                .userId(userId)
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(memberRepository.findByUserId(userId))
                .thenReturn(new ArrayList<>(List.of(dissolvedOwnerMembership, creatorOnlyMembership)));
        when(groupRepository.findAllByOwnerId(userId)).thenReturn(List.of(dissolvedGroup));
        when(memberRepository.findAllByGroupIdAndStatus("group-1", MemberStatus.ACTIVE))
                .thenReturn(List.of(dissolvedOwnerMembership));
        when(memberRepository.findAllByGroupId("group-1"))
                .thenReturn(List.of(dissolvedOwnerMembership, pendingMembership));
        when(groupRepository.findAllByCreatorId(userId)).thenReturn(List.of(dissolvedGroup, creatorOnlyGroup));

        long affectedRecords = groupService.cleanupDeletedUserData(userId);

        assertThat(affectedRecords).isEqualTo(5);
        assertThat(creatorOnlyGroup.getCreatorId()).isEqualTo("user-2");

        verify(memberRepository).deleteAll((Iterable<GroupMember>) List.of(dissolvedOwnerMembership, pendingMembership));
        verify(groupRepository).delete(dissolvedGroup);
        verify(groupRepository).saveAll((Iterable<Group>) List.of(creatorOnlyGroup));
        verify(memberRepository).deleteAll((Iterable<GroupMember>) List.of(creatorOnlyMembership));
        verify(mongoTemplate).updateFirst(any(), any(), eq(Group.class));
        verify(eventPublisher, never()).publishOwnershipTransferredEvent(any(), any(), any());
    }

    @Test
    void getGroupMembersAllowsPublicGroupViewers() {
        String groupId = "group-1";
        String currentUserId = "user-1";
        var pageable = PageRequest.of(0, 20);
        Group group = Group.builder()
                .id(groupId)
                .privacyType(PrivacyType.PUBLIC)
                .build();
        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId("user-2")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.of(2026, 4, 1, 12, 0))
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.empty());
        when(memberRepository.findAllByGroupIdAndStatus(groupId, MemberStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(member), pageable, 1));

        var result = groupService.getGroupMembers(groupId, currentUserId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getUserId()).isEqualTo("user-2");
    }

    @Test
    void getGroupMembersRejectsPrivateGroupVisitorsWithoutActiveMembership() {
        String groupId = "group-1";
        String currentUserId = "user-1";
        var pageable = PageRequest.of(0, 20);
        Group group = Group.builder()
                .id(groupId)
                .privacyType(PrivacyType.PRIVATE)
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupMembers(groupId, currentUserId, pageable))
                .isInstanceOf(AppException.class);

        verify(memberRepository, never()).findAllByGroupIdAndStatus(groupId, MemberStatus.ACTIVE, pageable);
    }
}