package com.chefkix.social.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "plate_rating")
@CompoundIndex(def = "{'postId': 1, 'userId': 1}", name = "idx_plate_rating_unique", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlateRating {
    @Id String id;
    String postId;
    String userId;
    String rating; // "FIRE" or "CRINGE"
    @CreatedDate Instant createdAt;
}
