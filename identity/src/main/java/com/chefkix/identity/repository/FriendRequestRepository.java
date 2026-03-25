package com.chefkix.identity.repository;

import com.chefkix.identity.entity.FriendRequest;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {
  Optional<FriendRequest> findBySenderIdAndReceiverId(String senderId, String receiverId);

  boolean existsBySenderIdAndReceiverId(String currentUserId, String targetUserId);

  void deleteBySenderIdAndReceiverId(String senderId, String receiverId);
}
