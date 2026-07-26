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
import org.springframework.dao.DuplicateKeyException;
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


  /**
   *
   */
  @Transactional
  public ProfileResponse toggleFollow(String followingId, Authentication authentication) {
    String followerId = securityUtils.getCurrentUserId(authentication);

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

    UserProfile followerProfile = userProfileRepository.findByUserId(followerId).orElse(null);

    Optional<Follow> existingFollow =
        followRepository.findByFollowerIdAndFollowingId(followerId, followingId);

    if (existingFollow.isPresent()) {
      followRepository.delete(existingFollow.get());
      updateFollowCounts(followerId, followingId, -1);
      log.info("User {} successfully UNFOLLOWED {}", followerId, followingId);
    } else {
      var targetPrivacy = settingsService.getPrivacySettingsByUserId(followingId);
      if (targetPrivacy != null && Boolean.FALSE.equals(targetPrivacy.getAllowFollowers())) {
        throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
      }
      Follow follow = Follow.builder().followerId(followerId).followingId(followingId).build();
      try {
        followRepository.save(follow);
        updateFollowCounts(followerId, followingId, 1);
        log.info("User {} successfully FOLLOWED {}", followerId, followingId);

        sendNewFollowerNotification(followerId, followerProfile, followingId);
      } catch (DuplicateKeyException e) {
        log.debug("Duplicate follow ignored for {} -> {}", followerId, followingId);
      }
    }

    UserProfile updatedTargetProfile = userProfileRepository.findByUserId(followingId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    ProfileResponse response = profileMapper.toProfileResponse(updatedTargetProfile);

    boolean nowFollowing = followRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent();
    response.setFollowing(nowFollowing);
    
    boolean theyFollowMe = followRepository.findByFollowerIdAndFollowingId(followingId, followerId).isPresent();
    response.setFollowedBy(theyFollowMe);
    
    response.setRelationshipStatus(socialUtils.determineRelationshipStatus(followerId, updatedTargetProfile));
    
    return response;
  }

  private void sendNewFollowerNotification(
      String followerId, UserProfile followerProfile, String followedUserId) {
    try {
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
      log.warn("Failed to send new follower notification", e);
    }
  }

  /**
   */
  private String getProfileDisplayName(UserProfile profile) {
    if (profile == null) {
      return "A user";
    }

    String displayName = profile.getDisplayName();
    if (displayName != null && !displayName.isBlank()) {
      return displayName;
    }

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

    String username = profile.getUsername();
    if (username != null && !username.isBlank()) {
      return username;
    }

    return "A user";
  }


  /**
   *
   */
  @Transactional
  public ProfileResponse toggleSendFriendRequest(String receiverId, Authentication authentication) {
    String senderId = securityUtils.getCurrentUserId(authentication);

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
.anyMatch(f -> f.getFriendId().equals(senderId));
    if (isAlreadyFriends) {
      throw new AppException(ErrorCode.ALREADY_FRIEND);
    }

    Optional<FriendRequest> existingRequest =
        friendRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId);

    if (existingRequest.isPresent()) {
      friendRequestRepository.delete(existingRequest.get());
      updateFriendRequestCounts(receiverId, -1);
      log.info("User {} CANCELLED friend request to {}", senderId, receiverId);
    } else {
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


  @Transactional
  public ProfileResponse acceptFriendRequest(String senderId, Authentication authentication) {
String currentUserId = securityUtils.getCurrentUserId(authentication);

    if (blockRepository.existsBlockBetween(senderId, currentUserId)) {
      throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
    }

    FriendRequest friendRequest =
        friendRequestRepository
            .findBySenderIdAndReceiverId(senderId, currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

    Instant friendedAt = Instant.now();

    Friendship friendshipForReceiver =
        Friendship.builder()
.friendId(senderId)
            .friendedAt(friendedAt)
            .build();

    Friendship friendshipForSender =
        Friendship.builder()
.friendId(currentUserId)
            .friendedAt(friendedAt)
            .build();

    mongoTemplate.updateFirst(
        Query.query(Criteria.where("userId").is(currentUserId)),
        new Update().addToSet("friends", friendshipForReceiver),
        UserProfile.class);

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

    statisticsService.incrementCounter(currentUserId, "friendRequestCount", -1);

    UserProfile updatedSenderProfile =
        userProfileRepository
            .findByUserId(senderId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    ProfileResponse response = profileMapper.toProfileResponse(updatedSenderProfile);

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
Query.query(Criteria.where("userId").is(friendId)),
new Update().pull("friends", pullQueryForCurrent),
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
    List<String> friendIds =
        friendshipRepository.findAllFriendIdsByUserId(
            userId, RelationshipStatus.FRIENDS.toString());

    if (friendIds == null) {
      friendIds = new ArrayList<>();
    }

    return InternalFriendListResponse.builder()
        .userId(userId)
        .friendIds(friendIds)
        .totalCount(friendIds.size())
        .build();
  }


  private void updateFollowCounts(String followerId, String followingId, int amount) {
    statisticsService.incrementCounter(followerId, "followingCount", amount);
    statisticsService.incrementCounter(followingId, "followerCount", amount);
  }

  private void updateFriendRequestCounts(String receiverId, int amount) {
    statisticsService.incrementCounter(receiverId, "friendRequestCount", amount);
  }

  public List<String> getFriendIds(String userId) {
    List<String> followingIds =
        followRepository.findAllByFollowerId(userId).stream().map(Follow::getFollowingId).toList();

    List<String> followerIds =
        followRepository.findAllByFollowingId(userId).stream().map(Follow::getFollowerId).toList();

    return followingIds.stream().filter(followerIds::contains).toList();
  }

  public List<UserMentionResponse> searchFriendsForMention(
      String currentUserId, String keyword, Pageable pageable) {
    UserProfile currentUser =
        userProfileRepository
            .findByUserId(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    if (currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) {
      return new ArrayList<>();
    }

    List<String> friendIds =
        currentUser.getFriends().stream()
            .map(Friendship::getFriendId)
            .collect(Collectors.toList());

    String safeKeyword = Pattern.quote(keyword);
    List<UserProfile> matchedFriends =
        friendshipRepository.findFriendsForMention(friendIds, safeKeyword, pageable);

    return matchedFriends.stream()
        .map(u -> new UserMentionResponse(u.getUserId(), u.getDisplayName(), u.getAvatarUrl()))
        .collect(Collectors.toList());
  }


  public boolean isMutualFollow(String userA, String userB) {
    boolean aFollowsB = followRepository.existsByFollowerIdAndFollowingId(userA, userB);
    boolean bFollowsA = followRepository.existsByFollowerIdAndFollowingId(userB, userA);
    return aFollowsB && bFollowsA;
  }

  /**
   */
  public List<String> getMutualFollowerIds(String userId) {
    List<String> followingIds =
        followRepository.findAllByFollowerId(userId).stream().map(Follow::getFollowingId).toList();

    List<String> followerIds =
        followRepository.findAllByFollowingId(userId).stream().map(Follow::getFollowerId).toList();

    return followingIds.stream().filter(followerIds::contains).toList();
  }

  public List<String> getFollowingIds(String userId) {
    return followRepository.findAllByFollowerId(userId).stream()
        .map(Follow::getFollowingId)
        .toList();
  }

  public List<String> getFollowerIds(String userId) {
    return followRepository.findAllByFollowingId(userId).stream()
        .map(Follow::getFollowerId)
        .toList();
  }


  /**
   */
  public List<ProfileResponse> getFollowingProfiles(String userId) {
    List<String> followingIds = getFollowingIds(userId);
    if (followingIds.isEmpty()) return List.of();
    Set<String> theyFollowMeBack = followRepository
        .findAllByFollowingIdAndFollowerIdIn(userId, followingIds).stream()
        .map(Follow::getFollowerId)
        .collect(Collectors.toSet());
    return userProfileRepository.findAllByUserIdIn(followingIds).stream()
        .map(
            profile -> {
              ProfileResponse response = profileMapper.toProfileResponse(profile);
              boolean mutual = theyFollowMeBack.contains(profile.getUserId());
              response.setRelationshipStatus(
                  mutual ? RelationshipStatus.FRIENDS : RelationshipStatus.NOT_FRIENDS);
              response.setFollowing(true);
              response.setFollowedBy(mutual);
              return response;
            })
        .toList();
  }

  /**
   */
  public List<ProfileResponse> getFollowerProfiles(String userId) {
    List<String> followerIds = getFollowerIds(userId);
    if (followerIds.isEmpty()) return List.of();
    Set<String> iFollowBack = followRepository
        .findAllByFollowerIdAndFollowingIdIn(userId, followerIds).stream()
        .map(Follow::getFollowingId)
        .collect(Collectors.toSet());
    return userProfileRepository.findAllByUserIdIn(followerIds).stream()
        .map(
            profile -> {
              ProfileResponse response = profileMapper.toProfileResponse(profile);
              boolean isFollowingThem = iFollowBack.contains(profile.getUserId());
              response.setRelationshipStatus(
                  isFollowingThem ? RelationshipStatus.FRIENDS : RelationshipStatus.NOT_FRIENDS);
              response.setFollowing(isFollowingThem);
response.setFollowedBy(true);
              return response;
            })
        .toList();
  }

  /**
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

  public boolean isFollowing(String userId, String targetId) {
    return followRepository.findByFollowerIdAndFollowingId(userId, targetId).isPresent();
  }

  /**
   */
  public List<ProfileResponse> getSuggestedFollows(String userId, int limit) {
    Set<String> followingIds = new HashSet<>(getFollowingIds(userId));
followingIds.add(userId);

    blockRepository.findAllByBlockerId(userId).forEach(b -> followingIds.add(b.getBlockedId()));
    blockRepository.findAllByBlockedId(userId).forEach(b -> followingIds.add(b.getBlockerId()));

    UserProfile currentProfile = userProfileRepository.findByUserId(userId).orElse(null);
    Set<String> myPrefs = (currentProfile != null && currentProfile.getPreferences() != null)
            ? new HashSet<>(currentProfile.getPreferences()) : Collections.emptySet();

    Query query = new Query()
            .addCriteria(Criteria.where("userId").nin(followingIds))
            .with(org.springframework.data.domain.PageRequest.of(0, 200,
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt")));
    List<UserProfile> candidates = mongoTemplate.find(query, UserProfile.class);

    if (candidates.isEmpty()) {
      return Collections.emptyList();
    }

    List<ScoredProfile> scored = candidates.stream()
            .map(profile -> {
              double score = 0.0;
              var stats = profile.getStatistics();

              if (!myPrefs.isEmpty() && profile.getPreferences() != null) {
                long overlap = profile.getPreferences().stream().filter(myPrefs::contains).count();
                score += 0.4 * ((double) overlap / myPrefs.size());
              }

              if (stats != null) {
                long followers = stats.getFollowerCount() != null ? stats.getFollowerCount() : 0;
                score += 0.2 * Math.min(1.0, Math.log1p(followers) / Math.log1p(1000));

                long posts = stats.getPostCount() != null ? stats.getPostCount() : 0;
                score += 0.2 * Math.min(1.0, Math.log1p(posts) / Math.log1p(50));

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
