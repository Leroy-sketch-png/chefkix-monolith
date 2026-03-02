package com.chefkix.identity.repository;

import com.chefkix.identity.entity.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
  Optional<UserProfile> findByEmail(String email);

  Optional<UserProfile> findByUserId(String userId);

  Optional<UserProfile> findByUsername(String username);

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
