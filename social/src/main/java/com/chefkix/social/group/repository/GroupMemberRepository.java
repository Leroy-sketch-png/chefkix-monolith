package com.chefkix.social.group.repository;

import com.chefkix.social.group.entity.GroupMember;
import com.chefkix.social.group.enums.MemberRole;
import com.chefkix.social.group.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends MongoRepository<GroupMember, String> {


    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);

    boolean existsByGroupIdAndUserId(String groupId, String userId);
    List<GroupMember> findByUserIdAndGroupIdIn(String userId, List<String> groupIds);


    Page<GroupMember> findAllByGroupIdAndStatus(String groupId, MemberStatus status, Pageable pageable);

    List<GroupMember> findAllByGroupIdAndStatus(String groupId, MemberStatus status);

    List<GroupMember> findAllByGroupId(String groupId);

    List<GroupMember> findAllByGroupIdAndRoleInAndStatus(
            String groupId,
            List<MemberRole> roles,
            MemberStatus status
    );

    long countByGroupIdAndStatus(String groupId, MemberStatus status);


    List<GroupMember> findAllByUserIdAndStatus(String userId, MemberStatus status);

    List<GroupMember> findByUserId(String currentUserId);

    Slice<GroupMember> findByUserIdAndStatus(String userId, MemberStatus status, Pageable pageable);
    Slice<GroupMember> findByUserIdAndStatusNot(String userId, MemberStatus status, Pageable pageable);

    @Query("{ 'groupId': ?0, 'status': 'PENDING' }")
    @Update("{ '$set': { 'status': 'ACTIVE' } }")
    void approveAllPendingMembers(String groupId);
}