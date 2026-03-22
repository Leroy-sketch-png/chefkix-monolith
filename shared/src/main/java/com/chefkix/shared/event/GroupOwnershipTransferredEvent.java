package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("GROUP_OWNERSHIP_TRANSFERRED")
public class GroupOwnershipTransferredEvent extends BaseEvent {

    private String groupId;
    private String groupName;
    private String groupCoverImageUrl;
    private String newOwnerId;
    private String oldOwnerId;

    @Builder
    public GroupOwnershipTransferredEvent(String groupId, String groupName,
                                          String groupCoverImageUrl, String newOwnerId,
                                          String oldOwnerId) {
        // The old owner triggered this event
        super("GROUP_OWNERSHIP_TRANSFERRED", oldOwnerId);

        this.groupId = groupId;
        this.groupName = groupName;
        this.groupCoverImageUrl = groupCoverImageUrl;
        this.newOwnerId = newOwnerId;
        this.oldOwnerId = oldOwnerId;
    }
}