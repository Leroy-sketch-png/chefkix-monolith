package com.chefkix.social.group.dto.query;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupExploreQuery {

    String keyword;
    String privacy;
    Boolean isJoined;

    @Builder.Default
    String sortBy = "newest";

    String currentUserId;
    Set<String> joinedGroupIds;
}