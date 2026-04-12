package com.chefkix.identity.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Individual tip record. Immutable after creation.
 * Links tipper → creator for a specific recipe/post.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "tips")
@CompoundIndex(name = "idx_creator_created", def = "{'creatorId': 1, 'createdAt': -1}")
public class Tip {

    @Id String id;

    @Indexed String tipperId;

    @Indexed String creatorId;

    String recipeId; // optional — null for profile-level tips

    int amountCents; // tip amount in cents (e.g., 500 = $5.00)

    String currency; // "USD"

    String message; // optional message from tipper

    @Builder.Default String status = "pending"; // pending, completed, refunded

    String paymentIntentId; // Stripe payment intent (null in Phase 0)

    @CreatedDate Instant createdAt;
}
