package com.chefkix.identity.entity;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "friend_requests")
// Đảm bảo 1 người chỉ gửi 1 request cho người kia
@CompoundIndex(name = "request_unique", def = "{'senderId': 1, 'receiverId': 1}", unique = true)
public class FriendRequest {
  @Id String id;

  String senderId;
  String receiverId;

  @CreatedDate Instant createdAt;
}
