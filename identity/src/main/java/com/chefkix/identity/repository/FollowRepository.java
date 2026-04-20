package com.chefkix.identity.repository;

import com.chefkix.identity.entity.Follow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends MongoRepository<Follow, String> {
  Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);

  boolean existsByFollowerIdAndFollowingId(String currentUserId, String targetUserId);

  // Get all users that userId is following
  List<Follow> findAllByFollowerId(String followerId);

  // Batch: find which of targetIds are followed by followerId
  List<Follow> findAllByFollowerIdAndFollowingIdIn(String followerId, Collection<String> followingIds);

  // Batch: find which of followerIds follow followingId
  List<Follow> findAllByFollowingIdAndFollowerIdIn(String followingId, Collection<String> followerIds);

  // Get all users that follow userId
  List<Follow> findAllByFollowingId(String followingId);

  // Paginated versions for large lists
  Page<Follow> findAllByFollowerId(String followerId, Pageable pageable);

  Page<Follow> findAllByFollowingId(String followingId, Pageable pageable);

  // Count followers and following
  long countByFollowerId(String followerId); // How many I'm following

  long countByFollowingId(String followingId); // How many follow me

  // Delete all follows for account deletion
  void deleteAllByFollowerId(String followerId);

  void deleteAllByFollowingId(String followingId);
}
