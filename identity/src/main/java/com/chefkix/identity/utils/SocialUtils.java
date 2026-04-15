package com.chefkix.identity.utils;

import com.chefkix.identity.entity.Friendship;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.enums.RelationshipStatus;
import com.chefkix.identity.repository.FriendRequestRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialUtils {
  final FriendRequestRepository friendRequestRepository;

  public RelationshipStatus determineRelationshipStatus(
      String currentUserId, UserProfile targetProfile) {
    String targetUserId = targetProfile.getUserId();

    // 1. Check if it's the current user themselves
    if (currentUserId.equals(targetUserId)) {
      return RelationshipStatus.SELF;
    }

    // 2. Check if already FRIENDS
    // (This check assumes you already have the UserProfile object of the CURRENT USER,
    // if not, you need to findOne profile of currentUserId first)

    // Approach 1: If you already stored List<Friendship> (as discussed)
    List<Friendship> friends =
        (targetProfile.getFriends() != null) ? targetProfile.getFriends() : Collections.emptyList();

    boolean isAlreadyFriends =
        friends.stream().anyMatch(f -> f.getFriendId().equals(currentUserId));
    if (isAlreadyFriends) {
      return RelationshipStatus.FRIENDS;
    }

    // 3. If not friends, check SENT REQUESTS (REQUEST_SENT)
    // (Current user sent to them)
    if (friendRequestRepository.existsBySenderIdAndReceiverId(currentUserId, targetUserId)) {
      return RelationshipStatus.REQUEST_SENT;
    }

    // 4. Otherwise, check RECEIVED REQUESTS (REQUEST_RECEIVED)
    // (They sent to current user)
    if (friendRequestRepository.existsBySenderIdAndReceiverId(targetUserId, currentUserId)) {
      return RelationshipStatus.REQUEST_RECEIVED;
    }

    // 5. If none of the above, they are STRANGERS
    return RelationshipStatus.NOT_FRIENDS;
  }
}
