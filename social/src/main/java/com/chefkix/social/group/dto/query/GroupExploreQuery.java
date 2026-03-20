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

    // --- Search & Filter from Frontend ---
    String keyword;
    String privacy;
    Boolean isJoined;

    // --- Custom Sorting ---
    @Builder.Default
    String sortBy = "newest";

    // --- Backend Injected Context (Not from Frontend) ---
    String currentUserId;
    Set<String> joinedGroupIds;
}