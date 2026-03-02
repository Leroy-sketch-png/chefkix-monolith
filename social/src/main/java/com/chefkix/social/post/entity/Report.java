package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

/**
 * Report entity for content moderation.
 * Users can report posts, comments, or recipes.
 * Per spec 13-moderation.txt: 3 unique reports triggers admin review.
 */
@Document(collection = "report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@CompoundIndex(name = "unique_report", def = "{'reporterId': 1, 'targetType': 1, 'targetId': 1}", unique = true)
public class Report {
    @Id
    String id;

    @Indexed
    String reporterId; // User who reported

    @Indexed
    String targetType; // "post", "comment", "recipe"

    @Indexed
    String targetId; // ID of the reported content

    String reason; // "fraud", "spam", "inappropriate", "other"

    String details; // Optional explanation

    @Builder.Default
    String status = "pending"; // "pending", "reviewed", "resolved", "dismissed"

    String reviewedBy; // Admin who reviewed

    String reviewNotes; // Admin notes

    Instant reviewedAt;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
