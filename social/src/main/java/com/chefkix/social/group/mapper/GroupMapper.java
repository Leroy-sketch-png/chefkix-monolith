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
     */
    default GroupResponse toExploreResponse(Group group, GroupMember myMembership) {
        if (group == null) {
            return null;
        }

        String myRole = (myMembership != null && myMembership.getRole() != null)
                ? myMembership.getRole().name() : "NONE";

        String myStatus = (myMembership != null && myMembership.getStatus() != null)
                ? myMembership.getStatus().name() : "NONE";

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .coverImageUrl(group.getCoverImageUrl())
                .privacyType(group.getPrivacyType() != null ? group.getPrivacyType().name() : "PUBLIC")
                .creatorId(group.getCreatorId())
                .ownerId(group.getOwnerId())
                .memberCount(group.getMemberCount())
                .createdAt(group.getCreatedAt())
                .myRole(myRole)
                .myStatus(myStatus)
                .build();
    }

    default List<GroupResponse> toGroupResponseList(List<Group> groups, List<GroupMember> memberships) {

        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, GroupMember> membershipMap = new HashMap<>();

        if (memberships != null) {
            for (GroupMember member : memberships) {
                membershipMap.put(member.getGroupId(), member);
            }
        }

        List<GroupResponse> responseList = new ArrayList<>();

        for (Group group : groups) {

            GroupMember myMembership = membershipMap.get(group.getId());

            GroupResponse response = toExploreResponse(group, myMembership);

            responseList.add(response);
        }

        return responseList;
    }
}
