package com.chefkix.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.entity.Follow;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.entity.UserSettings;
import com.chefkix.identity.enums.RelationshipStatus;
import com.chefkix.identity.mapper.ProfileMapper;
import com.chefkix.identity.repository.BlockRepository;
import com.chefkix.identity.repository.FollowRepository;
import com.chefkix.identity.repository.FriendRequestRepository;
import com.chefkix.identity.repository.FriendshipRepository;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.utils.SecurityUtils;
import com.chefkix.identity.utils.SocialUtils;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SocialServiceFollowTest {

  @Mock private FollowRepository followRepository;
  @Mock private FriendRequestRepository friendRequestRepository;
  @Mock private UserProfileRepository userProfileRepository;
  @Mock private FriendshipRepository friendshipRepository;
  @Mock private BlockRepository blockRepository;
  @Mock private ProfileMapper profileMapper;
  @Mock private MongoTemplate mongoTemplate;
  @Mock private SecurityUtils securityUtils;
  @Mock private SocialUtils socialUtils;
  @Mock private StatisticsService statisticsService;
  @Mock private SettingsService settingsService;
  @Mock private KafkaTemplate<String, Object> kafkaTemplate;
  @Mock private Authentication authentication;

  @InjectMocks private SocialService socialService;

  private final String followerId = "user-alice";
  private final String followingId = "user-bob";
  private UserProfile bobProfile;
  private UserProfile aliceProfile;
  private ProfileResponse bobResponse;

  @BeforeEach
  void setUp() {
    bobProfile = UserProfile.builder().userId(followingId).displayName("Bob").build();
    aliceProfile = UserProfile.builder().userId(followerId).displayName("Alice").build();
    bobResponse = ProfileResponse.builder().userId(followingId).displayName("Bob").build();
  }

  @Test
  @DisplayName("Happy path: follow persists record, increments counters, and emits notification")
  void happyPathFollowSavesRecordAndIncrementsCounters() {
    when(securityUtils.getCurrentUserId(authentication)).thenReturn(followerId);
    when(blockRepository.existsBlockBetween(followerId, followingId)).thenReturn(false);
    when(userProfileRepository.findByUserId(followingId)).thenReturn(Optional.of(bobProfile));
    when(userProfileRepository.findByUserId(followerId)).thenReturn(Optional.of(aliceProfile));
    when(followRepository.findByFollowerIdAndFollowingId(followerId, followingId))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(Follow.builder().followerId(followerId).followingId(followingId).build()));
    when(followRepository.findByFollowerIdAndFollowingId(followingId, followerId))
        .thenReturn(Optional.empty());
    when(settingsService.getPrivacySettingsByUserId(followingId))
        .thenReturn(UserSettings.PrivacySettings.builder().allowFollowers(true).build());
    when(profileMapper.toProfileResponse(bobProfile)).thenReturn(bobResponse);
    when(socialUtils.determineRelationshipStatus(followerId, bobProfile))
        .thenReturn(RelationshipStatus.NOT_FRIENDS);

    ProfileResponse response = socialService.toggleFollow(followingId, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getFollowing()).isTrue();
    verify(followRepository).save(any(Follow.class));
    verify(statisticsService).incrementCounter(followerId, "followingCount", 1);
    verify(statisticsService).incrementCounter(followingId, "followerCount", 1);
    verify(kafkaTemplate).send(eq("new-follower-delivery"), any());
  }

  @Test
  @DisplayName("Race condition: DuplicateKeyException on concurrent follow skips counter increments and notification")
  void duplicateFollowAttemptCatchesDuplicateKeyAndNeverIncrementsCounters() {
    when(securityUtils.getCurrentUserId(authentication)).thenReturn(followerId);
    when(blockRepository.existsBlockBetween(followerId, followingId)).thenReturn(false);
    when(userProfileRepository.findByUserId(followingId)).thenReturn(Optional.of(bobProfile));
    when(userProfileRepository.findByUserId(followerId)).thenReturn(Optional.of(aliceProfile));
    when(followRepository.findByFollowerIdAndFollowingId(followerId, followingId))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(Follow.builder().followerId(followerId).followingId(followingId).build()));
    when(followRepository.findByFollowerIdAndFollowingId(followingId, followerId))
        .thenReturn(Optional.empty());
    when(settingsService.getPrivacySettingsByUserId(followingId))
        .thenReturn(UserSettings.PrivacySettings.builder().allowFollowers(true).build());
    when(profileMapper.toProfileResponse(bobProfile)).thenReturn(bobResponse);
    when(socialUtils.determineRelationshipStatus(followerId, bobProfile))
        .thenReturn(RelationshipStatus.NOT_FRIENDS);

    // Simulate unique constraint violation from concurrent duplicate follow
    when(followRepository.save(any(Follow.class)))
        .thenThrow(new DuplicateKeyException("E11000 duplicate key error collection: follows index: followerId_followingId"));

    ProfileResponse response = socialService.toggleFollow(followingId, authentication);

    assertThat(response).isNotNull();
    // Verify followRepository.save was called, but counters and notification were skipped
    verify(followRepository).save(any(Follow.class));
    verify(statisticsService, never()).incrementCounter(anyString(), anyString(), anyInt());
    verify(kafkaTemplate, never()).send(anyString(), any());
  }

  @Test
  @DisplayName("Unfollow: existing follow record is deleted and counters are decremented")
  void unfollowDeletesRecordAndDecrementsCounters() {
    Follow existing = Follow.builder().followerId(followerId).followingId(followingId).build();
    when(securityUtils.getCurrentUserId(authentication)).thenReturn(followerId);
    when(blockRepository.existsBlockBetween(followerId, followingId)).thenReturn(false);
    when(userProfileRepository.findByUserId(followingId)).thenReturn(Optional.of(bobProfile));
    when(userProfileRepository.findByUserId(followerId)).thenReturn(Optional.of(aliceProfile));
    when(followRepository.findByFollowerIdAndFollowingId(followerId, followingId))
        .thenReturn(Optional.of(existing))
        .thenReturn(Optional.empty());
    when(followRepository.findByFollowerIdAndFollowingId(followingId, followerId))
        .thenReturn(Optional.empty());
    when(profileMapper.toProfileResponse(bobProfile)).thenReturn(bobResponse);
    when(socialUtils.determineRelationshipStatus(followerId, bobProfile))
        .thenReturn(RelationshipStatus.NOT_FRIENDS);

    ProfileResponse response = socialService.toggleFollow(followingId, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getFollowing()).isFalse();
    verify(followRepository).delete(existing);
    verify(statisticsService).incrementCounter(followerId, "followingCount", -1);
    verify(statisticsService).incrementCounter(followingId, "followerCount", -1);
    verify(kafkaTemplate, never()).send(anyString(), any());
  }

  @Test
  @DisplayName("Self-follow: rejected immediately before persistence or counter operations")
  void selfFollowIsRejectedBeforeAnyMutation() {
    when(securityUtils.getCurrentUserId(authentication)).thenReturn(followerId);

    assertThatThrownBy(() -> socialService.toggleFollow(followerId, authentication))
        .isInstanceOfSatisfying(AppException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    verify(followRepository, never()).save(any());
    verify(followRepository, never()).delete(any());
    verify(statisticsService, never()).incrementCounter(anyString(), anyString(), anyInt());
  }

  @Test
  @DisplayName("Blocked user: cannot follow and throws DO_NOT_HAVE_PERMISSION")
  void blockedUserCannotFollow() {
    when(securityUtils.getCurrentUserId(authentication)).thenReturn(followerId);
    when(blockRepository.existsBlockBetween(followerId, followingId)).thenReturn(true);

    assertThatThrownBy(() -> socialService.toggleFollow(followingId, authentication))
        .isInstanceOfSatisfying(AppException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DO_NOT_HAVE_PERMISSION));

    verify(followRepository, never()).save(any());
    verify(statisticsService, never()).incrementCounter(anyString(), anyString(), anyInt());
  }

  @Test
  @DisplayName("Privacy restriction: target disallowing followers is rejected with DO_NOT_HAVE_PERMISSION")
  void targetUserDisallowingFollowersThrowsPermission() {
    when(securityUtils.getCurrentUserId(authentication)).thenReturn(followerId);
    when(blockRepository.existsBlockBetween(followerId, followingId)).thenReturn(false);
    when(userProfileRepository.findByUserId(followingId)).thenReturn(Optional.of(bobProfile));
    when(userProfileRepository.findByUserId(followerId)).thenReturn(Optional.of(aliceProfile));
    when(followRepository.findByFollowerIdAndFollowingId(followerId, followingId))
        .thenReturn(Optional.empty());
    when(settingsService.getPrivacySettingsByUserId(followingId))
        .thenReturn(UserSettings.PrivacySettings.builder().allowFollowers(false).build());

    assertThatThrownBy(() -> socialService.toggleFollow(followingId, authentication))
        .isInstanceOfSatisfying(AppException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DO_NOT_HAVE_PERMISSION));

    verify(followRepository, never()).save(any());
    verify(statisticsService, never()).incrementCounter(anyString(), anyString(), anyInt());
  }

  @Test
  @DisplayName("Concurrent follow race: 10 concurrent threads result in exactly 1 counter increment")
  void concurrentFollowSimulationEnsuresSingleIncrement() throws Exception {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch endGate = new CountDownLatch(threadCount);
    AtomicInteger successfulSaves = new AtomicInteger(0);

    when(securityUtils.getCurrentUserId(authentication)).thenReturn(followerId);
    when(blockRepository.existsBlockBetween(followerId, followingId)).thenReturn(false);
    when(userProfileRepository.findByUserId(followingId)).thenReturn(Optional.of(bobProfile));
    when(userProfileRepository.findByUserId(followerId)).thenReturn(Optional.of(aliceProfile));

    // All 10 threads enter toggleFollow simultaneously before any follow record is committed (race condition)
    // All see existingFollow as empty, so all 10 threads attempt to save concurrently
    when(followRepository.findByFollowerIdAndFollowingId(followerId, followingId))
        .thenReturn(Optional.empty());
    when(followRepository.findByFollowerIdAndFollowingId(followingId, followerId))
        .thenReturn(Optional.empty());
    when(settingsService.getPrivacySettingsByUserId(followingId))
        .thenReturn(UserSettings.PrivacySettings.builder().allowFollowers(true).build());
    when(profileMapper.toProfileResponse(bobProfile)).thenReturn(bobResponse);
    when(socialUtils.determineRelationshipStatus(followerId, bobProfile))
        .thenReturn(RelationshipStatus.NOT_FRIENDS);

    // Only the first thread succeeds in saving; all other 9 threads encounter DuplicateKeyException
    when(followRepository.save(any(Follow.class))).thenAnswer(inv -> {
      if (successfulSaves.getAndIncrement() == 0) {
        return inv.getArgument(0);
      }
      throw new DuplicateKeyException("E11000 duplicate key error");
    });

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          startGate.await();
          socialService.toggleFollow(followingId, authentication);
        } catch (Exception ignored) {
        } finally {
          endGate.countDown();
        }
      });
    }

    startGate.countDown();
    boolean completed = endGate.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(completed).isTrue();
    // Verify followRepository.save was attempted by all 10 threads
    verify(followRepository, times(threadCount)).save(any(Follow.class));
    // Verify counters were incremented EXACTLY ONCE, never double-counted!
    verify(statisticsService, times(1)).incrementCounter(followerId, "followingCount", 1);
    verify(statisticsService, times(1)).incrementCounter(followingId, "followerCount", 1);
    verify(statisticsService, never()).incrementCounter(anyString(), anyString(), eq(-1));
    verify(kafkaTemplate, times(1)).send(eq("new-follower-delivery"), any());
  }
}
