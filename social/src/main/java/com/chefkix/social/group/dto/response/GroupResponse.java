package com.chefkix.social.group.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GroupResponse {
    private String id;
    private String name;
    private String description;
    private String coverImageUrl;
    private String privacyType;

    // Both will be the same initially
    private String creatorId;
    private String ownerId;

    private long memberCount;
    private List<String> tags;
    private LocalDateTime createdAt;
}