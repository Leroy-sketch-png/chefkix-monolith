package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a user requests to join a PRIVATE group.
 * <p>
 * Producer: social module (group). Consumer: notification module.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("GROUP_JOIN_REQUESTED")
public class GroupJoinRequestedEvent extends BaseEvent {

    private String groupId;
    private String groupName;
    private String requesterId;
    private String requesterDisplayName;
    private String requesterAvatarUrl;

    @Builder
    public GroupJoinRequestedEvent(String groupId, String groupName, String requesterId,
                                   String requesterDisplayName, String requesterAvatarUrl,
                                   String adminId) {

        // The adminId is passed as the BaseEvent 'userId' so the notification
        // module knows exactly whose bell icon to light up.
        super("GROUP_JOIN_REQUESTED", adminId);

        this.groupId = groupId;
        this.groupName = groupName;
        this.requesterId = requesterId;
        this.requesterDisplayName = requesterDisplayName;
        this.requesterAvatarUrl = requesterAvatarUrl;
    }
}