package com.chefkix.social.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.request.GroupPrivacyUpdateRequest;
import com.chefkix.social.group.dto.request.GroupUpdateRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    @Mock
    private ContentModerationProvider contentModerationProvider;

    @InjectMocks
    private GroupService groupService;

    @Test
    void createGroupPersistsSubmittedTags() {
        GroupCreationRequest request = createRequest(List.of("meal prep", "weeknight"));
        when(contentModerationProvider.moderate(
                        "Weeknight Cooks\nPractical meals\nmeal prep\nweeknight", "post_caption"))
                .thenReturn(ContentModerationProvider.ModerationResult.approved());
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group saved = invocation.getArgument(0);
            saved.setId("group-1");
            return saved;
        });
        when(memberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toGroupResponse(any(Group.class)))
                .thenReturn(com.chefkix.social.group.dto.response.GroupResponse.builder().id("group-1").build());

        groupService.createGroup(request, "owner-1");

        InOrder order = inOrder(contentModerationProvider, groupRepository, memberRepository);
        order.verify(contentModerationProvider).moderate(
                "Weeknight Cooks\nPractical meals\nmeal prep\nweeknight", "post_caption");
        order.verify(groupRepository).save(any(Group.class));
        order.verify(memberRepository).save(any(GroupMember.class));

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getTags()).containsExactly("meal prep", "weeknight");
    }

    @Test
    void createGroupRejectsUnapprovedMetadataBeforeSavingMembership() {
        GroupCreationRequest request = createRequest(List.of());
        when(contentModerationProvider.moderate(
                        "Weeknight Cooks\nPractical meals", "post_caption"))
                .thenReturn(new ContentModerationProvider.ModerationResult(
                        "flag", "unknown", "medium", 0.0, "Review required"));

        assertThatThrownBy(() -> groupService.createGroup(request, "owner-1"))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.CONTENT_MODERATION_FAILED));

        verify(groupRepository, never()).save(any(Group.class));
        verify(memberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    void updateGroupRejectsUnapprovedMetadataBeforeSaving() {
        GroupMember admin = GroupMember.builder()
                .groupId("group-1")
                .userId("owner-1")
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
        Group group = Group.builder()
                .id("group-1")
                .name("Weeknight Cooks")
                .description("Practical meals")
                .tags(List.of("meal prep"))
                .build();
        GroupUpdateRequest request = new GroupUpdateRequest();
        request.setName("Unsafe Group");
        request.setDescription("Updated description");
        when(memberRepository.findByGroupIdAndUserId("group-1", "owner-1"))
                .thenReturn(Optional.of(admin));
        when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
        when(contentModerationProvider.moderate(
                        "Unsafe Group\nUpdated description\nmeal prep", "post_caption"))
                .thenReturn(new ContentModerationProvider.ModerationResult(
                        "flag", "unknown", "medium", 0.0, "Review required"));

        assertThatThrownBy(() -> groupService.updateGroup("group-1", request, "owner-1"))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.CONTENT_MODERATION_FAILED));

        verify(groupRepository, never()).save(any(Group.class));
    }

    @Test
    void exploreGroupResponseIncludesStoredTags() {
        Group group = Group.builder()
                .id("group-1")
                .name("Weeknight Cooks")
                .tags(List.of("meal prep", "weeknight"))
                .build();

        GroupMapper groupMapper = org.mapstruct.factory.Mappers.getMapper(GroupMapper.class);

        assertThat(groupMapper.toGroupResponse(group).getTags())
                .containsExactly("meal prep", "weeknight");
        assertThat(groupMapper.toExploreResponse(group, null).getTags())
                .containsExactly("meal prep", "weeknight");
    }

    @Test
    void changePrivacyAcceptsValidConfirmationPasswordAndSavesPrivacy() {
        GroupMember admin = GroupMember.builder()
                .groupId("group-1")
                .userId("owner-1")
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
        Group group = Group.builder()
                .id("group-1")
                .privacyType(PrivacyType.PUBLIC)
                .memberCount(1)
                .build();
        BasicProfileInfo profile = BasicProfileInfo.builder()
                .userId("owner-1")
                .username("group-chef")
                .build();
        GroupPrivacyUpdateRequest request = new GroupPrivacyUpdateRequest();
        request.setPrivacyType("PRIVATE");
        request.setConfirmationPassword("correct-password");
        when(memberRepository.findByGroupIdAndUserId("group-1", "owner-1"))
                .thenReturn(Optional.of(admin));
        when(profileProvider.getBasicProfile("owner-1")).thenReturn(profile);
        when(profileProvider.verifyUserPassword("group-chef", "correct-password"))
                .thenReturn(true);
        when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        groupService.changePrivacy("group-1", request, "owner-1");

        assertThat(group.getPrivacyType()).isEqualTo(PrivacyType.PRIVATE);
        verify(profileProvider).verifyUserPassword("group-chef", "correct-password");
        verify(groupRepository).save(group);
    }

    @Test
    void changePrivacyRejectsInvalidConfirmationPasswordBeforeSaving() {
        GroupMember admin = GroupMember.builder()
                .groupId("group-1")
                .userId("owner-1")
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
        BasicProfileInfo profile = BasicProfileInfo.builder()
                .userId("owner-1")
                .username("group-chef")
                .build();
        GroupPrivacyUpdateRequest request = new GroupPrivacyUpdateRequest();
        request.setPrivacyType("PRIVATE");
        request.setConfirmationPassword("incorrect-password");
        when(memberRepository.findByGroupIdAndUserId("group-1", "owner-1"))
                .thenReturn(Optional.of(admin));
        when(profileProvider.getBasicProfile("owner-1")).thenReturn(profile);
        when(profileProvider.verifyUserPassword("group-chef", "incorrect-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> groupService.changePrivacy("group-1", request, "owner-1"))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(groupRepository, never()).save(any(Group.class));
        verify(groupRepository, never()).findById(anyString());
    }

    private GroupCreationRequest createRequest(List<String> tags) {
        GroupCreationRequest request = new GroupCreationRequest();
        request.setName("Weeknight Cooks");
        request.setDescription("Practical meals");
        request.setPrivacyType("PUBLIC");
        request.setTags(tags);
        return request;
    }

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
