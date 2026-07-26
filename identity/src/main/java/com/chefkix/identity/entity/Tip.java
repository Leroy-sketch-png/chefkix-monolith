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
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "tips")
@CompoundIndex(name = "idx_creator_created", def = "{'creatorId': 1, 'createdAt': -1}")
public class Tip {

    @Id String id;

    @Indexed String tipperId;

    @Indexed String creatorId;

String recipeId;

int amountCents;

String currency;

String message;

@Builder.Default String status = "pending";

String paymentIntentId;

    @CreatedDate Instant createdAt;
}
