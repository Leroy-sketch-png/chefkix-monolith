package com.chefkix.identity.dto.response;

import com.chefkix.social.api.dto.PostSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileWithPostsResponse {

  private ProfileResponse profile;

  private Page<PostSummary> posts;
}
