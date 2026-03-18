package com.chefkix.social.group.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Hides null fields from the JSON!
public class GroupResponse {
    private String id;
    private String name;
    private String description;
    private String coverImageUrl;
    private String privacyType;

    private String creatorId;
    private String ownerId;

    private long memberCount;
    private List<String> tags;
    private LocalDateTime createdAt;

    // --- Add these for contextual frontend rendering ---
    private String myRole;   // e.g., "ADMIN", "MEMBER"
    private String myStatus; // e.g., "ACTIVE", "PENDING", "BANNED"
}