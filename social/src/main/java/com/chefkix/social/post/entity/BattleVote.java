package com.chefkix.social.post.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Document(collection = "battle_vote")
@CompoundIndex(name = "postId_userId_unique", def = "{'postId': 1, 'userId': 1}", unique = true)
public class BattleVote {
    @Id
    String id;
    String postId;
    String userId;
    String choice; // "A" or "B" (maps to battleRecipeIdA / battleRecipeIdB)
    @CreatedDate
    Instant createdAt;
}
