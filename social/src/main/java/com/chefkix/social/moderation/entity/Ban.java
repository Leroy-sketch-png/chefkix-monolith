package com.chefkix.social.moderation.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
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
     */
    @Builder.Default
    String scope = "all";

    /**
     */
    int durationDays;

String issuedBy;

    @CreatedDate
    Instant issuedAt;

    Instant expiresAt;

    @Builder.Default
    boolean active = true;

    /**
     */
    int offenseNumber;

    String relatedReportId;
}
