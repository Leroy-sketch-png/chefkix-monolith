package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.identity.client.KeycloakAdminClient;
import com.chefkix.identity.dto.identity.TokenExchangeParam;
import com.chefkix.identity.dto.identity.TokenExchangeResponse;
import com.chefkix.identity.entity.Block;
import com.chefkix.identity.entity.Follow;
import com.chefkix.identity.entity.FriendRequest;
import com.chefkix.identity.entity.Friendship;
import com.chefkix.identity.entity.ResetPasswordRequest;
import com.chefkix.identity.entity.SignupRequest;
import com.chefkix.identity.entity.Statistics;
import com.chefkix.identity.entity.User;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.exception.ErrorNormalizer;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.BlockRepository;
import com.chefkix.identity.repository.CreatorTipSettingsRepository;
import com.chefkix.identity.repository.FollowRepository;
import com.chefkix.identity.repository.FriendRequestRepository;
import com.chefkix.identity.repository.ReferralCodeRepository;
import com.chefkix.identity.repository.ReferralRedemptionRepository;
import com.chefkix.identity.repository.ResetPasswordRepository;
import com.chefkix.identity.repository.SignupRequestRepository;
import com.chefkix.identity.repository.TipRepository;
import com.chefkix.identity.repository.UserActivityRepository;
import com.chefkix.identity.repository.UserEventRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.repository.UserRepository;
import com.chefkix.identity.repository.UserSettingsRepository;
import com.chefkix.identity.repository.UserSubscriptionRepository;
import com.chefkix.identity.repository.VerificationRequestRepository;
import com.chefkix.identity.utils.SecurityUtils;
import com.chefkix.identity.utils.SocialUtils;
import com.chefkix.social.api.PostProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  @Mock private UserProfileRepository profileRepository;
  @Mock private ProfileMapper profileMapper;
  @Mock private SignupRequestRepository signupRequestRepository;
  @Mock private ResetPasswordRepository resetPasswordRepository;
  @Mock private FollowRepository followRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserSettingsRepository userSettingsRepository;
  @Mock private UserSubscriptionRepository userSubscriptionRepository;
  @Mock private CreatorTipSettingsRepository creatorTipSettingsRepository;
  @Mock private TipRepository tipRepository;
  @Mock private ReferralCodeRepository referralCodeRepository;
  @Mock private ReferralRedemptionRepository referralRedemptionRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private UserEventRepository userEventRepository;
  @Mock private FriendRequestRepository friendRequestRepository;
  @Mock private BlockRepository blockRepository;
  @Mock private UserActivityRepository userActivityRepository;
  @Mock private KeycloakAdminClient keycloakAdminClient;
  @Mock private PostProvider postProvider;
  @Mock private RecipeProvider recipeProvider;
  @Mock private ErrorNormalizer errorNormalizer;
  @Mock private EmailService emailService;
  @Mock private BlockService blockService;
  @Mock private StatisticsCounterOperations statisticsCounterOperations;
  @Mock private SecurityUtils securityUtils;
  @Mock private SocialUtils socialUtils;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private MongoTemplate mongoTemplate;
  @Mock private Executor taskExecutor;

  @InjectMocks private ProfileService profileService;

  @Test
  void deleteAccountCleansIdentityResidueAndRepairsSurvivingCounters() {
    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    String userId = "12345678-dead-beef-9999";
    String email = "chef@example.com";
    String username = "chefkix-user";

    UserProfile deletedProfile =
        UserProfile.builder()
            .userId(userId)
            .email(email)
            .username(username)
            .displayName("Chef")
            .statistics(Statistics.builder().followerCount(4L).followingCount(3L).friendCount(2L).friendRequestCount(1L).build())
            .build();
    UserProfile survivingFriendProfile =
        UserProfile.builder()
            .userId("friend-1")
            .friends(
                new ArrayList<>(
                    List.of(
                        Friendship.builder().friendId(userId).build(),
                        Friendship.builder().friendId("friend-2").build())))
            .build();
    User localUser = User.builder().id("local-user-1").email(email).username(username).build();
    Follow outgoingFollow = Follow.builder().followerId(userId).followingId("followed-user").build();
    Follow incomingFollow = Follow.builder().followerId("follower-user").followingId(userId).build();
    FriendRequest outgoingRequest =
        FriendRequest.builder().senderId(userId).receiverId("request-receiver").build();
    FriendRequest incomingRequest =
        FriendRequest.builder().senderId("request-sender").receiverId(userId).build();
    Block outgoingBlock = Block.builder().blockerId(userId).blockedId("blocked-user").build();
    Block incomingBlock = Block.builder().blockerId("blocker-user").blockedId(userId).build();
    ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().email(email).build();
    SignupRequest signupRequest = SignupRequest.builder().email(email).username(username).password("password123").firstName("Chef").lastName("Kix").build();

    when(securityUtils.getCurrentUserId(authentication)).thenReturn(userId);
    when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(deletedProfile));
    when(profileRepository.findAllByFriendsFriendId(userId)).thenReturn(List.of(survivingFriendProfile));
    when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(keycloakAdminClient.exchangeToken(any(TokenExchangeParam.class)))
        .thenReturn(TokenExchangeResponse.builder().accessToken("token-123").build());
    when(userRepository.findByEmailOrUsername(email, username)).thenReturn(Optional.of(localUser));
    when(signupRequestRepository.findByEmail(email)).thenReturn(Optional.of(signupRequest));
    when(resetPasswordRepository.findByEmail(email)).thenReturn(Optional.of(resetPasswordRequest));
    when(followRepository.findAllByFollowerId(userId)).thenReturn(List.of(outgoingFollow));
    when(followRepository.findAllByFollowingId(userId)).thenReturn(List.of(incomingFollow));
    when(friendRequestRepository.findAllBySenderId(userId)).thenReturn(List.of(outgoingRequest));
    when(friendRequestRepository.findAllByReceiverId(userId)).thenReturn(List.of(incomingRequest));
    when(blockRepository.findAllByBlockerId(userId)).thenReturn(List.of(outgoingBlock));
    when(blockRepository.findAllByBlockedId(userId)).thenReturn(List.of(incomingBlock));
    when(userSettingsRepository.deleteByUserId(userId)).thenReturn(0L);
    when(userSubscriptionRepository.deleteByUserId(userId)).thenReturn(0L);
    when(creatorTipSettingsRepository.deleteByUserId(userId)).thenReturn(0L);
    when(referralCodeRepository.deleteByUserId(userId)).thenReturn(0L);
    when(referralRedemptionRepository.deleteByReferrerUserId(userId)).thenReturn(0L);
    when(referralRedemptionRepository.deleteByReferredUserId(userId)).thenReturn(0L);
    when(verificationRequestRepository.deleteByUserId(userId)).thenReturn(0L);
    when(userEventRepository.deleteByUserId(userId)).thenReturn(0L);
    when(userActivityRepository.deleteByKeycloakId("local-user-1")).thenReturn(1L);
    when(userActivityRepository.deleteByKeycloakId(userId)).thenReturn(0L);
    when(tipRepository.findByTipperIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
    when(tipRepository.findByCreatorIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
    when(postProvider.cleanupDeletedUserData(userId)).thenReturn(0L);
    when(recipeProvider.cleanupDeletedUserData(userId)).thenReturn(0L);

    profileService.deleteAccount(authentication);

    verify(keycloakAdminClient).deleteUser("Bearer token-123", userId);
    verify(followRepository).deleteAll(List.of(outgoingFollow));
    verify(followRepository).deleteAll(List.of(incomingFollow));
    verify(statisticsCounterOperations).incrementCounter("followed-user", "followerCount", -1);
    verify(statisticsCounterOperations).incrementCounter("follower-user", "followingCount", -1);
    verify(signupRequestRepository).deleteByEmail(email);
    verify(resetPasswordRepository).delete(resetPasswordRequest);
    verify(friendRequestRepository).deleteAll(List.of(outgoingRequest));
    verify(friendRequestRepository).deleteAll(List.of(incomingRequest));
    verify(statisticsCounterOperations).incrementCounter("request-receiver", "friendRequestCount", -1);
    verify(blockRepository).deleteAll(List.of(outgoingBlock));
    verify(blockRepository).deleteAll(List.of(incomingBlock));
    verify(statisticsCounterOperations).incrementCounter("friend-1", "friendCount", -1);
    verify(userActivityRepository).deleteByKeycloakId("local-user-1");
    verify(userRepository).delete(localUser);

    ArgumentCaptor<UserProfile> savedProfiles = ArgumentCaptor.forClass(UserProfile.class);
    verify(profileRepository, atLeast(2)).save(savedProfiles.capture());
    assertThat(savedProfiles.getAllValues())
        .anySatisfy(
            savedProfile -> {
              if (userId.equals(savedProfile.getUserId())) {
                assertThat(savedProfile.getDisplayName()).isEqualTo("Deleted User");
                assertThat(savedProfile.getEmail()).startsWith("deleted+").endsWith("@deleted.chefkix.local");
                assertThat(savedProfile.getUsername()).startsWith("deleted_");
                assertThat(savedProfile.getFriends()).isNull();
                assertThat(savedProfile.getStatistics().getFollowerCount()).isZero();
                assertThat(savedProfile.getStatistics().getFollowingCount()).isZero();
                assertThat(savedProfile.getStatistics().getFriendCount()).isZero();
                assertThat(savedProfile.getStatistics().getFriendRequestCount()).isZero();
              }
            });
    assertThat(savedProfiles.getAllValues())
        .anySatisfy(
            savedProfile -> {
              if ("friend-1".equals(savedProfile.getUserId())) {
                assertThat(savedProfile.getFriends()).extracting(Friendship::getFriendId).containsExactly("friend-2");
              }
            });
  }
}