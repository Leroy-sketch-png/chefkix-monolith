package com.chefkix.social.group.mapper;

import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.entity.GroupMember;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    default List<GroupResponse> toGroupResponseList(List<Group> groups, List<GroupMember> memberships) {

        // 1. Safety check: If there are no groups, return an empty list immediately
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Build the dictionary (Map) manually
        Map<String, GroupMember> membershipMap = new HashMap<>();

        // Safety check in case memberships is null (e.g., a guest user)
        if (memberships != null) {
            for (GroupMember member : memberships) {
                // The Group ID is the "word" we look up, the Member object is the "definition"
                membershipMap.put(member.getGroupId(), member);
            }
        }

        // 3. Prepare the final empty list for our responses
        List<GroupResponse> responseList = new ArrayList<>();

        // 4. Loop through every single group we fetched from the database
        for (Group group : groups) {

            // A. Look up the user's membership for this specific group using our dictionary
            // If they aren't in the group, this will safely return 'null'
            GroupMember myMembership = membershipMap.get(group.getId());

            // B. Hand both objects to your existing single-item mapper
            GroupResponse response = toExploreResponse(group, myMembership);

            // C. Add the finished, fully-mapped object to our list
            responseList.add(response);
        }

        // 5. Return the final list to the Service
        return responseList;
    }
}
