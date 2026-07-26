package com.chefkix.identity.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 *
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "creator_tip_settings")
public class CreatorTipSettings {

    @Id String id;

    @Indexed(unique = true) String userId;

    @Builder.Default boolean tipsEnabled = false;

String payoutAccountId;

    @Builder.Default String currency = "USD";

    @Builder.Default int[] suggestedAmounts = new int[]{1, 3, 5};

String thankYouMessage;

    @CreatedDate Instant createdAt;

    @LastModifiedDate Instant updatedAt;
}
