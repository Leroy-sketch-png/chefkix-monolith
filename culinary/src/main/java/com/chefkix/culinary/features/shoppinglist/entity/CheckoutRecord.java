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
 * Tracks checkout attempts for affiliate conversion tracking.
 * Revenue Pillar 4 (Intent): shopping list → checkout → grocery partner → commission.
 *
 * Lifecycle: REDIRECTED → CONFIRMED → DELIVERED (or CANCELLED)
 * Status transitions happen via webhook from grocery partner (Phase 1)
 * or manual polling (Phase 0).
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

    String provider; // "affiliate", "instacart", etc.

    int itemCount;

    double estimatedTotal;

    String checkoutUrl;

    @Builder.Default String status = "redirected"; // redirected, confirmed, in_progress, delivered, cancelled

    @CreatedDate Instant createdAt;

    @LastModifiedDate Instant updatedAt;
}
