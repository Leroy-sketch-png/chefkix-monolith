package com.chefkix.identity.repository;

import com.chefkix.identity.entity.Block;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockRepository extends MongoRepository<Block, String> {

  /** Find a specific block relationship */
  Optional<Block> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

  /** Check if user A has blocked user B */
  boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

  /**
   * Check if there's ANY block between two users (either direction) This is used to determine if
   * two users should be invisible to each other
   */
  @Query("{ $or: [ { 'blockerId': ?0, 'blockedId': ?1 }, { 'blockerId': ?1, 'blockedId': ?0 } ] }")
  Optional<Block> findAnyBlockBetween(String userId1, String userId2);

  /** Check if there's a block in either direction */
  @Query(
      value =
          "{ $or: [ { 'blockerId': ?0, 'blockedId': ?1 }, { 'blockerId': ?1, 'blockedId': ?0 } ] }",
      exists = true)
  boolean existsBlockBetween(String userId1, String userId2);

  /** Get all users that the specified user has blocked */
  List<Block> findAllByBlockerId(String blockerId);

  /** Get all users who have blocked the specified user */
  List<Block> findAllByBlockedId(String blockedId);

  /** Delete block when unblocking */
  void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);

  /** Count how many users this user has blocked */
  long countByBlockerId(String blockerId);
}
