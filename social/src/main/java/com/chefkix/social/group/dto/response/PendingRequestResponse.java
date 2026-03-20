package com.chefkix.social.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRequestResponse {

    // The ID of the user requesting to join (needed for the Accept/Reject API)
    private String userId;

    // Visual info for the Admin to see who they are accepting
    private String displayName;
    private String avatarUrl;

    // So the frontend can sort by "Oldest First" or say "Requested 2 days ago"
    private LocalDateTime requestedAt;
}