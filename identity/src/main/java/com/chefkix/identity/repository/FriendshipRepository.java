package com.chefkix.identity.repository;

import com.chefkix.identity.entity.Friendship;
import com.chefkix.identity.entity.UserProfile;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepository extends MongoRepository<Friendship, String> {
  @Query(value = "{ 'userId' : ?0, 'status' : ?1 }", fields = "{ 'friendId' : 1, '_id' : 0 }")
  List<String> findAllFriendIdsByUserId(String userId, String status);

  // Find users whose userId is in the friendIds list AND whose name contains the keyword (case insensitive)
  // Pageable to limit results to 5-10 people for performance
  @Query("{ 'userId': { $in: ?0 }, 'displayName': { $regex: ?1, $options: 'i' } }")
  List<UserProfile> findFriendsForMention(
      List<String> friendIds, String keyword, Pageable pageable);
}
