package com.chefkix.social.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionResponse {

    String id;
    String userId;
    String name;
    String description;
    String coverImageUrl;

    @JsonProperty("isPublic")
    boolean isPublic;

    int itemCount;
    List<String> postIds;

    Instant createdAt;
    Instant updatedAt;
}
