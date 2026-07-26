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
String reporterId;

    @Indexed
String targetType;

    @Indexed
String targetId;

String reason;

String details;

    @Builder.Default
String status = "pending";

String reviewedBy;

String reviewNotes;

    Instant reviewedAt;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
