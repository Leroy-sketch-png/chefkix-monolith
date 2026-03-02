package com.chefkix.identity.service;

import com.chefkix.identity.dto.response.BlockResponse;
import com.chefkix.identity.entity.Block;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.repository.BlockRepository;
import com.chefkix.identity.repository.FollowRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.utils.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user blocks. When a user blocks another: - Any follow relationships are
 * removed (both directions) - The blocked user cannot see the blocker's content - The blocker
 * cannot see the blocked user's content (mutual invisibility)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlockService {

  BlockRepository blockRepository;
  FollowRepository followRepository;
  UserProfileRepository userProfileRepository;
  SecurityUtils securityUtils;

  /** Block a user. Also removes any follow relationships between the two users. */
  @Transactional
  public BlockResponse blockUser(String blockedUserId, Authentication authentication) {
    String blockerId = securityUtils.getCurrentUserId(authentication);

    // Validate: can't block yourself
    if (blockerId.equals(blockedUserId)) {
      throw new AppException(ErrorCode.INVALID_OPERATION);
    }

    // Check if target user exists
    UserProfile blockedProfile =
        userProfileRepository
            .findByUserId(blockedUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    // Check if already blocked
    if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
      throw new AppException(ErrorCode.ALREADY_BLOCKED);
    }

    // Remove any follow relationships (both directions)
    followRepository
        .findByFollowerIdAndFollowingId(blockerId, blockedUserId)
        .ifPresent(
            follow -> {
              followRepository.delete(follow);
              log.info("Removed follow: {} -> {} due to block", blockerId, blockedUserId);
            });
    followRepository
        .findByFollowerIdAndFollowingId(blockedUserId, blockerId)
        .ifPresent(
            follow -> {
              followRepository.delete(follow);
              log.info("Removed follow: {} -> {} due to block", blockedUserId, blockerId);
            });

    // Create the block
    Block block =
        Block.builder()
            .blockerId(blockerId)
            .blockedId(blockedUserId)
            .createdAt(Instant.now())
            .build();
    block = blockRepository.save(block);

    log.info("User {} blocked user {}", blockerId, blockedUserId);

    return buildBlockResponse(block, blockedProfile);
  }

  /** Unblock a user. */
  @Transactional
  public void unblockUser(String blockedUserId, Authentication authentication) {
    String blockerId = securityUtils.getCurrentUserId(authentication);

    Block block =
        blockRepository
            .findByBlockerIdAndBlockedId(blockerId, blockedUserId)
            .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));

    blockRepository.delete(block);
    log.info("User {} unblocked user {}", blockerId, blockedUserId);
  }

  /** Get list of users the current user has blocked. */
  public List<BlockResponse> getBlockedUsers(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);

    List<Block> blocks = blockRepository.findAllByBlockerId(userId);

    return blocks.stream()
        .map(
            block -> {
              UserProfile profile =
                  userProfileRepository.findByUserId(block.getBlockedId()).orElse(null);
              return buildBlockResponse(block, profile);
            })
        .collect(Collectors.toList());
  }

  /**
   * Check if there's a block between two users (either direction). Used by other services to filter
   * content.
   */
  public boolean isBlocked(String userId1, String userId2) {
    return blockRepository.existsBlockBetween(userId1, userId2);
  }

  /** Check if currentUser has blocked targetUser. */
  public boolean hasBlocked(String blockerId, String blockedId) {
    return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
  }

  /**
   * Get all user IDs that should be invisible to the given user. This includes users they've
   * blocked AND users who blocked them.
   */
  public List<String> getInvisibleUserIds(String userId) {
    List<Block> blockedByMe = blockRepository.findAllByBlockerId(userId);
    List<Block> blockedMe = blockRepository.findAllByBlockedId(userId);

    List<String> invisibleIds = new java.util.ArrayList<>();
    blockedByMe.forEach(b -> invisibleIds.add(b.getBlockedId()));
    blockedMe.forEach(b -> invisibleIds.add(b.getBlockerId()));

    return invisibleIds;
  }

  private BlockResponse buildBlockResponse(Block block, UserProfile profile) {
    return BlockResponse.builder()
        .id(block.getId())
        .blockedUserId(block.getBlockedId())
        .blockedUsername(profile != null ? profile.getDisplayName() : "Deleted User")
        .blockedDisplayName(profile != null ? profile.getDisplayName() : "Deleted User")
        .blockedAvatarUrl(profile != null ? profile.getAvatarUrl() : null)
        .blockedAt(block.getCreatedAt())
        .build();
  }
}
