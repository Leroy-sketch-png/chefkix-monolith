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

  List<Follow> findAllByFollowerId(String followerId);

  List<Follow> findAllByFollowerIdAndFollowingIdIn(String followerId, Collection<String> followingIds);

  List<Follow> findAllByFollowingIdAndFollowerIdIn(String followingId, Collection<String> followerIds);

  List<Follow> findAllByFollowingId(String followingId);

  Page<Follow> findAllByFollowerId(String followerId, Pageable pageable);

  Page<Follow> findAllByFollowingId(String followingId, Pageable pageable);

long countByFollowerId(String followerId);

long countByFollowingId(String followingId);

  void deleteAllByFollowerId(String followerId);

  void deleteAllByFollowingId(String followingId);
}
