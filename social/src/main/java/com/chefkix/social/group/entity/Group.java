package com.chefkix.social.group.entity;

import com.chefkix.social.group.enums.PrivacyType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> tags;

private PrivacyType privacyType;
    private String creatorId;
    @Indexed
    private String ownerId;

private long memberCount;
    private LocalDateTime createdAt;

}
