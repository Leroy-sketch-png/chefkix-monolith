package com.chefkix.social.group.entity;

import com.chefkix.social.group.enums.PrivacyType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "groups")
public class Group {
    @Id
    private String id;
    private String name;
    private String description;
    private String coverImageUrl;

    private PrivacyType privacyType; // Enum: PUBLIC, PRIVATE
    private String creatorId;
    @Indexed
    private String ownerId;

    private long memberCount; // Denormalized count
    private LocalDateTime createdAt;

}