package com.chefkix.identity.service;

// Still imported, but not used directly
import com.chefkix.identity.dto.identity.Credential;
import com.chefkix.identity.dto.identity.ResetPasswordParam;
import com.chefkix.identity.dto.identity.TokenExchangeParam;
import com.chefkix.identity.dto.identity.UserCreationParam;
import com.chefkix.identity.dto.request.ProfileUpdateRequest;
import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.ProfileWithPostsResponse;
import com.chefkix.identity.dto.response.internal.InternalBasicProfileResponse;
import com.chefkix.identity.entity.Block;
import com.chefkix.identity.entity.Follow;
import com.chefkix.identity.entity.FriendRequest;
import com.chefkix.identity.entity.Friendship;
import com.chefkix.identity.entity.ResetPasswordRequest;
import com.chefkix.identity.entity.SignupRequest;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.events.UserIndexEvent;
import com.chefkix.identity.enums.RelationshipStatus;
import com.chefkix.shared.event.UserDeletedEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.identity.exception.ErrorNormalizer;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.*;
import com.chefkix.identity.client.KeycloakAdminClient;
import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.social.api.PostProvider;
import com.chefkix.social.api.dto.PostSummary;
import com.chefkix.identity.utils.SecurityUtils;
import com.chefkix.identity.utils.SocialUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
// Feign removed in monolith
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {

  // === Repositories & Mappers ===
  UserProfileRepository profileRepository;
  ProfileMapper profileMapper;
  SignupRequestRepository signupRequestRepository;
  ResetPasswordRepository resetPasswordRepository;
  FollowRepository followRepository;
  UserRepository userRepository;
  UserSettingsRepository userSettingsRepository;
  UserSubscriptionRepository userSubscriptionRepository;
  CreatorTipSettingsRepository creatorTipSettingsRepository;
  TipRepository tipRepository;
  ReferralCodeRepository referralCodeRepository;
  ReferralRedemptionRepository referralRedemptionRepository;
  VerificationRequestRepository verificationRequestRepository;
  UserEventRepository userEventRepository;
  FriendRequestRepository friendRequestRepository;
  BlockRepository blockRepository;
  UserActivityRepository userActivityRepository;

  // === External Module Providers ===
  KeycloakAdminClient keycloakAdminClient;
  @Lazy PostProvider postProvider;
  @Lazy RecipeProvider recipeProvider;

  // === Utilities & Services ===
  ErrorNormalizer errorNormalizer;
  EmailService emailService;
  BlockService blockService;
  StatisticsCounterOperations statisticsCounterOperations;
  SecurityUtils securityUtils;
  SocialUtils socialUtils;
  ApplicationEventPublisher eventPublisher;
  MongoTemplate mongoTemplate;

  // === Configuration ===
  @Qualifier("taskExecutor")
  Executor taskExecutor;

  @Value("${idp.client-id}")
  @NonFinal
  String clientId;

  @Value("${idp.client-secret}")
  @NonFinal
  String clientSecret;

  @NonFinal
  @Value("${app.otp.ttl-seconds:300}")
  private long otpTtlSeconds;

  @NonFinal
  @Value("${app.otp.max-attempts:5}")
  private int maxOtpAttempts;

  // ===================================================================
  // === PUBLIC API (For Controllers) ===
  // ===================================================================

  /**
   * @deprecated Use getProfilesPaginated() instead.
   */
  @Deprecated
  public List<ProfileResponse> getAllProfilesLimited() {
    var page = profileRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 100));
    return page.getContent().stream().map(profileMapper::toProfileResponse).toList();
  }

  /**
   * Get profiles with pagination and optional search.
   * 
   * @param pageable Spring Data pagination (page, size, sort)
   * @param search Optional search term for displayName/username
   * @return Paginated profiles
   */
  public org.springframework.data.domain.Page<ProfileResponse> getProfilesPaginated(
      org.springframework.data.domain.Pageable pageable, String search) {
    org.springframework.data.domain.Page<UserProfile> profilePage;
    
    if (search != null && !search.isBlank()) {
      // Search by displayName or username (case-insensitive)
      String searchLower = search.toLowerCase();
      profilePage = profileRepository.findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
          searchLower, searchLower, pageable);
    } else {
      profilePage = profileRepository.findAll(pageable);
    }
    
    return profilePage.map(profileMapper::toProfileResponse);
  }

  /** [PUBLIC] Get profile AND posts of the CURRENT user (for /me endpoint) */
  public ProfileWithPostsResponse getCurrentProfileWithPosts(
      Authentication authentication, Pageable pageable) {
    String currentUserId = securityUtils.getCurrentUserId(authentication);
    // When viewing "myself", targetUserId and currentUserId are the SAME
    return buildProfileWithPosts(currentUserId, currentUserId, pageable);
  }

  /** [PUBLIC] Get profile AND posts of ANY user (for /{userId} endpoint) */
  public ProfileWithPostsResponse getProfileWithPostsByUserId(
      String targetUserId, Authentication authentication, Pageable pageable) {
    String currentUserId = securityUtils.getCurrentUserId(authentication);
    // When viewing "someone else", targetUserId (from URL) and currentUserId (from token) differ
    return buildProfileWithPosts(targetUserId, currentUserId, pageable);
  }

  /** Get profile of the current user (profile only, no posts) */
  public ProfileResponse getCurrentProfile(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);
    UserProfile userProfile =
        profileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    ProfileResponse response = profileMapper.toProfileResponse(userProfile);
    response.setRelationshipStatus(RelationshipStatus.SELF);
    response.setFollowing(false);
    response.setIsBlocked(false); // Can't block yourself
    if (response.getFriends() == null) {
      response.setFriends(Collections.emptyList());
    }
    return response;
  }

  /** Get profile for any user (profile only, no posts) */
  public ProfileResponse getProfileByUserId(String targetUserId, Authentication authentication) {
    log.info("[PROFILE_GET] Fetching profile for target: {}", targetUserId);

    var targetProfile =
        profileRepository
            .findByUserId(targetUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    log.info("[PROFILE_GET] Found profile: {}", targetProfile.getDisplayName());

    ProfileResponse response = profileMapper.toProfileResponse(targetProfile);

    // Guest viewer: no relationship, no follow, no block — just public profile data
    boolean isGuest = authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName());

    if (isGuest) {
      response.setRelationshipStatus(RelationshipStatus.NOT_FRIENDS);
      response.setFollowing(false);
      response.setIsBlocked(false);
      response.setEmail(null);
      response.setPhoneNumber(null);
      response.setDob(null);
      if (response.getFriends() == null) {
        response.setFriends(Collections.emptyList());
      }
      return response;
    }

    String currentUserId = securityUtils.getCurrentUserId(authentication);

    // Determine relationship status and follow state
    RelationshipStatus status =
        socialUtils.determineRelationshipStatus(currentUserId, targetProfile);
    response.setRelationshipStatus(status);
    log.info("[PROFILE_GET] Determined relationship status: {}", status);

    boolean isFollowing =
        followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
    response.setFollowing(isFollowing);
    log.info("[PROFILE_GET] Follow check complete.");

    // Check if current user has blocked this profile
    boolean isBlocked = blockService.hasBlocked(currentUserId, targetUserId);
    response.setIsBlocked(isBlocked);

    if (response.getFriends() == null) {
      response.setFriends(Collections.emptyList());
    }

    // PRIVACY: Redact PII fields for non-SELF viewers
    if (status != RelationshipStatus.SELF) {
      response.setEmail(null);
      response.setPhoneNumber(null);
      response.setDob(null);
    }

    return response;
  }

  /** Verify OTP and create user (Keycloak + Mongo). Returns plaintext password for auto-login. */
  @Transactional
  public String verifyOtpAndCreateUser(String email, String otp) {
    // 1. Verify OTP
    SignupRequest req = validateSignupOtp(email, otp);

    // 2. Capture password before deletion (needed for auto-login in controller)
    String plainPassword = req.getPassword();

    // 3. Create user on Keycloak
    String userId = createKeycloakUser(req);

    // 4. Save profile to MongoDB
    createMongoProfile(req, userId);

    // 5. Delete signup request
    signupRequestRepository.delete(req);
    log.info("Signup verified and created user in Keycloak + Mongo, userId={}", userId);

    return plainPassword;
  }

  /** Verify OTP and reset password (Keycloak) */
  @Transactional
  public void resetPassword(String email, String otp, String newPassword) {
    // 1. Verify OTP
    ResetPasswordRequest req = validateResetPasswordOtp(email, otp);

    // 2. Update password on Keycloak
    updateKeycloakPassword(email, newPassword);

    // 3. Delete reset request
    resetPasswordRepository.delete(req);
  }

    /** Update the current user's Profile */
    @Transactional
    public ProfileResponse updateProfile(
            Authentication authentication, ProfileUpdateRequest request) {

        String userId = securityUtils.getCurrentUserId(authentication);

        // 1. Load current profile from DB
        UserProfile userProfile =
                profileRepository
                        .findByUserId(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Collect fields to update into Mongo Update object, also update in-memory object
        Update update = new Update();

        if (request.getFirstName() != null) {
            update.set("firstName", request.getFirstName());
            userProfile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            update.set("lastName", request.getLastName());
            userProfile.setLastName(request.getLastName());
        }
        if (request.getDisplayName() != null) {
            update.set("displayName", request.getDisplayName());
            userProfile.setDisplayName(request.getDisplayName());
        }
        if (request.getDob() != null) {
            update.set("dob", request.getDob());
            userProfile.setDob(request.getDob());
        }
        if (request.getPhoneNumber() != null) {
            update.set("phoneNumber", request.getPhoneNumber());
            userProfile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            update.set("avatarUrl", request.getAvatarUrl());
            userProfile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getCoverImageUrl() != null) {
            update.set("coverImageUrl", request.getCoverImageUrl());
            userProfile.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getBio() != null) {
            update.set("bio", request.getBio());
            userProfile.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            update.set("location", request.getLocation());
            userProfile.setLocation(request.getLocation());
        }
        if (request.getPreferences() != null) {
            update.set("preferences", request.getPreferences());
            userProfile.setPreferences(request.getPreferences());
        }

        // 3. Execute UPDATE directly against MongoDB (bypasses .save() which can cause issues)
        Query query = new Query(Criteria.where("userId").is(userId));
        mongoTemplate.updateFirst(query, update, UserProfile.class);

        // 4. Sync data to Typesense
        eventPublisher.publishEvent(UserIndexEvent.index(userProfile));

        // 5. Build response DTO
        ProfileResponse response = profileMapper.toProfileResponse(userProfile);
        response.setRelationshipStatus(RelationshipStatus.SELF);
        response.setFollowing(false);
        if (response.getFriends() == null) {
            response.setFriends(Collections.emptyList());
        }
        return response;
    }

  /**
   * Delete user account (GDPR right to erasure).
   * 1. Delete from Keycloak (prevents login)
   * 2. Remove all follows
   * 3. Anonymize profile in MongoDB (preserves referential integrity for posts/comments)
  * 4. Cleanup cross-module user data
  * 5. Remove from search index
   */
  @Transactional
  public void deleteAccount(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);

    UserProfile profile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    String originalEmail = profile.getEmail();
    String originalUsername = profile.getUsername();
    User localUser = null;
    if (originalEmail != null && !originalEmail.isBlank() && originalUsername != null && !originalUsername.isBlank()) {
      localUser = userRepository.findByEmailOrUsername(originalEmail, originalUsername).orElse(null);
    }
    if (localUser == null && originalEmail != null && !originalEmail.isBlank()) {
      localUser = userRepository.findByEmail(originalEmail).orElse(null);
    }
    if (localUser == null && originalUsername != null && !originalUsername.isBlank()) {
      localUser = userRepository.findByUsername(originalUsername).orElse(null);
    }
    if (localUser == null) {
      localUser = userRepository.findByGoogleId(userId).orElse(null);
    }

    // 1. Delete from Keycloak
    try {
      var token = keycloakAdminClient.exchangeToken(
          TokenExchangeParam.builder()
              .grant_type("client_credentials")
              .client_id(clientId)
              .client_secret(clientSecret)
              .scope("openid")
              .build());
      keycloakAdminClient.deleteUser("Bearer " + token.getAccessToken(), userId);
    } catch (Exception e) {
      log.error("Failed to delete Keycloak user {}: {}", userId, e.getMessage(), e);
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to delete account from identity provider");
    }

    // 2. Remove social relationships owned by this profile and repair surviving counters.
    long followRecords = cleanupFollowRelationships(userId);

    // 3. Anonymize profile (keeps document for referential integrity)
    profile.setEmail(buildDeletedProfileEmail(userId));
    profile.setUsername(buildDeletedProfileUsername(userId));
    profile.setDisplayName("Deleted User");
    profile.setFirstName(null);
    profile.setLastName(null);
    profile.setFullName(null);
    profile.setPhoneNumber(null);
    profile.setAvatarUrl(null);
    profile.setCoverImageUrl(null);
    profile.setBio(null);
    profile.setLocation(null);
    profile.setDob(null);
    profile.setPreferences(null);
    profile.setFriends(null);
    profile.setVerified(false);
    if (profile.getStatistics() != null) {
      profile.getStatistics().setFollowerCount(0L);
      profile.getStatistics().setFollowingCount(0L);
      profile.getStatistics().setFriendCount(0L);
      profile.getStatistics().setFriendRequestCount(0L);
    }
    profileRepository.save(profile);

    long identityRecords = followRecords + cleanupIdentityUserData(userId, originalEmail, localUser);

    // 4. Cleanup cross-module user data
    long socialRecords = postProvider.cleanupDeletedUserData(userId);
    long culinaryRecords = recipeProvider.cleanupDeletedUserData(userId);
    eventPublisher.publishEvent(UserDeletedEvent.builder().userId(userId).build());

    // 5. Remove from search index
    eventPublisher.publishEvent(UserIndexEvent.remove(userId));

    log.info(
      "Account deleted for userId={}, identityRecordsAffected={}, socialRecordsAffected={}, culinaryRecordsAffected={}",
      userId,
      identityRecords,
      socialRecords,
      culinaryRecords);
  }

  /**
   * Export all user data (GDPR right to data portability).
   * Returns a structured map of all PII and user-generated content references.
   */
  public Map<String, Object> exportUserData(Authentication authentication) {
    String userId = securityUtils.getCurrentUserId(authentication);

    UserProfile profile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("exportDate", Instant.now().toString());
    data.put("userId", profile.getUserId());

    // Personal info
    Map<String, Object> personalInfo = new LinkedHashMap<>();
    personalInfo.put("username", profile.getUsername());
    personalInfo.put("email", profile.getEmail());
    personalInfo.put("displayName", profile.getDisplayName());
    personalInfo.put("firstName", profile.getFirstName());
    personalInfo.put("lastName", profile.getLastName());
    personalInfo.put("phoneNumber", profile.getPhoneNumber());
    personalInfo.put("bio", profile.getBio());
    personalInfo.put("location", profile.getLocation());
    personalInfo.put("dateOfBirth", profile.getDob() != null ? profile.getDob().toString() : null);
    personalInfo.put("accountType", profile.getAccountType());
    personalInfo.put("verified", profile.isVerified());
    personalInfo.put("createdAt", profile.getCreatedAt() != null ? profile.getCreatedAt().toString() : null);
    data.put("personalInfo", personalInfo);

    // Statistics
    if (profile.getStatistics() != null) {
      data.put("statistics", profile.getStatistics());
    }

    // Preferences
    if (profile.getPreferences() != null) {
      data.put("preferences", profile.getPreferences());
    }

    // Social connections
    long followersCount = followRepository.countByFollowingId(userId);
    long followingCount = followRepository.countByFollowerId(userId);
    Map<String, Object> social = new LinkedHashMap<>();
    social.put("followersCount", followersCount);
    social.put("followingCount", followingCount);
    data.put("socialConnections", social);

    return data;
  }

  // ===================================================================
  // === PRIVATE HELPERS (Internal logic) ===
  // ===================================================================

  private long cleanupIdentityUserData(String userId, String email, User localUser) {
    long affectedRecords = 0;

    if (email != null && !email.isBlank()) {
      if (signupRequestRepository.findByEmail(email).isPresent()) {
        signupRequestRepository.deleteByEmail(email);
        affectedRecords += 1;
      }

      var resetPasswordRequest = resetPasswordRepository.findByEmail(email);
      if (resetPasswordRequest.isPresent()) {
        resetPasswordRepository.delete(resetPasswordRequest.get());
        affectedRecords += 1;
      }
    }

    affectedRecords += userSettingsRepository.deleteByUserId(userId);
    affectedRecords += userSubscriptionRepository.deleteByUserId(userId);
    affectedRecords += creatorTipSettingsRepository.deleteByUserId(userId);
    affectedRecords += referralCodeRepository.deleteByUserId(userId);
    affectedRecords += referralRedemptionRepository.deleteByReferrerUserId(userId);
    affectedRecords += referralRedemptionRepository.deleteByReferredUserId(userId);
    affectedRecords += verificationRequestRepository.deleteByUserId(userId);
    affectedRecords += userEventRepository.deleteByUserId(userId);

    List<Block> outgoingBlocks = blockRepository.findAllByBlockerId(userId);
    if (!outgoingBlocks.isEmpty()) {
      blockRepository.deleteAll(outgoingBlocks);
      affectedRecords += outgoingBlocks.size();
    }

    List<Block> incomingBlocks = blockRepository.findAllByBlockedId(userId);
    if (!incomingBlocks.isEmpty()) {
      blockRepository.deleteAll(incomingBlocks);
      affectedRecords += incomingBlocks.size();
    }

    List<FriendRequest> sentFriendRequests = friendRequestRepository.findAllBySenderId(userId);
    if (!sentFriendRequests.isEmpty()) {
      for (FriendRequest request : sentFriendRequests) {
        if (request.getReceiverId() != null && !request.getReceiverId().isBlank()) {
          statisticsCounterOperations.incrementCounter(
              request.getReceiverId(), "friendRequestCount", -1);
        }
      }
      friendRequestRepository.deleteAll(sentFriendRequests);
      affectedRecords += sentFriendRequests.size();
    }

    List<FriendRequest> receivedFriendRequests = friendRequestRepository.findAllByReceiverId(userId);
    if (!receivedFriendRequests.isEmpty()) {
      friendRequestRepository.deleteAll(receivedFriendRequests);
      affectedRecords += receivedFriendRequests.size();
    }

    List<UserProfile> linkedFriendProfiles = profileRepository.findAllByFriendsFriendId(userId);
    for (UserProfile linkedProfile : linkedFriendProfiles) {
      List<Friendship> existingFriends = linkedProfile.getFriends();
      if (existingFriends == null || existingFriends.isEmpty()) {
        continue;
      }

      List<Friendship> remainingFriends =
          existingFriends.stream().filter(friendship -> !userId.equals(friendship.getFriendId())).toList();
      int removedFriendCount = existingFriends.size() - remainingFriends.size();
      if (removedFriendCount == 0) {
        continue;
      }

      linkedProfile.setFriends(remainingFriends.isEmpty() ? null : remainingFriends);
      profileRepository.save(linkedProfile);
      if (linkedProfile.getUserId() != null && !linkedProfile.getUserId().isBlank()) {
        statisticsCounterOperations.incrementCounter(
            linkedProfile.getUserId(), "friendCount", -removedFriendCount);
      }
      affectedRecords += removedFriendCount;
    }

    if (localUser != null) {
      String localUserId = localUser.getId();
      if (localUserId != null && !localUserId.isBlank()) {
        affectedRecords += userActivityRepository.deleteByKeycloakId(localUserId);
        if (!localUserId.equals(userId)) {
          affectedRecords += userActivityRepository.deleteByKeycloakId(userId);
        }
      } else {
        affectedRecords += userActivityRepository.deleteByKeycloakId(userId);
      }
      userRepository.delete(localUser);
      affectedRecords += 1;
    } else {
      affectedRecords += userActivityRepository.deleteByKeycloakId(userId);
    }

    long tippedByUser = tipRepository.findByTipperIdOrderByCreatedAtDesc(userId).size();
    if (tippedByUser > 0) {
      tipRepository.deleteAllByTipperId(userId);
      affectedRecords += tippedByUser;
    }

    long tipsForUser = tipRepository.findByCreatorIdOrderByCreatedAtDesc(userId).size();
    if (tipsForUser > 0) {
      tipRepository.deleteAllByCreatorId(userId);
      affectedRecords += tipsForUser;
    }

    return affectedRecords;
  }

  private long cleanupFollowRelationships(String userId) {
    long affectedRecords = 0;

    List<Follow> outgoingFollows = followRepository.findAllByFollowerId(userId);
    if (!outgoingFollows.isEmpty()) {
      for (Follow follow : outgoingFollows) {
        if (follow.getFollowingId() != null && !follow.getFollowingId().isBlank()) {
          statisticsCounterOperations.incrementCounter(follow.getFollowingId(), "followerCount", -1);
        }
      }
      followRepository.deleteAll(outgoingFollows);
      affectedRecords += outgoingFollows.size();
    }

    List<Follow> incomingFollows = followRepository.findAllByFollowingId(userId);
    if (!incomingFollows.isEmpty()) {
      for (Follow follow : incomingFollows) {
        if (follow.getFollowerId() != null && !follow.getFollowerId().isBlank()) {
          statisticsCounterOperations.incrementCounter(follow.getFollowerId(), "followingCount", -1);
        }
      }
      followRepository.deleteAll(incomingFollows);
      affectedRecords += incomingFollows.size();
    }

    return affectedRecords;
  }

  private String buildDeletedProfileEmail(String userId) {
    return "deleted+" + deletedIdentitySuffix(userId) + "@deleted.chefkix.local";
  }

  private String buildDeletedProfileUsername(String userId) {
    return "deleted_" + deletedIdentitySuffix(userId);
  }

  private String deletedIdentitySuffix(String userId) {
    String normalized = userId == null ? "user" : userId.replaceAll("[^A-Za-z0-9]", "");
    return normalized.isBlank() ? "user" : normalized;
  }

  /**
   * [CORE] Build a ProfileWithPostsResponse object.
   * Uses PostProvider for cross-module post retrieval.
   */
  private ProfileWithPostsResponse buildProfileWithPosts(
      String targetUserId, String currentUserId, Pageable pageable) {

    // 1. PREPARE TASK A (Profile - CPU) - Run in parallel
    CompletableFuture<ProfileResponse> profileFuture =
        CompletableFuture.supplyAsync(
            () -> {
              UserProfile userProfile =
                  profileRepository
                      .findByUserId(targetUserId)
                      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

              ProfileResponse localResponse = profileMapper.toProfileResponse(userProfile);

              // Determine relationship logic
              RelationshipStatus status =
                  socialUtils.determineRelationshipStatus(currentUserId, userProfile);
              localResponse.setRelationshipStatus(status);

              boolean isFollowing =
                  followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
              localResponse.setFollowing(isFollowing);

              if (localResponse.getFriends() == null) {
                localResponse.setFriends(Collections.emptyList());
              }

              // PRIVACY: Redact PII fields for non-SELF viewers
              if (status != RelationshipStatus.SELF) {
                localResponse.setEmail(null);
                localResponse.setPhoneNumber(null);
                localResponse.setDob(null);
              }

              return localResponse;
            },
            taskExecutor);

    // 2. Get posts via PostProvider (direct call in monolith, was Feign)
    try {
      Page<PostSummary> posts = postProvider.getPostsByUserId(targetUserId, pageable);

      // 3. WAIT FOR PROFILE RESULT
      ProfileResponse profile = profileFuture.join();

      // 4. Return result
      return new ProfileWithPostsResponse(profile, posts);

    } catch (Exception e) {
      log.error("Failed to load profile and posts in parallel: {}", e.getMessage(), e);
      throw new AppException(
          ErrorCode.INTERNAL_SERVER_ERROR, "Failed to load profile data");
    }
  }

  /** Helper to update fields when non-null */
  private <T> void updateIfNotNull(T value, Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }

  // --- Helpers for Registration (verifyOtpAndCreateUser) ---

  private SignupRequest validateSignupOtp(String email, String otp) {
    SignupRequest req =
        signupRequestRepository
            .findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.SIGNUP_REQUEST_NOT_FOUND));

    // Check max attempts first (before expiry check to prevent timing attacks)
    int currentAttempts = req.getAttempts() != null ? req.getAttempts() : 0;
    if (currentAttempts >= maxOtpAttempts) {
      signupRequestRepository.delete(req);
      throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
    }

    // Check expiry
    if (Instant.now().isAfter(req.getExpiresAt())) {
      signupRequestRepository.delete(req);
      throw new AppException(ErrorCode.OTP_EXPIRED);
    }

    // Validate OTP hash
    String expected = emailService.hmacOtp(otp);
    if (!constantTimeEquals(expected, req.getOtpHash())) {
      req.setAttempts(currentAttempts + 1);
      signupRequestRepository.save(req);
      throw new AppException(ErrorCode.OTP_INVALID);
    }
    return req;
  }

  private String createKeycloakUser(SignupRequest req) {
    try {
      var token =
          keycloakAdminClient.exchangeToken(
              TokenExchangeParam.builder()
                  .grant_type("client_credentials")
                  .client_id(clientId)
                  .client_secret(clientSecret)
                  .scope("openid")
                  .build());

      var creationResponse =
          keycloakAdminClient.createUser(
              "Bearer " + token.getAccessToken(),
              UserCreationParam.builder()
                  .username(req.getUsername())
                  .email(req.getEmail())
                  .firstName(req.getFirstName())
                  .lastName(req.getLastName())
                  .enabled(true)
                  .emailVerified(false)
                  .credentials(
                      List.of(
                          Credential.builder()
                              .type("password")
                              .temporary(false)
                              .value(req.getPassword())
                              .build()))
                  .build());

      return extractUserId(creationResponse);
    } catch (Exception e) {
      log.error(
          "Error creating user on Keycloak for email {}: {}", req.getEmail(), e.getMessage(), e);
      throw errorNormalizer.handleKeyCloakException(e);
    }
  }

  private UserProfile createMongoProfile(SignupRequest req, String userId) {
    UserProfile profile = profileMapper.toProfile(req);
    profile.setUserId(userId);
    profile.setEmail(req.getEmail());
    profile.setFirstName(req.getFirstName());
    profile.setDisplayName(req.getDisplayName());
    profile.setDob(req.getDob());
    profile.setLastName(req.getLastName());
    profile.setUsername(req.getUsername());
    profile.setAccountType("user");

    Statistics initialStats = Statistics.builder().build();
    profile.setStatistics(initialStats);

    UserProfile saved = profileRepository.save(profile);

    // Real-time Typesense indexing
    eventPublisher.publishEvent(UserIndexEvent.index(saved));

    return saved;
  }

  // --- Helpers for Password Reset (resetPassword) ---

  private ResetPasswordRequest validateResetPasswordOtp(String email, String otp) {
    ResetPasswordRequest req =
        resetPasswordRepository
            .findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.RESET_PASSWORD_REQUEST_NOT_FOUND));

    // Check max attempts first (before expiry check to prevent timing attacks)
    int currentAttempts = req.getAttempts() != null ? req.getAttempts() : 0;
    if (currentAttempts >= maxOtpAttempts) {
      resetPasswordRepository.delete(req);
      throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
    }

    // Check expiry
    if (Instant.now().isAfter(req.getExpiresAt())) {
      resetPasswordRepository.delete(req);
      throw new AppException(ErrorCode.OTP_EXPIRED);
    }

    // Validate OTP hash
    String expected = emailService.hmacOtp(otp);
    if (!constantTimeEquals(expected, req.getOtpHash())) {
      req.setAttempts(currentAttempts + 1);
      resetPasswordRepository.save(req);
      throw new AppException(ErrorCode.OTP_INVALID);
    }
    return req;
  }

  public InternalBasicProfileResponse getBasicProfile(String userId) {
    UserProfile user =
        profileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // CRITICAL: displayName is OPTIONAL. Apply fallback: displayName → firstName+lastName → username
    String displayName = user.getDisplayName();
    if (displayName == null || displayName.isBlank()) {
      String firstName = user.getFirstName();
      String lastName = user.getLastName();
      if (firstName != null && !firstName.isBlank()) {
        displayName = lastName != null && !lastName.isBlank() 
            ? firstName + " " + lastName 
            : firstName;
      } else if (lastName != null && !lastName.isBlank()) {
        displayName = lastName;
      } else {
        displayName = user.getUsername() != null ? user.getUsername() : "Unknown User";
      }
    }

    return InternalBasicProfileResponse.builder()
        .userId(user.getUserId())
        .username(user.getUsername())
        .displayName(displayName)
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .avatarUrl(user.getAvatarUrl())
        .verified(user.isVerified())
        .build();
  }

  private void updateKeycloakPassword(String email, String newPassword) {
    // Need to find UserId from Profile Repository
    var userProfile =
        profileRepository
            .findByEmail(email)
            .orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_FOUND));

    String userId = userProfile.getUserId();
    if (userId == null) {
      throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    try {
      // 1. Get Admin Access Token
      var token =
          keycloakAdminClient.exchangeToken(
              TokenExchangeParam.builder()
                  .grant_type("client_credentials")
                  .client_id(clientId)
                  .client_secret(clientSecret)
                  .scope("openid")
                  .build());

      // 2. Call resetPassword API
      keycloakAdminClient.resetPassword(
          "Bearer " + token.getAccessToken(),
          userId,
          ResetPasswordParam.builder()
              .type("password")
              .temporary(false)
              .value(newPassword)
              .build());

      // 3. Revoke all sessions — force re-authentication on all devices
      keycloakAdminClient.logoutUser("Bearer " + token.getAccessToken(), userId);

      log.info("Password successfully reset for userId={}", userId);

    } catch (Exception e) {
      log.error("Failed to reset password in Keycloak for userId={}", userId, e);
      throw errorNormalizer.handleKeyCloakException(e);
    }
  }

  // --- General helpers ---

  private String extractUserId(ResponseEntity<?> response) {
    // Safer handling
    try {
      var locationHeader = response.getHeaders().get("Location");
      if (locationHeader == null || locationHeader.isEmpty()) {
        throw new AppException(
            ErrorCode.INTERNAL_SERVER_ERROR, "Location header missing after user creation.");
      }
      String location = locationHeader.getFirst();
      String[] splitedStr = location.split("/");
      return splitedStr[splitedStr.length - 1];
    } catch (Exception e) {
      log.error(
          "Could not extract UserId from 'Location' header: {}",
          response.getHeaders().get("Location"),
          e);
      throw new AppException(
          ErrorCode.INTERNAL_SERVER_ERROR, "Could not get UserId after user creation.");
    }
  }

  private boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    if (a.length() != b.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }
}
