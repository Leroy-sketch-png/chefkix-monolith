package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Fired when a user's request to join a private group is approved by an admin.
 * <p>
 * Producer: social module (group). Consumer: notification module.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("GROUP_REQUEST_APPROVED")
public class GroupRequestApprovedEvent extends BaseEvent {

    private String groupId;
    private String groupName;
    private String groupCoverImageUrl;
    private String requesterId;
    private String adminId;

    @Builder
    public GroupRequestApprovedEvent(String groupId, String groupName,
                                     String groupCoverImageUrl, String requesterId,
                                     String adminId) {
        super("GROUP_REQUEST_APPROVED", adminId);

        this.groupId = groupId;
        this.groupName = groupName;
        this.groupCoverImageUrl = groupCoverImageUrl;
        this.requesterId = requesterId;
        this.adminId = adminId;
    }
}