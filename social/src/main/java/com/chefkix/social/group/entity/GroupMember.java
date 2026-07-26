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
        @CompoundIndex(name = "unique_group_user_idx", def = "{'groupId': 1, 'userId': 1}", unique = true),

        @CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}"),

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