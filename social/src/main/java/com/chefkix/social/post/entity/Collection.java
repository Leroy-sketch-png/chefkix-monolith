package com.chefkix.social.post.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "collections")
@CompoundIndexes({
    @CompoundIndex(name = "user_updated_idx", def = "{'userId': 1, 'updatedAt': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Collection {

    @Id
    String id;

    @Indexed
    String userId;

    String name;
    String description;
    String coverImageUrl;

    @Builder.Default
    boolean isPublic = false;

    @Builder.Default
    List<String> postIds = new ArrayList<>();

    @Builder.Default
    int itemCount = 0;

    @CreatedDate
    Instant createdAt;

    @LastModifiedDate
    Instant updatedAt;
}
