package com.chefkix.social.group.mapper;

import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.entity.ChatMessage;
import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.entity.GroupMember;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    GroupResponse toGroupResponse(Group group);

    Group toGroup(GroupCreationRequest request);

    /**
     * Custom mapping for the Explore/Discovery feed that injects the user's current context.
     */
    default GroupResponse toExploreResponse(Group group, GroupMember myMembership) {
        if (group == null) {
            return null;
        }

        // 1. Calculate the contextual status (Default to "NONE" if they haven't joined)
        String myRole = (myMembership != null && myMembership.getRole() != null)
                ? myMembership.getRole().name() : "NONE";

        String myStatus = (myMembership != null && myMembership.getStatus() != null)
                ? myMembership.getStatus().name() : "NONE";

        // 2. Build and return the rich DTO
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .coverImageUrl(group.getCoverImageUrl())
                .privacyType(group.getPrivacyType() != null ? group.getPrivacyType().name() : "PUBLIC")
                .creatorId(group.getCreatorId())
                .ownerId(group.getOwnerId())
                .memberCount(group.getMemberCount())
                // .tags(group.getTags()) // Uncomment if you have tags in your Group entity!
                .createdAt(group.getCreatedAt())
                .myRole(myRole)
                .myStatus(myStatus)
                .build();
    }
}
