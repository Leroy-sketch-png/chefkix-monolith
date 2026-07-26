package com.chefkix.identity.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.identity.dto.response.BlockResponse;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.UserMentionResponse;
import com.chefkix.identity.service.BlockService;
import com.chefkix.identity.service.SocialService;
import com.chefkix.identity.utils.SecurityUtils;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocialController {

  SocialService socialService;
  BlockService blockService;
  SecurityUtils securityUtils;


  @PostMapping("/toggle-follow/{followingId}")
  public ApiResponse<ProfileResponse> profileFollow(
      @PathVariable("followingId") String followingId, Authentication authentication) {
    return ApiResponse.success(socialService.toggleFollow(followingId, authentication));
  }

  /**
   */
  @GetMapping("/following")
  public ApiResponse<List<ProfileResponse>> getFollowing(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.getFollowingProfiles(userId));
  }

  /**
   */
  @GetMapping("/followers")
  public ApiResponse<List<ProfileResponse>> getFollowers(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.getFollowerProfiles(userId));
  }

  /**
   */
  @GetMapping("/friends")
  public ApiResponse<List<ProfileResponse>> getFriends(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.getFriendProfiles(userId));
  }

  @GetMapping("/is-mutual/{targetUserId}")
  public ApiResponse<Boolean> isMutualFollow(
      @PathVariable("targetUserId") String targetUserId, Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.isMutualFollow(userId, targetUserId));
  }

  @GetMapping("/suggested")
  public ApiResponse<List<ProfileResponse>> getSuggestedFollows(
      @RequestParam(defaultValue = "10") int limit, Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.getSuggestedFollows(userId, Math.min(limit, 30)));
  }


  /**
   */
  @PostMapping("/block/{userId}")
  public ApiResponse<BlockResponse> blockUser(
      @PathVariable("userId") String userId, Authentication authentication) {
    return ApiResponse.success(
        blockService.blockUser(userId, authentication), "User blocked successfully");
  }

  @DeleteMapping("/block/{userId}")
  public ApiResponse<Void> unblockUser(
      @PathVariable("userId") String userId, Authentication authentication) {
    blockService.unblockUser(userId, authentication);
    return ApiResponse.success(null, "User unblocked successfully");
  }

  @GetMapping("/blocked-users")
  public ApiResponse<List<BlockResponse>> getBlockedUsers(Authentication authentication) {
    return ApiResponse.success(blockService.getBlockedUsers(authentication));
  }

  @GetMapping("/is-blocked/{targetUserId}")
  public ApiResponse<Boolean> isBlocked(
      @PathVariable("targetUserId") String targetUserId, Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(blockService.hasBlocked(userId, targetUserId));
  }


  /**
   */
  @GetMapping("/friends/search-mention")
  public ApiResponse<List<UserMentionResponse>> searchMention(
      @RequestParam("keyword") String keyword,
      @org.springframework.data.web.PageableDefault(size = 10) Pageable pageable) {
    String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    var result = socialService.searchFriendsForMention(currentUserId, keyword, pageable);
    return ApiResponse.success(result);
  }
}
