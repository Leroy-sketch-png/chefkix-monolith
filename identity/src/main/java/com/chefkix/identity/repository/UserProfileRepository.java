package com.chefkix.identity.repository;

import com.chefkix.identity.entity.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
  Optional<UserProfile> findByEmail(String email);

  Optional<UserProfile> findByUserId(String userId);

    /**
     * Field-limited lookup for profile-only APIs.
     * Excludes heavy nested arrays like friends to reduce payload hydration and tail latency.
     */
    @Query(
      value = "{ 'userId': ?0 }",
      fields =
        "{ 'id': 1, 'userId': 1, 'email': 1, 'username': 1, 'firstName': 1, 'lastName': 1, "
          + "'dob': 1, 'displayName': 1, 'phoneNumber': 1, 'avatarUrl': 1, 'coverImageUrl': 1, "
          + "'bio': 1, 'accountType': 1, 'location': 1, 'preferences': 1, 'statistics': 1, "
          + "'verified': 1, 'createdAt': 1, 'updatedAt': 1 }")
    Optional<UserProfile> findProfileOnlyByUserId(String userId);

  Optional<UserProfile> findByUsername(String username);

  List<UserProfile> findAllByFriendsFriendId(String friendId);

  /**
   * Find all profiles for given user IDs. Used for social features (followers, following, friends
   * lists).
   */
  List<UserProfile> findAllByUserIdIn(List<String> userIds);

  /**
   * Find profiles by displayName or username containing search term (case-insensitive).
   * Used for user discovery with search functionality.
   */
  Page<UserProfile> findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
      String displayName, String username, Pageable pageable);
}
