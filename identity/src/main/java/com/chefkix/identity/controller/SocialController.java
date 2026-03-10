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

  // ===================================================================================
  // --- CORE: Follow System (Instagram Model) ---
  // ===================================================================================

  /** Toggle follow/unfollow a user. Mutual follows = implicit friends. */
  @PostMapping("/toggle-follow/{followingId}")
  public ApiResponse<ProfileResponse> profileFollow(
      @PathVariable("followingId") String followingId, Authentication authentication) {
    return ApiResponse.success(socialService.toggleFollow(followingId, authentication));
  }

  /**
   * Get list of profiles that the current user is following. Returns full ProfileResponse objects
   * for FE.
   */
  @GetMapping("/following")
  public ApiResponse<List<ProfileResponse>> getFollowing(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.getFollowingProfiles(userId));
  }

  /**
   * Get list of profiles that follow the current user. Returns full ProfileResponse objects for FE.
   */
  @GetMapping("/followers")
  public ApiResponse<List<ProfileResponse>> getFollowers(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.getFollowerProfiles(userId));
  }

  /**
   * Get list of mutual followers (friends) for the current user. Friends = users who mutually
   * follow each other. Returns full ProfileResponse objects for FE.
   */
  @GetMapping("/friends")
  public ApiResponse<List<ProfileResponse>> getFriends(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.getFriendProfiles(userId));
  }

  /** Check if the current user and another user mutually follow each other. */
  @GetMapping("/is-mutual/{targetUserId}")
  public ApiResponse<Boolean> isMutualFollow(
      @PathVariable("targetUserId") String targetUserId, Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(socialService.isMutualFollow(userId, targetUserId));
  }

  // ===================================================================================
  // --- Block System (Safety Feature) ---
  // ===================================================================================

  /**
   * Block a user. This will: - Remove any follow relationships (both directions) - Hide content
   * between both users (mutual invisibility)
   */
  @PostMapping("/block/{userId}")
  public ApiResponse<BlockResponse> blockUser(
      @PathVariable("userId") String userId, Authentication authentication) {
    return ApiResponse.success(
        blockService.blockUser(userId, authentication), "User blocked successfully");
  }

  /** Unblock a user. */
  @DeleteMapping("/block/{userId}")
  public ApiResponse<Void> unblockUser(
      @PathVariable("userId") String userId, Authentication authentication) {
    blockService.unblockUser(userId, authentication);
    return ApiResponse.success(null, "User unblocked successfully");
  }

  /** Get list of users the current user has blocked. */
  @GetMapping("/blocked-users")
  public ApiResponse<List<BlockResponse>> getBlockedUsers(Authentication authentication) {
    return ApiResponse.success(blockService.getBlockedUsers(authentication));
  }

  /** Check if the current user has blocked a specific user. */
  @GetMapping("/is-blocked/{targetUserId}")
  public ApiResponse<Boolean> isBlocked(
      @PathVariable("targetUserId") String targetUserId, Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    return ApiResponse.success(blockService.hasBlocked(userId, targetUserId));
  }

  // Friend request endpoints REMOVED — use follow system. Mutual follows = friends.
  // Deleted in audit 2025-03: toggle-friend-request, accept-friend, reject-friend, unfriend
  // These created a parallel social graph alongside the follow system, causing data inconsistency.

  /**
   * Get a list of friend by text-change that like the keyword (display name) to mention them And
   * please use regex as [userId|displayName] to handle the tag friends properly
   */
  @GetMapping("/friends/search-mention")
  public ApiResponse<List<UserMentionResponse>> searchMention(
      @RequestParam String keyword, Pageable pageable) {
    String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    var result = socialService.searchFriendsForMention(currentUserId, keyword, pageable);
    return ApiResponse.success(result);
  }
}
