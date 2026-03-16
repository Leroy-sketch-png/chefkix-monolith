package com.chefkix.social.group.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
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
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupService {

    final GroupRepository groupRepository;
    final GroupMemberRepository memberRepository;
    final KafkaTemplate<String, Object> kafkaTemplate;
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

}
