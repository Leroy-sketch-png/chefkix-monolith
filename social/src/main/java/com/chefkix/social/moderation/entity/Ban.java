package com.chefkix.social.moderation.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Ban entity for user suspensions.
 * Per spec 16-moderation.txt: Escalating penalties.
 * 1st offense = 3 days, 2nd = 7 days, 3rd = 14 days, 4th = permanent.
 */
@Document(collection = "bans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ban {
    @Id
    String id;

    @Indexed
    String userId;

    String reason;

    /**
     * Scope of the ban: "post", "comment", "all"
     */
    @Builder.Default
    String scope = "all";

    /**
     * Duration in days. -1 = permanent.
     */
    int durationDays;

    String issuedBy; // Admin userId

    @CreatedDate
    Instant issuedAt;

    Instant expiresAt;

    @Builder.Default
    boolean active = true;

    /**
     * Which offense number this is (1st, 2nd, 3rd, 4th+)
     */
    int offenseNumber;

    String relatedReportId;
}
