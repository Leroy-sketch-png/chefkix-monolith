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
     */
    @Builder.Default
    String status = "pending";

    String reviewedBy;

    String reviewNotes;

    Instant reviewedAt;

    @CreatedDate
    Instant createdAt;
}
