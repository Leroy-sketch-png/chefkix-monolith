package com.chefkix.identity.dto.response.internal;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalFriendListResponse {
  private String userId;

  private List<String> friendIds;

  private int totalCount;
}
