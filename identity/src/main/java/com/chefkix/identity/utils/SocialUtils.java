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

    if (currentUserId.equals(targetUserId)) {
      return RelationshipStatus.SELF;
    }


    List<Friendship> friends =
        (targetProfile.getFriends() != null) ? targetProfile.getFriends() : Collections.emptyList();

    boolean isAlreadyFriends =
        friends.stream().anyMatch(f -> f.getFriendId().equals(currentUserId));
    if (isAlreadyFriends) {
      return RelationshipStatus.FRIENDS;
    }

    if (friendRequestRepository.existsBySenderIdAndReceiverId(currentUserId, targetUserId)) {
      return RelationshipStatus.REQUEST_SENT;
    }

    if (friendRequestRepository.existsBySenderIdAndReceiverId(targetUserId, currentUserId)) {
      return RelationshipStatus.REQUEST_RECEIVED;
    }

    return RelationshipStatus.NOT_FRIENDS;
  }
}
