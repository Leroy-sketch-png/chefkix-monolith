package com.chefkix.social.group.entity;

import com.chefkix.social.group.enums.MemberRole;
import com.chefkix.social.group.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "group_members")
@CompoundIndexes({
        // 1. Prevents duplicate join requests. A user has exactly ONE state per group.
        @CompoundIndex(name = "unique_group_user_idx", def = "{'groupId': 1, 'userId': 1}", unique = true),

        // 2. Used by the "Home Feed Aggregator" to instantly find all groups a user is active in.
        @CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}"),

        // 3. Used by Group Admins to quickly load all "PENDING" requests or "ACTIVE" members.
        @CompoundIndex(name = "group_status_idx", def = "{'groupId': 1, 'status': 1}")
})
public class GroupMember {

    @Id
    private String id;

    private String groupId;
    private String userId;

    private MemberRole role;
    private MemberStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime joinedAt;




}