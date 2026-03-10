package com.chefkix.social.moderation.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Appeal entity for users to contest bans.
 * Per spec 16-moderation.txt: Users can appeal with evidence.
 */
@Document(collection = "appeals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Appeal {
    @Id
    String id;

    @Indexed
    String userId;

    @Indexed
    String banId;

    String reason;

    List<String> evidenceUrls;

    /**
     * Status: "pending", "approved", "rejected"
     */
    @Builder.Default
    String status = "pending";

    String reviewedBy;

    String reviewNotes;

    Instant reviewedAt;

    @CreatedDate
    Instant createdAt;
}
