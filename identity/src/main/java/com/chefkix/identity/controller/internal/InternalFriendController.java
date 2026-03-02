package com.chefkix.identity.controller.internal;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.internal.InternalFriendListResponse;
import com.chefkix.identity.service.SocialService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// QUAN TRỌNG: Dùng prefix "/internal" để phân biệt với API public
@RequestMapping("/auth")
@RequiredArgsConstructor
public class InternalFriendController {

  private final SocialService socialService;

  /**
   * API Nội bộ: Lấy danh sách ID bạn bè của user. Dành cho: Recipe Service, Notification Service.
   * Không dành cho: Frontend/Mobile App.
   */
  @GetMapping("/{userId}/ids")
  public ApiResponse<InternalFriendListResponse> getFriendId(
      @PathVariable("userId") String userId) {
    // Gọi hàm service tối ưu mà chúng ta vừa viết
    InternalFriendListResponse response = socialService.getAllFriends(userId);

    return ApiResponse.success(response, "Successfully retrieved friends");
  }

  @GetMapping("/{userId}/friends")
  public List<String> getFriendIds(@PathVariable String userId) {
    return socialService.getFriendIds(userId);
  }
}
