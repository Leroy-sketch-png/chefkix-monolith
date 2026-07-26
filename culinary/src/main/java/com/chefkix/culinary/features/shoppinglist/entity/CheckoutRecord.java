package com.chefkix.culinary.features.shoppinglist.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 *
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "checkout_records")
@CompoundIndex(name = "idx_user_created", def = "{'userId': 1, 'createdAt': -1}")
public class CheckoutRecord {

    @Id String id;

    @Indexed String userId;

    @Indexed(unique = true) String orderId;

    String shoppingListId;

String provider;

    int itemCount;

    double estimatedTotal;

    String checkoutUrl;

@Builder.Default String status = "redirected";

    @CreatedDate Instant createdAt;

    @LastModifiedDate Instant updatedAt;
}
