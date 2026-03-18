package com.chefkix.social.group.repository;

import com.chefkix.social.group.entity.GroupMember;
import com.chefkix.social.group.enums.MemberRole;
import com.chefkix.social.group.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends MongoRepository<GroupMember, String> {

    // --- 1. SINGLE RECORD LOOKUPS ---

    // Find a specific user's membership in a specific group (used for validating permissions)
    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);

    // Check if a user is already in the group before saving a new request
    boolean existsByGroupIdAndUserId(String groupId, String userId);
    List<GroupMember> findByUserIdAndGroupIdIn(String userId, List<String> groupIds);


    // --- 2. GROUP ADMIN QUERIES (Using group_status_idx) ---

    // Get all members of a group with a specific status (e.g., all ACTIVE members, or all PENDING requests)
    Page<GroupMember> findAllByGroupIdAndStatus(String groupId, MemberStatus status, Pageable pageable);

    // Get all admins or moderators of a group
    List<GroupMember> findAllByGroupIdAndRoleInAndStatus(
            String groupId,
            List<MemberRole> roles,
            MemberStatus status
    );

    // Count how many pending requests a group has (for the admin notification badge)
    long countByGroupIdAndStatus(String groupId, MemberStatus status);


    // --- 3. FEED & PROFILE QUERIES (Using user_status_idx) ---

    // Get all groups a user is actively a part of (CRITICAL for the Home Feed Aggregator)
    List<GroupMember> findAllByUserIdAndStatus(String userId, MemberStatus status);

    List<GroupMember> findByUserId(String currentUserId);

    Slice<GroupMember> findByUserIdAndStatus(String userId, MemberStatus status, Pageable pageable);
    Slice<GroupMember> findByUserIdAndStatusNot(String userId, MemberStatus status, Pageable pageable);
}