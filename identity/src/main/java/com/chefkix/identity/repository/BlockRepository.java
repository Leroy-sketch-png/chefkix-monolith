package com.chefkix.identity.repository;

import com.chefkix.identity.entity.Block;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockRepository extends MongoRepository<Block, String> {

  Optional<Block> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

  boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

  /**
   */
  @Query("{ $or: [ { 'blockerId': ?0, 'blockedId': ?1 }, { 'blockerId': ?1, 'blockedId': ?0 } ] }")
  Optional<Block> findAnyBlockBetween(String userId1, String userId2);

  @Query(
      value =
          "{ $or: [ { 'blockerId': ?0, 'blockedId': ?1 }, { 'blockerId': ?1, 'blockedId': ?0 } ] }",
      exists = true)
  boolean existsBlockBetween(String userId1, String userId2);

  List<Block> findAllByBlockerId(String blockerId);

  List<Block> findAllByBlockedId(String blockedId);

  void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);

  long countByBlockerId(String blockerId);
}
