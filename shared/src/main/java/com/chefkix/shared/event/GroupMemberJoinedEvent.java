package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a user officially becomes an ACTIVE member of a group.
 * <p>
 * Producer: social module (group). Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("GROUP_MEMBER_JOINED")
public class GroupMemberJoinedEvent extends BaseEvent {

    private String groupId;
    private String groupName;
    private String memberId;
    private String memberDisplayName;
    private String memberAvatarUrl;

    @Builder
    public GroupMemberJoinedEvent(String groupId, String groupName, String memberId,
                                  String memberDisplayName, String memberAvatarUrl,
                                  String adminId) {

        super("GROUP_MEMBER_JOINED", adminId);

        this.groupId = groupId;
        this.groupName = groupName;
        this.memberId = memberId;
        this.memberDisplayName = memberDisplayName;
        this.memberAvatarUrl = memberAvatarUrl;
    }
}