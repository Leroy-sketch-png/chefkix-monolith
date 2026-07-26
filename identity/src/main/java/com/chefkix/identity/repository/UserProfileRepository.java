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
   */
  List<UserProfile> findAllByUserIdIn(List<String> userIds);

  /**
   */
  Page<UserProfile> findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
      String displayName, String username, Pageable pageable);
}
