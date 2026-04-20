package com.chefkix.identity.entity;

import java.time.OffsetDateTime;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "invalidated_token")
public class InvalidatedToken {

  @Id String id;

  @Indexed(expireAfterSeconds = 0)
  @Field("expiry_time")
  Date expiryTime;

  @CreatedDate OffsetDateTime createdAt;
}
