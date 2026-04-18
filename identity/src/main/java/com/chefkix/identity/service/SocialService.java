package com.chefkix.identity.service;

import com.chefkix.shared.event.NewFollowerEvent;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.UserMentionResponse;
import com.chefkix.identity.dto.response.internal.InternalFriendListResponse;
import com.chefkix.identity.entity.Follow;
import com.chefkix.identity.entity.FriendRequest;
import com.chefkix.identity.entity.Friendship;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.enums.RelationshipStatus;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.BlockRepository;
import com.chefkix.identity.repository.FollowRepository;
import com.chefkix.identity.repository.FriendRequestRepository;
import com.chefkix.identity.repository.FriendshipRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.utils.SecurityUtils;
import com.chefkix.identity.utils.SocialUtils;
import com.mongodb.client.result.UpdateResult;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocialService {

  FollowRepository followRepository;
  FriendRequestRepository friendRequestRepository;
  UserProfileRepository userProfileRepository;
  FriendshipRepository friendshipRepository;
  BlockRepository blockRepository;
  ProfileMapper profileMapper;
  MongoTemplate mongoTemplate;
  SecurityUtils securityUtils;
  SocialUtils socialUtils;
  StatisticsService statisticsService;
  SettingsService settingsService;
  KafkaTemplate<String, Object> kafkaTemplate;

  private static final String NEW_FOLLOWER_TOPIC = "new-follower-delivery";

  // ===================================================================================
  // --- Follow Logic ---
  // ===================================================================================

  /**
   * Perform follow or unfollow action (toggle).
   *
   * @return ProfileResponse of the FOLLOWED user (with updated isFollowing status).
   */
  @Transactional
  public ProfileResponse toggleFollow(String followingId, Authentication authentication) {
    String followerId = securityUtils.getCurrentUserId(authentication);

    // --- VALIDATION ---
    if (followerId.equals(followingId)) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    if (blockRepository.existsBlockBetween(followerId, followingId)) {
      throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
    }
    userProfileRepository
        .findByUserId(followingId)
        .orElseThrow(
            () -> new AppException(ErrorCode.USER_NOT_FOUND));

    // Get follower profile for notification
    UserProfile followerProfile = userProfileRepository.findByUserId(followerId).orElse(null);

    // --- TOGGLE LOGIC ---
    Optional<Follow> existingFollow =
        followRepository.findByFollowerIdAndFollowingId(followerId, followingId);

    if (existingFollow.isPresent()) {
      // A. ALREADY FOLLOWING -> UNFOLLOW
      followRepository.delete(existingFollow.get());
      updateFollowCounts(followerId, followingId, -1);
      log.info("User {} successfully UNFOLLOWED {}", followerId, followingId);
    } else {
      // B. NOT FOLLOWING -> FOLLOW
      // PRIVACY: Check if target user allows followers
      var targetPrivacy = settingsService.getPrivacySettingsByUserId(followingId);
      if (targetPrivacy != null && Boolean.FALSE.equals(targetPrivacy.getAllowFollowers())) {
        throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
      }
      Follow follow = Follow.builder().followerId(followerId).followingId(followingId).build();
      followRepository.save(follow);
      updateFollowCounts(followerId, followingId, 1);
      log.info("User {} successfully FOLLOWED {}", followerId, followingId);

      // Send notification for new follower
      sendNewFollowerNotification(followerId, followerProfile, followingId);
    }

    // --- MAP & RETURN ---
    // Fetch latest profile to get accurate counts
    UserProfile updatedTargetProfile = userProfileRepository.findByUserId(followingId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    ProfileResponse response = profileMapper.toProfileResponse(updatedTargetProfile);

    // Set the isFollowing status - CRITICAL: FE depends on this!
    boolean nowFollowing = followRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent();
    response.setFollowing(nowFollowing);
    
    // Also set followedBy for mutual detection
    boolean theyFollowMe = followRepository.findByFollowerIdAndFollowingId(followingId, followerId).isPresent();
    response.setFollowedBy(theyFollowMe);
    
    // Set relationship status
    response.setRelationshipStatus(socialUtils.determineRelationshipStatus(followerId, updatedTargetProfile));
    
    return response;
  }

  /** Send Kafka notification when someone gets a new follower. */
  private void sendNewFollowerNotification(
      String followerId, UserProfile followerProfile, String followedUserId) {
    try {
      // Check if this creates a mutual follow
      boolean isMutual =
          followRepository.findByFollowerIdAndFollowingId(followedUserId, followerId).isPresent();

      NewFollowerEvent event =
          NewFollowerEvent.builder()
              .followerId(followerId)
              .followerDisplayName(getProfileDisplayName(followerProfile))
              .followerAvatarUrl(followerProfile != null ? followerProfile.getAvatarUrl() : null)
              .followedUserId(followedUserId)
              .isMutualFollow(isMutual)
              .build();

      kafkaTemplate.send(NEW_FOLLOWER_TOPIC, event);
      log.info("Sent new follower notification: {} → {}", followerId, followedUserId);
    } catch (Exception e) {
      // Fire and forget - don't fail the follow operation
      log.warn("Failed to send new follower notification", e);
    }
  }

  /**
   * Get a robust display name for a user profile.
   * Fallback chain: displayName → firstName + lastName → username → "A user"
   * CRITICAL: displayName is OPTIONAL (users sign up with firstName/lastName/username, NOT displayName)
   */
  private String getProfileDisplayName(UserProfile profile) {
    if (profile == null) {
      return "A user";
    }

    // Try displayName first
    String displayName = profile.getDisplayName();
    if (displayName != null && !displayName.isBlank()) {
      return displayName;
    }

    // Fallback to firstName + lastName
    String firstName = profile.getFirstName();
    String lastName = profile.getLastName();
    if (firstName != null && !firstName.isBlank()) {
      return (lastName != null && !lastName.isBlank()) 
          ? firstName + " " + lastName 
          : firstName;
    }
    if (lastName != null && !lastName.isBlank()) {
      return lastName;
    }

    // Fallback to username
    String username = profile.getUsername();
    if (username != null && !username.isBlank()) {
      return username;
    }

    return "A user";
  }

  // ===================================================================================
  // --- Friend Request Logic ---
  // ===================================================================================

  /**
   * Send or CANCEL a friend request (toggle).
   *
   * @return ProfileResponse of the RECEIVER (with updated relationshipStatus).
   */
  @Transactional
  public ProfileResponse toggleSendFriendRequest(String receiverId, Authentication authentication) {
    String senderId = securityUtils.getCurrentUserId(authentication);

    // --- VALIDATION ---
    if (senderId.equals(receiverId)) {
      throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
    }
    if (blockRepository.existsBlockBetween(senderId, receiverId)) {
      throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
    }
    UserProfile targetProfile =
        userProfileRepository
            .findByUserId(receiverId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    List<Friendship> friends =
        (targetProfile.getFriends() != null) ? targetProfile.getFriends() : Collections.emptyList();

    boolean isAlreadyFriends =
        friends.stream()
            .anyMatch(f -> f.getFriendId().equals(senderId)); // FIX: friendId -> userId
    if (isAlreadyFriends) {
      throw new AppException(ErrorCode.ALREADY_FRIEND);
    }

    // --- TOGGLE LOGIC ---
    Optional<FriendRequest> existingRequest =
        friendRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId);

    if (existingRequest.isPresent()) {
      // A. ALREADY SENT -> CANCEL REQUEST
      friendRequestRepository.delete(existingRequest.get());
      updateFriendRequestCounts(receiverId, -1);
      log.info("User {} CANCELLED friend request to {}", senderId, receiverId);
    } else {
      // B. NOT SENT -> SEND REQUEST
      FriendRequest request =
          FriendRequest.builder().senderId(senderId).receiverId(receiverId).build();
      friendRequestRepository.save(request);
      updateFriendRequestCounts(receiverId, 1);
      log.info("User {} SENT friend request to {}", senderId, receiverId);
    }

    ProfileResponse response = profileMapper.toProfileResponse(targetProfile);

    response.setRelationshipStatus(
        socialUtils.determineRelationshipStatus(senderId, targetProfile));
    return response;
  }

  // This method assumes it lives in SocialService, where
  // MongoTemplate, UserProfileRepository, FriendRequestRepository,
  // StatisticsService, ProfileMapper, and SecurityUtils are injected

  @Transactional
  public ProfileResponse acceptFriendRequest(String senderId, Authentication authentication) {
    String currentUserId = securityUtils.getCurrentUserId(authentication); // This is YOU (Receiver)

    // Block check: cannot accept friendship if either user has blocked the other
    if (blockRepository.existsBlockBetween(senderId, currentUserId)) {
      throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
    }

    // 1. Find the friend request (existing code is correct)
    FriendRequest friendRequest =
        friendRequestRepository
            .findBySenderIdAndReceiverId(senderId, currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

    // 2. Create Friendship object (using a shared timestamp)
    Instant friendedAt = Instant.now();

    // "New friend" object for YOU (Receiver)
    Friendship friendshipForReceiver =
        Friendship.builder()
            .friendId(senderId) // ID of the sender
            .friendedAt(friendedAt)
            .build();

    // "New friend" object for THEM (Sender)
    Friendship friendshipForSender =
        Friendship.builder()
            .friendId(currentUserId) // ID of you (the receiver)
            .friendedAt(friendedAt)
            .build();

    // 3. Update friends (using $push for atomicity)
    // Add Sender (A) to Receiver's (B - You) friend list
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("userId").is(currentUserId)),
        new Update().addToSet("friends", friendshipForReceiver),
        UserProfile.class);

    // Add Receiver (B) to Sender's (A) friend list
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("userId").is(senderId)),
        new Update().addToSet("friends", friendshipForSender),
        UserProfile.class);

    friendRequestRepository.delete(friendRequest);

    statisticsService.incrementCounter(currentUserId, "friendRequestCount", -1);
    statisticsService.incrementCounter(currentUserId, "friendCount", 1);
    statisticsService.incrementCounter(senderId, "friendCount", 1);

    UserProfile updatedSenderProfile =
        userProfileRepository
            .findByUserId(senderId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    ProfileResponse response = profileMapper.toProfileResponse(updatedSenderProfile);

    response.setRelationshipStatus(RelationshipStatus.FRIENDS);

    return response;
  }

  @Transactional
  public ProfileResponse rejectFriendRequest(String senderId, Authentication authentication) {
    String currentUserId = securityUtils.getCurrentUserId(authentication);

    FriendRequest friendRequest =
        friendRequestRepository
            .findBySenderIdAndReceiverId(senderId, currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

    friendRequestRepository.delete(friendRequest);

    // Only decrement the pending request count — do NOT increment friendCount
    // (this was a copy-paste bug from acceptFriendRequest)
    statisticsService.incrementCounter(currentUserId, "friendRequestCount", -1);

    UserProfile updatedSenderProfile =
        userProfileRepository
            .findByUserId(senderId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    ProfileResponse response = profileMapper.toProfileResponse(updatedSenderProfile);

    // After rejection, relationship goes back to NONE (not FRIENDS)
    response.setRelationshipStatus(
        socialUtils.determineRelationshipStatus(currentUserId, updatedSenderProfile));

    return response;
  }

  @Transactional
  public ProfileResponse unfriend(String friendId, Authentication authentication) {
    String currentUserId = securityUtils.getCurrentUserId(authentication);

    if (currentUserId.equals(friendId)) {
      throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
    }

    // --- UNFRIEND LOGIC ---
    Query pullQueryForFriend = Query.query(Criteria.where("friendId").is(friendId));

    UpdateResult result =
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").is(currentUserId)),
            new Update().pull("friends", pullQueryForFriend),
            UserProfile.class);

    if (result.getModifiedCount() == 0) {
      log.warn("User {} tried to unfriend {}, but they were not friends.", currentUserId, friendId);
      throw new AppException(ErrorCode.NOT_FRIEND);
    }

    Query pullQueryForCurrent = Query.query(Criteria.where("friendId").is(currentUserId));

    mongoTemplate.updateFirst(
        Query.query(Criteria.where("userId").is(friendId)), // Find UserProfile (correct)
        new Update().pull("friends", pullQueryForCurrent), // Pull nested object (fixed)
        UserProfile.class);

    statisticsService.incrementCounter(currentUserId, "friendCount", -1);
    statisticsService.incrementCounter(friendId, "friendCount", -1);

    log.info("User {} unfriended user {}", currentUserId, friendId);

    UserProfile updatedFriendProfile =
        userProfileRepository
            .findByUserId(friendId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    ProfileResponse response = profileMapper.toProfileResponse(updatedFriendProfile);

    response.setRelationshipStatus(RelationshipStatus.NOT_FRIENDS);
    response.setFollowing(
        followRepository.existsByFollowerIdAndFollowingId(currentUserId, friendId));

    return response;
  }

  public InternalFriendListResponse getAllFriends(String userId) {
    // 1. Call Repository to get LIST of STRING IDs
    // Note: Must filter status as ACCEPTED (already friends)
    List<String> friendIds =
        friendshipRepository.findAllFriendIdsByUserId(
            userId, RelationshipStatus.FRIENDS.toString());

    // 2. Null-safe handling (avoid error if repo returns null)
    if (friendIds == null) {
      friendIds = new ArrayList<>();
    }

    // 3. Wrap in Internal DTO
    return InternalFriendListResponse.builder()
        .userId(userId)
        .friendIds(friendIds)
        .totalCount(friendIds.size())
        .build();
  }

  // ===================================================================================
  // --- Private Helper Methods ---
  // ===================================================================================

  /** Update follower/following counts via StatisticsService. */
  private void updateFollowCounts(String followerId, String followingId, int amount) {
    statisticsService.incrementCounter(followerId, "followingCount", amount);
    statisticsService.incrementCounter(followingId, "followerCount", amount);
  }

  private void updateFriendRequestCounts(String receiverId, int amount) {
    statisticsService.incrementCounter(receiverId, "friendRequestCount", amount);
  }

  public List<String> getFriendIds(String userId) {
    // NEW APPROACH: Friends = Mutual Followers
    // Get people I follow
    List<String> followingIds =
        followRepository.findAllByFollowerId(userId).stream().map(Follow::getFollowingId).toList();

    // Get people who follow me
    List<String> followerIds =
        followRepository.findAllByFollowingId(userId).stream().map(Follow::getFollowerId).toList();

    // Mutual = intersection
    return followingIds.stream().filter(followerIds::contains).toList();
  }

  /** Get list of friend when being mentioned in comments */
  public List<UserMentionResponse> searchFriendsForMention(
      String currentUserId, String keyword, Pageable pageable) {
    // 1. Get profile of the user typing the comment
    UserProfile currentUser =
        userProfileRepository
            .findByUserId(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    // 2. Get friend ID list (only those ACCEPTED)
    if (currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) {
      return new ArrayList<>();
    }

    List<String> friendIds =
        currentUser.getFriends().stream()
            // .filter(f -> "ACCEPTED".equals(f.getStatus())) // If status check is needed
            .map(Friendship::getFriendId)
            .collect(Collectors.toList());

    // 3. Search the database for IDs matching the keyword
    // Limit to 10 results for fast autocomplete
    // Escape regex special chars to prevent ReDoS from user-controlled input
    String safeKeyword = Pattern.quote(keyword);
    List<UserProfile> matchedFriends =
        friendshipRepository.findFriendsForMention(friendIds, safeKeyword, pageable);

    // 4. Map to lightweight DTO for Frontend
    return matchedFriends.stream()
        .map(u -> new UserMentionResponse(u.getUserId(), u.getDisplayName(), u.getAvatarUrl()))
        .collect(Collectors.toList());
  }

  // ===================================================================================
  // --- NEW: Mutual Follow = Friends System ---
  // ===================================================================================

  /** Check if two users mutually follow each other (i.e., are "friends"). */
  public boolean isMutualFollow(String userA, String userB) {
    boolean aFollowsB = followRepository.existsByFollowerIdAndFollowingId(userA, userB);
    boolean bFollowsA = followRepository.existsByFollowerIdAndFollowingId(userB, userA);
    return aFollowsB && bFollowsA;
  }

  /**
   * Get list of mutual followers (friends) for a user. Friends = people who both follow each other.
   */
  public List<String> getMutualFollowerIds(String userId) {
    // Get people I follow
    List<String> followingIds =
        followRepository.findAllByFollowerId(userId).stream().map(Follow::getFollowingId).toList();

    // Get people who follow me
    List<String> followerIds =
        followRepository.findAllByFollowingId(userId).stream().map(Follow::getFollowerId).toList();

    // Mutual = intersection
    return followingIds.stream().filter(followerIds::contains).toList();
  }

  /** Get list of users that userId is following. */
  public List<String> getFollowingIds(String userId) {
    return followRepository.findAllByFollowerId(userId).stream()
        .map(Follow::getFollowingId)
        .toList();
  }

  /** Get list of users that follow userId. */
  public List<String> getFollowerIds(String userId) {
    return followRepository.findAllByFollowingId(userId).stream()
        .map(Follow::getFollowerId)
        .toList();
  }

  // ===================================================================================
  // --- Profile-returning variants (for FE API) ---
  // ===================================================================================

  /**
   * Get profiles of users that the current user is following. Returns full ProfileResponse objects
   * for FE consumption.
   */
  public List<ProfileResponse> getFollowingProfiles(String userId) {
    List<String> followingIds = getFollowingIds(userId);
    return userProfileRepository.findAllByUserIdIn(followingIds).stream()
        .map(
            profile -> {
              ProfileResponse response = profileMapper.toProfileResponse(profile);
              response.setRelationshipStatus(
                  isMutualFollow(userId, profile.getUserId())
                      ? RelationshipStatus.FRIENDS
                      : RelationshipStatus.NOT_FRIENDS);
              response.setFollowing(true); // Current user follows them
              response.setFollowedBy(isMutualFollow(userId, profile.getUserId()));
              return response;
            })
        .toList();
  }

  /**
   * Get profiles of users that follow the current user. Returns full ProfileResponse objects for FE
   * consumption.
   */
  public List<ProfileResponse> getFollowerProfiles(String userId) {
    List<String> followerIds = getFollowerIds(userId);
    return userProfileRepository.findAllByUserIdIn(followerIds).stream()
        .map(
            profile -> {
              ProfileResponse response = profileMapper.toProfileResponse(profile);
              boolean isFollowingThem = isFollowing(userId, profile.getUserId());
              response.setRelationshipStatus(
                  isFollowingThem ? RelationshipStatus.FRIENDS : RelationshipStatus.NOT_FRIENDS);
              response.setFollowing(isFollowingThem);
              response.setFollowedBy(true); // They follow current user
              return response;
            })
        .toList();
  }

  /**
   * Get profiles of mutual followers (friends). Friends = users who mutually follow each other.
   * Returns full ProfileResponse objects for FE consumption.
   */
  public List<ProfileResponse> getFriendProfiles(String userId) {
    List<String> friendIds = getMutualFollowerIds(userId);
    return userProfileRepository.findAllByUserIdIn(friendIds).stream()
        .map(
            profile -> {
              ProfileResponse response = profileMapper.toProfileResponse(profile);
              response.setRelationshipStatus(RelationshipStatus.FRIENDS);
              response.setFollowing(true);
              response.setFollowedBy(true);
              return response;
            })
        .toList();
  }

  /** Check if userId is following targetId. */
  public boolean isFollowing(String userId, String targetId) {
    return followRepository.findByFollowerIdAndFollowingId(userId, targetId).isPresent();
  }

  /**
   * Get suggested follow profiles for a user based on preference overlap and popularity.
   * Excludes: self, already following, blocked users.
   * Scoring: preference overlap (0.4) + follower count (0.2) + post count (0.2) + level (0.2).
   */
  public List<ProfileResponse> getSuggestedFollows(String userId, int limit) {
    // Gather exclusion sets
    Set<String> followingIds = new HashSet<>(getFollowingIds(userId));
    followingIds.add(userId); // Exclude self

    // Exclude blocked users (both directions)
    blockRepository.findAllByBlockerId(userId).forEach(b -> followingIds.add(b.getBlockedId()));
    blockRepository.findAllByBlockedId(userId).forEach(b -> followingIds.add(b.getBlockerId()));

    // Get current user's preferences for matching
    UserProfile currentProfile = userProfileRepository.findByUserId(userId).orElse(null);
    Set<String> myPrefs = (currentProfile != null && currentProfile.getPreferences() != null)
            ? new HashSet<>(currentProfile.getPreferences()) : Collections.emptySet();

    // Fetch candidate pool: recent active users not in exclusion set
    // Use Pageable to limit the candidate pool size
    Query query = new Query()
            .addCriteria(Criteria.where("userId").nin(followingIds))
            .with(org.springframework.data.domain.PageRequest.of(0, 200,
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt")));
    List<UserProfile> candidates = mongoTemplate.find(query, UserProfile.class);

    if (candidates.isEmpty()) {
      return Collections.emptyList();
    }

    // Score and rank
    List<ScoredProfile> scored = candidates.stream()
            .map(profile -> {
              double score = 0.0;
              var stats = profile.getStatistics();

              // Preference overlap (0.4) — shared interests
              if (!myPrefs.isEmpty() && profile.getPreferences() != null) {
                long overlap = profile.getPreferences().stream().filter(myPrefs::contains).count();
                score += 0.4 * ((double) overlap / myPrefs.size());
              }

              if (stats != null) {
                // Follower count (0.2) — popularity, normalized log scale
                long followers = stats.getFollowerCount() != null ? stats.getFollowerCount() : 0;
                score += 0.2 * Math.min(1.0, Math.log1p(followers) / Math.log1p(1000));

                // Post count (0.2) — content creator signal
                long posts = stats.getPostCount() != null ? stats.getPostCount() : 0;
                score += 0.2 * Math.min(1.0, Math.log1p(posts) / Math.log1p(50));

                // Level (0.2) — engagement signal
                int level = stats.getCurrentLevel() != null ? stats.getCurrentLevel() : 1;
                score += 0.2 * Math.min(1.0, (double) level / 20);
              }

              return new ScoredProfile(profile, score);
            })
            .sorted(Comparator.comparingDouble(ScoredProfile::score).reversed())
            .limit(limit)
            .toList();

    return scored.stream()
            .map(sp -> {
              ProfileResponse response = profileMapper.toProfileResponse(sp.profile());
              response.setFollowing(false);
              response.setFollowedBy(isFollowing(sp.profile().getUserId(), userId));
              response.setRelationshipStatus(
                      response.getFollowedBy() != null && response.getFollowedBy() ? RelationshipStatus.NOT_FRIENDS : RelationshipStatus.NOT_FRIENDS);
              return response;
            })
            .toList();
  }

  private record ScoredProfile(UserProfile profile, double score) {}
}
